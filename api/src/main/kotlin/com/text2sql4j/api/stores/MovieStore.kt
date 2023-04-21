package com.text2sql4j.api.stores

import com.text2sql4j.api.models.movies.Actor
import com.text2sql4j.api.models.movies.Movie
import com.text2sql4j.api.models.movies.Cast
import com.text2sql4j.api.models.movies.ColorProcess
import com.text2sql4j.api.models.movies.Genre
import io.vertx.core.Promise
import io.vertx.kotlin.coroutines.await
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.Tuple
import java.lang.IllegalStateException

class MovieStore(private val pgPool: PgPool) {
    companion object {
        const val DB_ID = "movies"

        private const val MOVIE_TABLE = "movies"
        private val MOVIE_COLUMNS = listOf(
            "title", "year_released", "director_name", "producer_name", "studio_name", "color_process", "genre"
        )
        private val INSERT_MOVIE = "INSERT INTO $MOVIE_TABLE " +
            "(${MOVIE_COLUMNS.joinToString(",")}) " +
            "VALUES (${(1..MOVIE_COLUMNS.size).joinToString(",") { i -> "$$i"}})"
        private const val DELETE_MOVIES = "DELETE FROM $MOVIE_TABLE"
        val EXPECTED_GET_MOVIES_PREFIX = "select ${MOVIE_COLUMNS.joinToString(",") { "t1.$it" }} " +
            "from $MOVIE_TABLE as t1"
        private val DEFAULT_GET_MOVIES = EXPECTED_GET_MOVIES_PREFIX

        private const val CAST_TABLE = "movie_casts"
        private val CAST_COLUMNS = listOf(
            "movie_title", "actor_name", "role"
        )
        private val INSERT_CAST = "INSERT INTO $CAST_TABLE " +
            "(${CAST_COLUMNS.joinToString(",")}) " +
            "VALUES (${(1..CAST_COLUMNS.size).joinToString(",") { i -> "$$i"}})"
        private const val DELETE_CASTS = "DELETE FROM $CAST_TABLE"

        private const val ACTOR_TABLE = "actors"
        private val ACTOR_COLUMNS = listOf(
            "name", "start_year_of_work", "end_year_of_work", "gender", "year_of_birth"
        )
        private val INSERT_ACTOR = "INSERT INTO $ACTOR_TABLE " +
            "(${ACTOR_COLUMNS.joinToString(",")}) " +
            "VALUES (${(1..ACTOR_COLUMNS.size).joinToString(",") { i -> "$$i"}})"
        private const val DELETE_ACTORS = "DELETE FROM $ACTOR_TABLE"
        private val GET_ACTORS_IN_MOVIE = "SELECT ${ACTOR_COLUMNS.joinToString(",") { "A.$it" }} " +
            "FROM $ACTOR_TABLE A " +
            "INNER JOIN $CAST_TABLE C ON C.actor_name = A.name " +
            "WHERE C.movie_title = $1 " +
            "ORDER BY A.name ASC"

        private const val DEFAULT_MOVIE_ORDER_BY = " ORDER BY t1.year_released DESC"
        private const val LIMIT_OFFSET_CLAUSES = " LIMIT %s OFFSET %s"

        fun addSchemaToQuery(query: String): String {
            val tableSchemas = listOf(
                MOVIE_TABLE.plus(" : ")
                    .plus(MOVIE_COLUMNS.joinToString(", ")),
                ACTOR_TABLE.plus(" : ")
                    .plus(ACTOR_COLUMNS.joinToString(", ")),
                CAST_TABLE.plus(" : ")
                    .plus(CAST_COLUMNS.joinToString(", "))
            )
            return "$query | $DB_ID | ".plus(tableSchemas.joinToString(" | "))
        }
    }

    suspend fun insertActors(actors: List<Actor>) {
        pgPool.withConnection { connection ->
            val promise = Promise.promise<Unit>()
            connection.preparedQuery(INSERT_ACTOR)
                .executeBatch(
                    actors.map { actor ->
                        Tuple.of(
                            actor.name,
                            actor.startYearOfWork,
                            actor.endYearOfWork,
                            actor.gender,
                            actor.yearOfBirth
                        )
                    }
                )
                .onComplete { result ->
                    if (result.succeeded()) {
                        promise.complete(null)
                    } else {
                        promise.fail(IllegalStateException("Failed to insert actors", result.cause()))
                    }
                }
            promise.future().onComplete { connection.close() }
        }
            .await()
    }

