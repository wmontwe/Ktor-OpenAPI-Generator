package com.papsign.ktor.openapigen.route

import io.ktor.server.routing.Route
import io.ktor.util.pipeline.ContextDsl

@ContextDsl
inline fun OpenAPIRoute<*>.exitAPI(crossinline fn: Route.() -> Unit) {
    ktorRoute.fn()
}
