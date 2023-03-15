package com.text2sql4j.api.verticles

import io.vertx.core.http.HttpServer
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.FileSystemAccess
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.openapi.RouterBuilder
import io.vertx.kotlin.coroutines.await
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.util.concurrent.atomic.AtomicReference

class RESTVerticle : CoroutineVerticle() {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(RESTVerticle::class.java)
        private const val API_PATH = "api"
        private const val SWAGGER_UI_PATH = "swagger-ui"
    }

    private val httpServerRef: AtomicReference<HttpServer?> = AtomicReference()

    override suspend fun start() {
        val globalRouter: Router = Router.router(vertx)
        globalRouter.errorHandler(400, errorHandler())
        globalRouter.errorHandler(403, errorHandler())
        globalRouter.errorHandler(404, errorHandler())
        globalRouter.errorHandler(500, errorHandler())

        // Serves the yaml files in resources/api
        globalRouter.route("/$API_PATH/*")
            .handler(StaticHandler.create(FileSystemAccess.RELATIVE, "api"))
        // Serves the swagger ui in the resources/swagger-ui
        globalRouter.route("/$SWAGGER_UI_PATH/*")
            .handler(StaticHandler.create(FileSystemAccess.RELATIVE, "swagger-ui/4.18.1"))

        val routerBuilder = RouterBuilder.create(this.vertx, "api/openapi/movieApi.yaml")
            .onFailure {
                LOGGER.error("Failed to load OpenAPI Spec.", it)
            }
            .await()
        routerBuilder.operation("getMovies").handler { ctx ->
            LOGGER.info("asdfasdfasdf")
            ctx.response().end("{\"blah blah blah\": \"steeeve\"}")
        }
        globalRouter.route("/*").subRouter(routerBuilder.createRouter())

        val server = vertx.createHttpServer(HttpServerOptions().setPort(8081).setHost("localhost"))
            .requestHandler(globalRouter)
        httpServerRef.set(server)
        server.listen()
            .onSuccess { println("Server started on port: ${server.actualPort()}") }
            .await()
    }

    override suspend fun stop() {
        httpServerRef.get()
            ?.close()
            ?.await()
    }

    private fun errorHandler(): (RoutingContext) -> Unit {
        return { ctx ->
            val statusCode = ctx.statusCode()
            val error = ctx.failure()
            val message = error?.message ?: "unknown"
            if (error != null) {
                LOGGER.debug("Failed request", error)
            }
            if (statusCode == 403) {
                ctx.response()
                    .setStatusCode(403)
                    // TODO ErrorResponse
                    .end("Unauthorized")
            } else if (error == null) {
                ctx.response()
                    .setStatusCode(statusCode)
                    // TODO ErrorResponse
                    .end("unknown")
            } else {
                when (error) {
                    is IllegalArgumentException -> ctx.response()
                        .setStatusCode(400)
                        // TODO ErrorResponse
                        .end("Invalid Request")
                    else -> ctx.response()
                        .setStatusCode(statusCode)
                        // TODO ErrorResponse
                        .end("Internal Server Error")
                }
            }
        }
    }
}