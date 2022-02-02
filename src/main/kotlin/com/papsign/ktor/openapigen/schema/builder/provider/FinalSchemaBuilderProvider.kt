package com.papsign.ktor.openapigen.schema.builder.provider

import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.OpenAPIGenModuleExtension
import com.papsign.ktor.openapigen.classLogger
import com.papsign.ktor.openapigen.model.schema.SchemaModel
import com.papsign.ktor.openapigen.modules.ModuleProvider
import com.papsign.ktor.openapigen.modules.ofType
import com.papsign.ktor.openapigen.schema.builder.FinalSchemaBuilder
import com.papsign.ktor.openapigen.schema.builder.SchemaBuilder
import com.papsign.ktor.openapigen.schema.processor.SchemaProcessor
import com.papsign.ktor.openapigen.schema.processor.SchemaProcessorAnnotation
import java.util.Optional
import java.util.TreeMap
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.jvmErasure

object FinalSchemaBuilderProvider: FinalSchemaBuilderProviderModule, OpenAPIGenModuleExtension {

    private val log = classLogger()

    override fun provide(apiGen: OpenAPIGen, provider: ModuleProvider<*>): FinalSchemaBuilder {
        return Builder(
            provider.ofType<SchemaBuilderProviderModule>().flatMap { it.provide(apiGen, provider) }
        )
    }

    private fun SchemaProcessorAnnotation.getHandlerInstance(): SchemaProcessor<*> {
        return handler.objectInstance ?: error("${SchemaProcessorAnnotation::class.simpleName} handler must be an object")
    }

    private fun SchemaModel<*>.applyAnnotations(type: KType, annotations: List<Annotation>): SchemaModel<*> {
        return annotations.mapNotNull { annot ->
            annot.annotationClass
                .findAnnotation<SchemaProcessorAnnotation>()
                ?.getHandlerInstance()
                ?.let { Pair(it, annot) }
        }.fold(this) { model, (handler, annot) ->
            @Suppress("UNCHECKED_CAST")
            (handler as SchemaProcessor<Annotation>).process(model, type, annot)
        }
    }

    private class Builder(builders: List<SchemaBuilder>) : FinalSchemaBuilder {

        private val map = TreeMap<KType, SchemaBuilder>(
            Comparator { a, b ->
                when {
                    a.isSubtypeOf(b) -> -1
                    b.isSubtypeOf(a) -> 1
                    a == b -> 0
                    else -> 1
                }
            }
        ).apply {
            putAll(builders.groupBy { it.superType }.map { (key, value) ->
                val last = value.last()
                if (value.size > 1) log.warn("Two builder detected for type $key, selecting last: $last")
                Pair(key, last)
            })
        }

        override fun build(type: KType, annotations: List<Annotation>): SchemaModel<*> {
            type.let {
                when {
                    type.jvmErasure.isSubclassOf(Optional::class) -> type.arguments[0].type!!.withNullability(true)
                    else -> type
                }
            }.let { type ->
                return map.getOrPut(type) {
                    map.entries.firstOrNull { type.isSubtypeOf(it.key) }?.value
                        ?: error("Schema builder could not find declared builder for type $type, make sure it has a provider registered on the route")
                }.build(type, this) { it.applyAnnotations(type, type.jvmErasure.annotations).applyAnnotations(type, type.annotations).applyAnnotations(type, annotations) }
            }
        }
    }
}
