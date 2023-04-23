package com.text2sql4j.api.resources

import com.text2sql4j.api.models.movies.MoviesResponse
import com.text2sql4j.api.models.movies.MovieWithActors
import com.text2sql4j.api.stores.MovieStore
import com.text2sql4j.api.verticles.TranslatorVerticle
import com.text2sql4j.translator.models.SqlTranslateInputs
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.await
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException

class MovieResource(private val eventBus: EventBus, private val movieStore: MovieStore) {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(MovieResource::class.java)
    }

    suspend fun getMovies(ctx: RoutingContext) {
        val query = ctx.queryParam("query").firstOrNull()
            ?: throw IllegalArgumentException("'query' query param is required")

        val limit = try {
            ctx.queryParam("limit").firstOrNull()?.toInt() ?: 100
        } catch (t: Throwable) {
            throw IllegalArgumentException("'limit' query param is invalid")
        }

        val offset = try {
            ctx.queryParam("offset").firstOrNull()?.toInt() ?: 0
        } catch (t: Throwable) {
            throw IllegalArgumentException("'offset' query param is invalid")
        }

        val translationInputs = SqlTranslateInputs(
            expectedPrefix = MovieStore.EXPECTED_GET_MOVIES_PREFIX,
            query = MovieStore.addSchemaToQuery(query),
            dbId = MovieStore.DB_ID,
            isIncremental = false
        )
        val generatedSql = try {
            eventBus.request<String>(
                TranslatorVerticle.SQL_TRANSLATE_ADDR,
                translationInputs,
                DeliveryOptions()
                    // This timeout needs to be slightly more than the translation timeout.
                    .setSendTimeout(180_000)
            )
                .await()
                .body()
        } catch (t: Throwable) {
            LOGGER.warn("Failed to generate SQL for query: '$query'", t)
            null
        }
        LOGGER.trace("Generated SQL: $generatedSql")

        var generatedSqlValid = generatedSql != null
        val moviesTotalCountPair = try {
            val movies = movieStore.getMovies(
                generatedSql = generatedSql,
                offset = offset,
                limit = limit
            )
            val count = movieStore.getMovieCount(generatedSql)
            movies to count
        } catch (t: Throwable) {
            LOGGER.warn("Generated SQL is invalid, using default query as fallback", t)
            generatedSqlValid = false
            // If the generated SQL causes a failure, then we retry with the default.
            val movies = movieStore.getMovies(
                generatedSql = null,
                offset = offset,
                limit = limit
            )
            val count = movieStore.getMovieCount(null)
            movies to count
        }
        val moviesWithActors = moviesTotalCountPair.first.map { movie ->
            val actors = movieStore.getActorsInMovie(movie.title)
            MovieWithActors(movie, actors)
        }

        val responseBody = MoviesResponse(
            generatedSql = generatedSql,
            moviesWithActors = moviesWithActors,
            generatedSqlValid = generatedSqlValid
        )

        ctx.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "application/json; charset=utf-8")
            .putHeader("X-Total-Count", moviesTotalCountPair.second.toString())
            .end(DatabindCodec.mapper().writeValueAsString(responseBody))
    }
}