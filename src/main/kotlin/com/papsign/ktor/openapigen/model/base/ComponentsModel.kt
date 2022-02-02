package com.papsign.ktor.openapigen.model.base

import com.papsign.ktor.openapigen.model.DataModel
import com.papsign.ktor.openapigen.model.info.ExampleModel
import com.papsign.ktor.openapigen.model.operation.HeaderModel
import com.papsign.ktor.openapigen.model.operation.ParameterModel
import com.papsign.ktor.openapigen.model.operation.RequestBodyModel
import com.papsign.ktor.openapigen.model.operation.StatusResponseModel
import com.papsign.ktor.openapigen.model.schema.SchemaModel
import com.papsign.ktor.openapigen.model.security.SecuritySchemeModel

data class ComponentsModel(
    var schemas: MutableMap<String, SchemaModel<*>> = sortedMapOf(),
    var responses: MutableMap<String, StatusResponseModel> = sortedMapOf(),
    var parameters: MutableMap<String, ParameterModel<*>> = sortedMapOf(),
    var examples: MutableMap<String, ExampleModel<*>> = sortedMapOf(),
    var requestBodies: MutableMap<String, RequestBodyModel> = sortedMapOf(),
    var headers: MutableMap<String, HeaderModel<*>> = sortedMapOf(),
    var securitySchemes: MutableMap<String, SecuritySchemeModel<*>> = sortedMapOf()
    //links
    //callbacks
): DataModel