    suspend fun insertMovies(movies: List<Movie>) {
        pgPool.withConnection { connection ->
            val promise = Promise.promise<Unit>()
            connection.preparedQuery(INSERT_MOVIE)
                .executeBatch(
                    movies.map { movie ->
                        Tuple.of(
                            movie.title,
                            movie.yearReleased,
                            movie.director,
                            movie.producer,
                            movie.studio,
                            movie.colorProcess.name,
                            movie.genre.name
                        )
                    })
                .onComplete { result ->
                    if (result.succeeded()) {
                        promise.complete(null)
                    } else {
                        promise.fail(IllegalStateException("Failed to insert movies", result.cause()))
                    }
                }
            promise.future().onComplete { connection.close() }
        }
            .await()
    }

    suspend fun insertCasts(casts: List<Cast>) {
        pgPool.withConnection { connection ->
            val promise = Promise.promise<Unit>()
            connection.preparedQuery(INSERT_CAST)
                .executeBatch(
                    casts.map { cast ->
                        Tuple.of(cast.movieTitle, cast.actorName, cast.role.name)
                    }
                )
                .onComplete { result ->
                    if (result.succeeded()) {
                        promise.complete(null)
                    } else {
                        promise.fail(IllegalStateException("Failed to insert casts", result.cause()))
                    }
                }
            promise.future().onComplete { connection.close() }
        }
            .await()
    }

    private fun toMovie(row: Row): Movie {
        return Movie(
            title = row.getString("title"),
            yearReleased = row.getInteger("year_released"),
            director = row.getString("director_name"),
            producer = row.getString("producer_name"),
            studio = row.getString("studio_name"),
            colorProcess = ColorProcess.valueOf(row.getString("color_process")),
            genre = Genre.valueOf(row.getString("genre"))
        )
    }

    suspend fun getMovies(generatedSql: String?, offset: Int, limit: Int): List<Movie> {
        val baseSql = generatedSql ?: DEFAULT_GET_MOVIES
        val updatedSql = if (baseSql.lowercase().contains("order by")) {
            baseSql
        } else {
            baseSql.plus(DEFAULT_MOVIE_ORDER_BY)
        }
            .plus(LIMIT_OFFSET_CLAUSES.format(limit, offset))

        return pgPool.withConnection { connection ->
            val promise = Promise.promise<List<Movie>>()
            connection.preparedQuery(updatedSql)
                .execute()
                .onComplete { result ->
                    try {
                        if (result.succeeded()) {
                            promise.complete(result.result().map(::toMovie))
                        } else {
                            throw IllegalStateException("Failed to get movies", result.cause())
                        }
                    } catch (t: Throwable) {
                        promise.fail(t)
                    }
                }
            promise.future().onComplete { connection.close() }
        }
            .await()
    }

    private fun toActor(row: Row): Actor {
        return Actor(
            name = row.getString("name"),
            startYearOfWork = row.getInteger("start_year_of_work"),
            endYearOfWork = row.getInteger("end_year_of_work"),
            gender = row.getString("gender"),
            yearOfBirth = row.getInteger("year_of_birth")
        )
    }

    suspend fun getActorsInMovie(movieTitle: String): List<Actor> {
        return pgPool.withConnection { connection ->
            val promise = Promise.promise<List<Actor>>()
            connection.preparedQuery(GET_ACTORS_IN_MOVIE)
                .execute(Tuple.of(movieTitle))
                .onComplete { result ->
                    try {
                        if (result.succeeded()) {
                            promise.complete(result.result().map(::toActor))
                        } else {
                            throw IllegalStateException("Failed to get actors", result.cause())
                        }
                    } catch (t: Throwable) {
                        promise.fail(t)
                    }
                }
            promise.future().onComplete { connection.close() }
        }
            .await()
    }

    suspend fun deleteMovies() {
        pgPool.withConnection { connection ->
            val promise = Promise.promise<Unit>()
            connection.preparedQuery(DELETE_MOVIES)
                .execute()
                .onComplete { result ->
                    if (result.succeeded()) {
                        promise.complete(null)
                    } else {
                        promise.fail(IllegalStateException("Failed to delete movies", result.cause()))
                    }
                }
            promise.future().onComplete { connection.close() }
        }
            .await()
    }


    suspend fun deleteActors() {
        pgPool.withConnection { connection ->
            val promise = Promise.promise<Unit>()
            connection.preparedQuery(DELETE_ACTORS)
                .execute()
                .onComplete { result ->
                    if (result.succeeded()) {
                        promise.complete(null)
                    } else {
                        promise.fail(IllegalStateException("Failed to delete actors", result.cause()))
                    }
                }
            promise.future().onComplete { connection.close() }
        }
            .await()
    }


    suspend fun deleteCasts() {
        pgPool.withConnection { connection ->
            val promise = Promise.promise<Unit>()
            connection.preparedQuery(DELETE_CASTS)
                .execute()
                .onComplete { result ->
                    if (result.succeeded()) {
                        promise.complete(null)
                    } else {
                        promise.fail(IllegalStateException("Failed to delete casts", result.cause()))
                    }
                }
            promise.future().onComplete { connection.close() }
        }
            .await()
    }
}