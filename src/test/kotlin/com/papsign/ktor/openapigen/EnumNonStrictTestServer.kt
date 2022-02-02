package com.papsign.ktor.openapigen

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.exceptions.OpenAPIBadContentException
import com.papsign.ktor.openapigen.exceptions.OpenAPIRequiredFieldException
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

enum class NonStrictTestEnum {
    VALID,
    ALSO_VALID,
}

@Path("/")
data class NullableNonStrictEnumParams(@QueryParam("") val type: NonStrictTestEnum? = null)

@Path("/")
data class NonNullableNonStrictEnumParams(@QueryParam("") val type: NonStrictTestEnum)

class NonStrictEnumTestServer {

    companion object {
        // test server for nullable enums
        private fun Application.nullableEnum() {
            install(OpenAPIGen)
            install(StatusPages) {
                exception { call: ApplicationCall, cause: OpenAPIBadContentException ->
                    call.respond(HttpStatusCode.BadRequest, cause.localizedMessage)

                }
            }
            apiRouting {
                get<NullableNonStrictEnumParams, String> { params ->
                    if (params.type != null)
                        assertTrue { NonStrictTestEnum.values().contains(params.type) }
                    respond(params.type?.toString() ?: "null")
                }
            }
        }

        // test server for non-nullable enums
        private fun Application.nonNullableEnum() {
            install(OpenAPIGen)
            install(StatusPages) {
                exception { call: ApplicationCall, cause: OpenAPIRequiredFieldException ->
                    call.respond(HttpStatusCode.BadRequest, cause.localizedMessage)
                }
                exception { call: ApplicationCall, cause: OpenAPIBadContentException ->
                    call.respond(HttpStatusCode.BadRequest, cause.localizedMessage)
                }
            }
            apiRouting {
                get<NonNullableNonStrictEnumParams, String> { params ->
                    assertTrue { NonStrictTestEnum.values().contains(params.type) }
                    respond(params.type.toString())
                }
            }
        }
    }

    @Test
    fun `nullable enum could be omitted and it will be null`() {
        withTestApplication({ nullableEnum() }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("null", response.content)
            }
        }
    }

    @Test
    fun `nullable enum should be parsed correctly`() {
        withTestApplication({ nullableEnum() }) {
            handleRequest(HttpMethod.Get, "/?type=VALID").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("VALID", response.content)
            }
            handleRequest(HttpMethod.Get, "/?type=ALSO_VALID").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("ALSO_VALID", response.content)
            }
        }
    }

    @Test
    fun `nullable enum parsing should be case-sensitive and should return 200 with null result`() {
        withTestApplication({ nullableEnum() }) {
            handleRequest(HttpMethod.Get, "/?type=valid").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("null", response.content)
            }
            handleRequest(HttpMethod.Get, "/?type=also_valid").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("null", response.content)
            }
        }
    }

    @Test
    fun `nullable enum parsing should return 200 with null result on parse values outside of enum`() {
        withTestApplication({ nullableEnum() }) {
            handleRequest(HttpMethod.Get, "/?type=what").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("null", response.content)
            }
        }
    }

    @Test
    fun `non-nullable enum cannot be omitted`() {
        withTestApplication({ nonNullableEnum() }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertEquals("The field type is required", response.content)
            }
        }
    }

    @Test
    fun `non-nullable enum should be parsed correctly`() {
        withTestApplication({ nonNullableEnum() }) {
            handleRequest(HttpMethod.Get, "/?type=VALID").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("VALID", response.content)
            }
            handleRequest(HttpMethod.Get, "/?type=ALSO_VALID").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("ALSO_VALID", response.content)
            }
        }
    }

    @Test
    fun `non-nullable enum parsing should be case-sensitive and should throw on passing wrong case`() {
        withTestApplication({ nonNullableEnum() }) {
            handleRequest(HttpMethod.Get, "/?type=valid").apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertEquals("The field type is required", response.content)
            }
            handleRequest(HttpMethod.Get, "/?type=also_valid").apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertEquals("The field type is required", response.content)
            }
        }
    }

    @Test
    fun `non-nullable enum parsing should not parse values outside of enum`() {
        withTestApplication({ nonNullableEnum() }) {
            handleRequest(HttpMethod.Get, "/?type=what").apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertEquals("The field type is required", response.content)
            }
        }
    }
}
