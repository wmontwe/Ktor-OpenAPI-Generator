package com.papsign.ktor.openapigen.content.type.multipart

import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.OpenAPIGenModuleExtension
import com.papsign.ktor.openapigen.annotations.mapping.openAPIName
import com.papsign.ktor.openapigen.content.type.BodyParser
import com.papsign.ktor.openapigen.content.type.ContentTypeProvider
import com.papsign.ktor.openapigen.exceptions.assertContent
import com.papsign.ktor.openapigen.getKType
import com.papsign.ktor.openapigen.model.operation.MediaTypeEncodingModel
import com.papsign.ktor.openapigen.model.operation.MediaTypeModel
import com.papsign.ktor.openapigen.model.schema.SchemaModel
import com.papsign.ktor.openapigen.modules.ModuleProvider
import com.papsign.ktor.openapigen.modules.ofType
import com.papsign.ktor.openapigen.schema.builder.provider.FinalSchemaBuilderProviderModule
import com.papsign.ktor.openapigen.unitKType
import io.ktor.http.ContentType
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart
import io.ktor.util.asStream
import io.ktor.util.pipeline.PipelineContext
import java.io.InputStream
import java.time.Instant
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.jvmErasure

object MultipartFormDataContentProvider : BodyParser, OpenAPIGenModuleExtension {

    override fun <T : Any> getParseableContentTypes(type: KType): List<ContentType> {
        return listOf(ContentType.MultiPart.FormData)
    }

    data class MultipartCVT<T>(val default: T?, val type: KType, val clazz: KClass<*>, val serializer: (T) -> String, val parser: (String) -> T)

    inline fun <reified T> cvt(noinline serializer: (T) -> String, noinline parser: (String) -> T, default: T? = null) = MultipartCVT(default,
        getKType<T>(), T::class, serializer, parser)

    private val streamTypes = setOf(
        getKType<InputStream>(),
        getKType<ContentInputStream>(),
        getKType<NamedFileInputStream>(),
        getKType<InputStream?>(),
        getKType<ContentInputStream?>(),
        getKType<NamedFileInputStream?>()
    )

    private val conversions = setOf(
            cvt({ it }, { it }, ""),
            cvt(Int::toString, String::toInt, 0),
            cvt(Long::toString, String::toLong, 0),
            cvt(Float::toString, String::toFloat, .0f),
            cvt(Double::toString, String::toDouble, .0),
            cvt(Instant::toString, Instant::parse),
            cvt(Boolean::toString, String::toBoolean, false)
    )

    private val conversionsByType = conversions.associateBy { it.type.withNullability(false) } + conversions.associateBy { it.type.withNullability(true) }

    private val nonNullTypes = streamTypes + conversionsByType.keys

    private val allowedTypes = nonNullTypes

    private val typeContentTypes = HashMap<KType, Map<String, MediaTypeEncodingModel>>()


    override suspend fun <T : Any> parseBody(type: KType, request: PipelineContext<Unit, ApplicationCall>): T {
        val objectMap = HashMap<String, Any>()
        request.context.receiveMultipart().forEachPart {
            val name = it.name
            if (name != null) {
                when (it) {
                    is PartData.FormItem -> {
                        objectMap[name] = it.value
                    }
                    is PartData.FileItem -> {
                        objectMap[name] = NamedFileInputStream(it.originalFileName, it.contentType, it.streamProvider())
                    }
                    is PartData.BinaryItem -> {
                        objectMap[name] = ContentInputStream(it.contentType, it.provider().asStream())
                    }
                }
            }
        }
        @Suppress("UNCHECKED_CAST")
        val ctor = (type.classifier as KClass<T>).primaryConstructor!!
        return ctor.callBy(ctor.parameters.associateWith {
            val raw = objectMap[it.openAPIName]
            if ((raw == null || (raw !is InputStream && streamTypes.contains(it.type))) && it.type.isMarkedNullable) {
                null
            } else {
                if (raw is InputStream) {
                    raw
                } else {
                    val cvt = conversionsByType[it.type] ?: error("Unhandled Type ${it.type}")
                    when (raw) {
                        null -> {
                            cvt.default ?: error("No provided value for field ${it.openAPIName}")
                        }
                        is String -> {
                            cvt.parser(raw)
                        }
                        else -> error("Unhandled Type ${it.type}")
                    }
                }
            }
        })
    }


    override fun <T> getMediaType(type: KType, apiGen: OpenAPIGen, provider: ModuleProvider<*>, example: T?, usage: ContentTypeProvider.Usage): Map<ContentType, MediaTypeModel<T>>? {
        if (type == unitKType) return null
        val formContentType = type.jvmErasure.findAnnotation<FormDataRequest>()?.type?.contentType ?: return null
        val ctor = type.jvmErasure.primaryConstructor
        when (usage) {
            ContentTypeProvider.Usage.PARSE -> {
                assertContent(ctor != null) {
                    "${this::class.simpleName} requires a primary constructor"
                }
                assertContent(allowedTypes.containsAll(ctor!!.parameters.map { it.type.withNullability(false) })) {
                    "${this::class.simpleName} all constructor parameters must be of types: $allowedTypes"
                }
            }
            ContentTypeProvider.Usage.SERIALIZE -> {
                assertContent(allowedTypes.containsAll(ctor!!.parameters.map { it.type.withNullability(false) })) {
                    "${this::class.simpleName} only supports DTOs containing following types: ${allowedTypes.joinToString()}"
                }
            }
        }

        val contentTypes = synchronized(typeContentTypes) {
            typeContentTypes.getOrPut(type) {
                type.jvmErasure.memberProperties
                        .associateBy { it.name }
                        .mapValues { it.value.findAnnotation<PartEncoding>() }
                        .filterValues { it != null }
                        .mapValues { MediaTypeEncodingModel(it.value!!.contentType) }
            }.toMap()
        }
        val schemaBuilder = provider.ofType<FinalSchemaBuilderProviderModule>().last().provide(apiGen, provider)
        @Suppress("UNCHECKED_CAST")
        return mapOf(formContentType to MediaTypeModel(schemaBuilder.build(type) as SchemaModel<T>, example, null, contentTypes))
    }
}
