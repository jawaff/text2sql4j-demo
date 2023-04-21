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
import org.junit.jupiter.api.BeforeEach
import java.net.URLEncoder

class MovieResourceTest {
    @BeforeEach
    fun clearDatabase() {
        TestSession.clearDatabase()
    }

    @Test
    fun getMovies_success() {
        runBlocking {
            val expectedMovies = (0 until 5).map { i ->
                Movie(
                    title = "banana$i",
                    yearReleased = Random.nextInt(),
                    director = UUID.randomUUID().toString(),
                    producer = UUID.randomUUID().toString(),
                    studio = UUID.randomUUID().toString(),
                    colorProcess = ColorProcess.values().random(),
                    genre = Genre.values().random(),
                )
            }
            TestSession.movieStore.insertMovies(expectedMovies)

            val expectedActors = (0 until 25).map { i ->
                Actor(
                    name = UUID.randomUUID().toString(),
                    startYearOfWork = Random.nextInt(),
                    endYearOfWork = Random.nextInt(),
                    gender = if (i % 2 == 0) "female" else "male",
                    yearOfBirth = Random.nextInt()
                )
            }
            TestSession.movieStore.insertActors(expectedActors)

            val pageLimit = expectedActors.size / expectedMovies.size
            val expectedCasts = expectedMovies.flatMapIndexed { i, movie ->
                val offset = pageLimit * i
                val actorPage = expectedActors.subList(offset, offset + pageLimit)

                actorPage.map { actor ->
                    Cast(
                        movieTitle = movie.title,
                        actorName = actor.name,
                        role = ActorRole.values().random()
                    )
                }
            }
            TestSession.movieStore.insertCasts(expectedCasts)

            val expectedMovie = expectedMovies[0]
            val expectedActorsForMovie = expectedActors.subList(0, pageLimit)
                .sortedBy { it.name }

            val query = URLEncoder.encode("Movies with banana in title", StandardCharsets.UTF_8.toString())
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
                val results: List<MovieWithActors> = DatabindCodec.mapper()
                    .readValue(response.body().await().toString(StandardCharsets.UTF_8))
                println(results)
            } else {
                val error: ErrorResponse = DatabindCodec.mapper()
                    .readValue(response.body().await().toString(StandardCharsets.UTF_8))
                println(error)
            }
        }
    }
}