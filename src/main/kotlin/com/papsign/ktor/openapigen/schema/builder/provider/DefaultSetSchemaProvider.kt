package com.papsign.ktor.openapigen.schema.builder.provider

import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.OpenAPIGenModuleExtension
import com.papsign.ktor.openapigen.getKType
import com.papsign.ktor.openapigen.model.schema.SchemaModel
import com.papsign.ktor.openapigen.modules.DefaultOpenAPIModule
import com.papsign.ktor.openapigen.modules.ModuleProvider
import com.papsign.ktor.openapigen.schema.builder.FinalSchemaBuilder
import com.papsign.ktor.openapigen.schema.builder.SchemaBuilder
import kotlin.reflect.KType

object DefaultSetSchemaProvider: SchemaBuilderProviderModule, OpenAPIGenModuleExtension, DefaultOpenAPIModule {

    private val builders = listOf(
        Builder(getKType<Set<*>?>()) { type: KType ->
            type.arguments[0].type ?: error("bad type $type: star projected types are not supported")
        }
    )

    override fun provide(apiGen: OpenAPIGen, provider: ModuleProvider<*>): List<SchemaBuilder> {
        return builders
    }

    private data class Builder(override val superType: KType, private val getter: (KType) -> KType) :
        SchemaBuilder {
        override fun build(type: KType, builder: FinalSchemaBuilder, finalize: (SchemaModel<*>)->SchemaModel<*>): SchemaModel<*> {
            checkType(type)
            return finalize(SchemaModel.SchemaModelArr<Any?>(builder.build(getter(type)), type.isMarkedNullable, uniqueItems = true))
        }
    }
}

