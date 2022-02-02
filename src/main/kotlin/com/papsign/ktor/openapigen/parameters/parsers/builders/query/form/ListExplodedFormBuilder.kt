package com.papsign.ktor.openapigen.parameters.parsers.builders.query.form

import com.papsign.ktor.openapigen.parameters.parsers.builders.BuilderSelector
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

class ListExplodedFormBuilder(type: KType) : CollectionExplodedFormBuilder(type) {

    override fun transform(lst: List<Any?>): Any? {
        return lst
    }


    companion object : BuilderSelector<ListExplodedFormBuilder> {

        override fun canHandle(type: KType, explode: Boolean): Boolean {
            return type.jvmErasure.isSubclassOf(List::class) && explode
        }

        override fun create(type: KType, explode: Boolean): ListExplodedFormBuilder {
            return ListExplodedFormBuilder(type)
        }
    }
}
