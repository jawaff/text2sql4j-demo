package com.text2sql4j.api.resources

import com.text2sql4j.api.TestSession
import com.text2sql4j.api.models.movies.*
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.RequestOptions
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.random.Random
import com.fasterxml.jackson.module.kotlin.readValue
import com.text2sql4j.api.models.ErrorResponse
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import java.net.URLEncoder

class MovieResourceTest {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(MovieResourceTest::class.java)

    }
    @AfterEach
    @BeforeEach
    fun clearDatabase() {
        TestSession.clearDatabase()
    }

    @Test
    fun getMovies_success() {
        LOGGER.info("test getMovies_success")
        runBlocking {
            val expectedMovies = (0 until 5).map { i ->
                Movie(
                    title = UUID.randomUUID().toString(),
                    yearReleased = 1950+i,
                    director = UUID.randomUUID().toString(),
                    producer = UUID.randomUUID().toString(),
                    studio = UUID.randomUUID().toString(),
                    colorProcess = ColorProcess.values().random(),
                    genre = Genre.values().random(),
                )
            }
            TestSession.movieStore.insertMovies(expectedMovies)

            val expectedActorsForMovies: List<List<Actor>> = expectedMovies.indices.map { i ->
                val actors = (0 until 5).map { j ->
                    Actor(
                        name = UUID.randomUUID().toString(),
                        startYearOfWork = Random.nextInt(),
                        endYearOfWork = Random.nextInt(),
                        gender = if (i % 2 == 0) "female" else "male",
                        yearOfBirth = Random.nextInt()
                    )
                }
                TestSession.movieStore.insertActors(actors)

                val movie = expectedMovies[i]
                val casts = actors.map { actor ->
                    Cast(
                        movieTitle = movie.title,
                        actorName = actor.name,
                        role = ActorRole.values().random()
                    )
                }
                TestSession.movieStore.insertCasts(casts)
                actors
            }

            val expectedMovie = expectedMovies[0]
            val expectedActorsForMovie = expectedActorsForMovies[0]

            val query = URLEncoder.encode("Movies released in ${expectedMovie.yearReleased}", StandardCharsets.UTF_8.toString())
            val options = RequestOptions()
                .setMethod(HttpMethod.GET)
                .setAbsoluteURI("http://localhost:8081/movies?query=$query")
            val request = TestSession.vertx.createHttpClient()
                .request(options)
                .await()
            request.end().await()
            val response = request.response().await()
            val statusCode = response.statusCode()
            if (statusCode == 200) {
                val body = response.body().await().toString(StandardCharsets.UTF_8)
                val totalCount = response.getHeader("X-Total-Count").toInt()
                LOGGER.debug("Response Body: $body")
                LOGGER.debug("Total Count: $totalCount")
                val results: MoviesResponse = DatabindCodec.mapper().readValue(body)

                Assertions.assertThat(results.generatedSql).isNotNull()
                Assertions.assertThat(results.generatedSqlValid).isTrue
                Assertions.assertThat(results.moviesWithActors)
                    .hasSize(1)
                    .usingRecursiveComparison()
                    .ignoringCollectionOrder()
                    .isEqualTo(
                        listOf(MovieWithActors(movie = expectedMovie, actors = expectedActorsForMovie))
                    )

                Assertions.assertThat(totalCount).isEqualTo(1)
            } else {
                val error: ErrorResponse = DatabindCodec.mapper()
                    .readValue(response.body().await().toString(StandardCharsets.UTF_8))
                throw IllegalStateException("Unexpected error response: $error")
            }
        }
    }

    @Test
    fun getMovies_paging_success() {
        LOGGER.info("test getMovies_paging_success")
        runBlocking {
            val expectedMovies = (0 until 5).map { i ->
                Movie(
                    title = UUID.randomUUID().toString(),
                    yearReleased = 1950+i,
                    director = UUID.randomUUID().toString(),
                    producer = UUID.randomUUID().toString(),
                    studio = UUID.randomUUID().toString(),
                    colorProcess = ColorProcess.values().random(),
                    genre = Genre.values().random(),
                )
            }
                .sortedByDescending { it.yearReleased }
            TestSession.movieStore.insertMovies(expectedMovies)

            val expectedActorsForMovies: List<List<Actor>> = expectedMovies.indices.map { i ->
                val actors = (0 until 5).map { j ->
                    Actor(
                        name = UUID.randomUUID().toString(),
                        startYearOfWork = Random.nextInt(),
                        endYearOfWork = Random.nextInt(),
                        gender = if (i % 2 == 0) "female" else "male",
                        yearOfBirth = Random.nextInt()
                    )
                }
                TestSession.movieStore.insertActors(actors)

                val movie = expectedMovies[i]
                val casts = actors.map { actor ->
                    Cast(
                        movieTitle = movie.title,
                        actorName = actor.name,
                        role = ActorRole.values().random()
                    )
                }
                TestSession.movieStore.insertCasts(casts)
                actors
            }

            val query = URLEncoder.encode("All movies", StandardCharsets.UTF_8.toString())
            val pageLimit = 2
            val pageCount = expectedMovies.size / pageLimit
            (0 until pageCount).forEach { pageIndex ->
                val offset = pageLimit * pageIndex

                val options = RequestOptions()
                    .setMethod(HttpMethod.GET)
                    .setAbsoluteURI("http://localhost:8081/movies?query=$query&limit=$pageLimit&offset=$offset")
                val request = TestSession.vertx.createHttpClient()
                    .request(options)
                    .await()
                request.end().await()
                val response = request.response().await()
                val statusCode = response.statusCode()
                if (statusCode == 200) {
                    val body = response.body().await().toString(StandardCharsets.UTF_8)
                    val totalCount = response.getHeader("X-Total-Count").toInt()
                    LOGGER.debug("Response Body: $body")
                    LOGGER.debug("Total Count: $totalCount")
                    val results: MoviesResponse = DatabindCodec.mapper().readValue(body)

                    val expectedMoviePage = expectedMovies.subList(offset, offset + pageLimit)
                    val expectedActorsForMoviesPage = expectedActorsForMovies.subList(offset, offset + pageLimit)
                    val expectedMoviesWithActors = expectedMoviePage.indices.map { expectedPageIndex ->
                        MovieWithActors(
                            movie = expectedMoviePage[expectedPageIndex],
                            actors = expectedActorsForMoviesPage[expectedPageIndex]
                        )
                    }

                    Assertions.assertThat(results.generatedSql).isNotNull()
                    Assertions.assertThat(results.generatedSqlValid).isTrue
                    Assertions.assertThat(results.moviesWithActors)
                        .hasSize(pageLimit)
                        .usingRecursiveComparison()
                        .ignoringCollectionOrder()
                        .isEqualTo(expectedMoviesWithActors)

                    Assertions.assertThat(totalCount).isEqualTo(expectedMovies.size)
                } else {
                    val error: ErrorResponse = DatabindCodec.mapper()
                        .readValue(response.body().await().toString(StandardCharsets.UTF_8))
                    throw IllegalStateException("Unexpected error response: $error")
                }
            }
        }
    }
}