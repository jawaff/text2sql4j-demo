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
        const val DB_ID = "movies_db"

        private const val MOVIE_TABLE = "movies"
        private val MOVIE_COLUMNS = listOf(
            "movie_title", "year_released", "director_name", "producer_name", "studio_name", "color_process", "genre"
        )
        private val INSERT_MOVIE = "INSERT INTO $MOVIE_TABLE " +
                "(${MOVIE_COLUMNS.joinToString(",")}) " +
                "VALUES (${(1..MOVIE_COLUMNS.size).joinToString(",") { i -> "$$i"}}) " +
                "ON CONFLICT ON CONSTRAINT movies_primary_key DO NOTHING"
        private const val DELETE_MOVIES = "DELETE FROM $MOVIE_TABLE"
        val EXPECTED_GET_MOVIES_PREFIX = "select ${MOVIE_COLUMNS.joinToString(", ") { "t1.$it" }} " +
                "from $MOVIE_TABLE as t1"
        private val DEFAULT_GET_MOVIES = EXPECTED_GET_MOVIES_PREFIX

        private const val ACTOR_TABLE = "actors"
        private val ACTOR_COLUMNS = listOf(
            "actor_name", "start_year_of_work", "end_year_of_work", "gender", "year_of_birth"
        )
        private val INSERT_ACTOR = "INSERT INTO $ACTOR_TABLE " +
                "(${ACTOR_COLUMNS.joinToString(",")}) " +
                "VALUES (${(1..ACTOR_COLUMNS.size).joinToString(",") { i -> "$$i"}}) " +
                "ON CONFLICT ON CONSTRAINT actors_primary_key DO NOTHING"
        private const val DELETE_ACTORS = "DELETE FROM $ACTOR_TABLE"

        private const val CAST_TABLE = "movie_casts"
        private val CAST_COLUMNS = listOf(
            "movie_title", "actor_name", "role"
        )
        private val INSERT_CAST = "INSERT INTO $CAST_TABLE " +
                "(${CAST_COLUMNS.joinToString(",")}) " +
                "SELECT C.* " +
                "FROM (VALUES (${(1..CAST_COLUMNS.size).joinToString(",") { i -> "$$i"}})) C " +
                "(${CAST_COLUMNS.joinToString(",")}) " +
                "WHERE EXISTS(SELECT 1 FROM $MOVIE_TABLE AS M WHERE M.movie_title = C.movie_title) " +
                "AND EXISTS(SELECT 1 FROM $ACTOR_TABLE AS A WHERE A.actor_name = C.actor_name) " +
                "ON CONFLICT ON CONSTRAINT movie_casts_primary_key DO NOTHING"
        private const val DELETE_CASTS = "DELETE FROM $CAST_TABLE"

        private val GET_ACTORS_IN_MOVIE = "SELECT ${ACTOR_COLUMNS.joinToString(",") { "A.$it" }} " +
                "FROM $ACTOR_TABLE A " +
                "INNER JOIN $CAST_TABLE C ON C.actor_name = A.actor_name " +
                "WHERE C.movie_title = $1 " +
                "ORDER BY A.actor_name ASC"

        private const val OUTER_GET_MOVIES_COUNT = "SELECT COUNT(*) FROM (%s) AS sub"
        private const val OUTER_GET_MOVIES = "SELECT * FROM (%s) AS sub " +
                "ORDER BY sub.year_released DESC LIMIT %s OFFSET %s"

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

    private fun String.removeAfter(delimiter: String, includeDelimiter: Boolean = true): String {
        val index = if (!includeDelimiter) {
            indexOf(delimiter) + delimiter.length
        } else {
            indexOf(delimiter)
        }
        return if (index == -1) this else replaceRange(index, length, "")
    }

    private fun normalizeGeneratedSql(generatedSql: String?): String {
        // The SQL parser in the translator doesn't support the "distinct on" clause, so we inject it after the fact
        // to prevent duplicate movies being returned due to joins.
        return generatedSql?.replace("select", "select distinct on (t1.movie_title)")
            // Removing order by and group by clauses to simplify edge cases.
            ?.removeAfter("order by")
            ?.removeAfter("group by")
            ?: DEFAULT_GET_MOVIES
    }

    private fun toMovie(row: Row): Movie {
        return Movie(
            title = row.getString("movie_title"),
            yearReleased = row.getInteger("year_released"),
            director = row.getString("director_name"),
            producer = row.getString("producer_name"),
            studio = row.getString("studio_name"),
            colorProcess = ColorProcess.valueOf(row.getString("color_process")),
            genre = Genre.valueOf(row.getString("genre"))
        )
    }

    suspend fun getMovieCount(generatedSql: String?): Int {
        val baseSql = normalizeGeneratedSql(generatedSql)
        val finalSql = OUTER_GET_MOVIES_COUNT.format(baseSql)

        return pgPool.withConnection { connection ->
            val promise = Promise.promise<Int>()
            connection.preparedQuery(finalSql)
                .execute()
                .onComplete { result ->
                    try {
                        if (result.succeeded()) {
                            promise.complete(result.result().first().getInteger(0))
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

    suspend fun getMovies(generatedSql: String?, offset: Int, limit: Int): List<Movie> {
        val baseSql = normalizeGeneratedSql(generatedSql)
        // Due to the different distinct and order by clauses, the generated sql needs to go into a subquery
        val finalSql = OUTER_GET_MOVIES.format(baseSql, limit, offset)

        return pgPool.withConnection { connection ->
            val promise = Promise.promise<List<Movie>>()
            connection.preparedQuery(finalSql)
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
            name = row.getString("actor_name"),
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