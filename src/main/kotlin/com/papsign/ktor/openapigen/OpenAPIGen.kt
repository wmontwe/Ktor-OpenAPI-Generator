package com.papsign.ktor.openapigen

import com.papsign.ktor.openapigen.model.base.OpenAPIModel
import com.papsign.ktor.openapigen.model.info.ContactModel
import com.papsign.ktor.openapigen.model.info.ExternalDocumentationModel
import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.model.server.ServerModel
import com.papsign.ktor.openapigen.modules.CachingModuleProvider
import com.papsign.ktor.openapigen.modules.OpenAPIModule
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.Plugin
import io.ktor.server.application.call
import io.ktor.server.request.path
import io.ktor.util.AttributeKey
import org.reflections.Reflections
import kotlin.reflect.full.starProjectedType

class OpenAPIGen(
        config: Configuration,
        @Deprecated("Will be replaced with less dangerous alternative when the use case has been fleshed out.") val pipeline: ApplicationCallPipeline
) {
    private val log = classLogger()

    val api = config.api

    private val tags = HashMap<String, APITag>()

    val globalModuleProvider = CachingModuleProvider()

    init {
        (config.scanPackagesForModules + javaClass.`package`.name).forEach {
            val reflections = Reflections(it)
            log.debug("Registering modules in package $it")
            val objects = reflections.getSubTypesOf(OpenAPIGenExtension::class.java).mapNotNull { it.kotlin.objectInstance }
            objects.forEach {
                log.trace("Registering global module: ${it::class.simpleName}")
                it.onInit(this)
            }
        }
        config.removeModules.forEach(globalModuleProvider::unRegisterModule)
        config.addModules.forEach { globalModuleProvider.registerModule(it, it::class.starProjectedType) }
    }

    class Configuration(val api: OpenAPIModel) {
        inline fun info(crossinline configure: InfoModel.() -> Unit) {
            api.info = InfoModel().apply(configure)
        }

        inline fun InfoModel.contact(crossinline configure: ContactModel.() -> Unit) {
            contact = ContactModel().apply(configure)
        }

        inline fun server(url: String, crossinline configure: ServerModel.() -> Unit = {}) {
            api.servers.add(ServerModel(url).apply(configure))
        }

        inline fun externalDocs(url: String, crossinline configure: ExternalDocumentationModel.() -> Unit = {}) {
            api.externalDocs = ExternalDocumentationModel(url).apply(configure)
        }

        var swaggerUiPath = "swagger-ui"
        var serveSwaggerUi = true
        var swaggerUiVersion = "3.25.0"

        var scanPackagesForModules: Array<String> = arrayOf()

        var addModules = mutableListOf<OpenAPIModule>()
        var removeModules = mutableListOf<OpenAPIModule>()

        fun addModules(vararg modules: OpenAPIModule) {
            addModules.addAll(modules)
        }

        fun addModules(modules: Iterable<OpenAPIModule>) {
            addModules.addAll(modules)
        }

        fun removeModules(vararg modules: OpenAPIModule) {
            removeModules.addAll(modules)
        }

        fun removeModules(modules: Iterable<OpenAPIModule>) {
            removeModules.addAll(modules)
        }

        fun replaceModule(delete: OpenAPIModule, add: OpenAPIModule) {
            addModules.add(add)
            removeModules.add(delete)
        }
    }


    fun getOrRegisterTag(tag: APITag): String {
        val other = tags.getOrPut(tag.name) {
            api.tags.add(tag.toTag())
            tag
        }
        if (other != tag) error("TagModule named ${tag.name} is already defined")
        return tag.name
    }

    companion object Feature : Plugin<ApplicationCallPipeline, Configuration, OpenAPIGen> {

        override val key = AttributeKey<OpenAPIGen>("OpenAPI Generator")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): OpenAPIGen {
            val api = OpenAPIModel()
            val cfg = Configuration(api).apply(configure)
            if (cfg.serveSwaggerUi) {
                val ui = SwaggerUi(cfg.swaggerUiPath, cfg.swaggerUiVersion)
                pipeline.intercept(ApplicationCallPipeline.Call) {
                    val cmp = "/${cfg.swaggerUiPath.trim('/')}/"
                    if (call.request.path().startsWith(cmp))
                        ui.serve(call.request.path().removePrefix(cmp), call)
                }
            }
            return OpenAPIGen(cfg, pipeline)
        }
    }
}
