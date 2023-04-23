package com.text2sql4j.api.verticles

import com.text2sql4j.api.models.ErrorResponse
import com.text2sql4j.api.resources.MovieResource
import com.text2sql4j.api.stores.MovieDatasetLoader
import com.text2sql4j.api.stores.MovieStore
import io.vertx.core.http.HttpServer
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.FileSystemAccess
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.openapi.Operation
import io.vertx.ext.web.openapi.RouterBuilder
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.pgclient.PgPool
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.util.concurrent.atomic.AtomicReference

class RESTVerticle(
    private val pgPool: PgPool,
    private val doInsertDataset: Boolean
) : CoroutineVerticle() {
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

        val movieStore = MovieStore(pgPool)
        val movieResource = MovieResource(vertx.eventBus(), movieStore)

        if (doInsertDataset) {
            LOGGER.info("Checking database for movies")

            // We check to see if our database has movies in it before tests run.
            // We need to load our dataset into the database if it hasn't already been done.
            val moviesExist = movieStore.getMovies(null, 0, 1)
                .isNotEmpty()

            if (moviesExist) {
                LOGGER.info("Database already has movies")
            } else {
                LOGGER.info("Preparing Movie Dataset")
                MovieDatasetLoader.loadDataset(movieStore)
                LOGGER.info("Done Preparing Movie Dataset")
            }
        }

        routerBuilder.operation("getMovies").coroutineHandler(movieResource::getMovies)
        globalRouter.route("/*").subRouter(routerBuilder.createRouter())

        val server = vertx.createHttpServer(HttpServerOptions().setPort(8081).setHost("localhost"))
            .requestHandler(globalRouter)
        httpServerRef.set(server)
        server.listen()
            .onSuccess {
                println("Server started on port: ${server.actualPort()}")
                println("Access Swagger UI at: http://localhost:${server.actualPort()}/swagger-ui")
            }
            .await()
    }

    override suspend fun stop() {
        httpServerRef.get()
            ?.close()
            ?.await()
    }

    private fun Operation.coroutineHandler(fn: suspend (RoutingContext) -> Unit) {
        handler { ctx ->
            launch(ctx.vertx().dispatcher()) {
                try {
                    fn(ctx)
                } catch (t: Throwable) {
                    ctx.fail(t)
                }
            }
        }
    }

    private fun errorHandler(): (RoutingContext) -> Unit {
        return { ctx ->
            val statusCode = ctx.statusCode()
            val error = ctx.failure()
            val message = error?.message ?: "unknown"
            if (error != null) {
                LOGGER.debug("Failed request", error)
            }
            if (error == null) {
                ctx.response()
                    .setStatusCode(statusCode)
                    .end(DatabindCodec.mapper().writeValueAsString(ErrorResponse("unknown", "unknown")))
            } else {
                when (error) {
                    is IllegalArgumentException -> ctx.response()
                        .setStatusCode(400)
                        .end(DatabindCodec.mapper().writeValueAsString(ErrorResponse("Invalid Request", message)))
                    else -> ctx.response()
                        .setStatusCode(statusCode)
                        .end(DatabindCodec.mapper().writeValueAsString(ErrorResponse("Server Error", message)))
                }
            }
        }
    }
}