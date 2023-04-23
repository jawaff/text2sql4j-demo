package com.text2sql4j.api.stores

import com.text2sql4j.api.TestSession
import com.text2sql4j.api.models.movies.Movie
import com.text2sql4j.api.models.movies.Actor
import com.text2sql4j.api.models.movies.Cast
import com.text2sql4j.api.models.movies.Genre
import com.text2sql4j.api.models.movies.ColorProcess
import com.text2sql4j.api.models.movies.ActorRole
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.random.Random

class MovieStoreTest {
    @AfterEach
    @BeforeEach
    fun clearDatabase() {
        TestSession.clearDatabase()
    }

    @Test
    fun getMovies_defaultSql() {
        runBlocking {
            val expectedMovies = (0 until 5).map {
                Movie(
                    title = UUID.randomUUID().toString(),
                    yearReleased = Random.nextInt(),
                    director = UUID.randomUUID().toString(),
                    producer = UUID.randomUUID().toString(),
                    studio = UUID.randomUUID().toString(),
                    colorProcess = ColorProcess.values().random(),
                    genre = Genre.values().random(),
                )
            }
                .sortedByDescending { it.yearReleased }
            TestSession.movieStore.insertMovies(expectedMovies)

            val results = TestSession.movieStore.getMovies(null, 0, 10)
            Assertions.assertThat(results)
                .hasSize(expectedMovies.size)
                .isEqualTo(expectedMovies)
        }
    }

    @Test
    fun getMovies_generatedSql() {
        runBlocking {
            val expectedMovies = (0 until 5).map {
                Movie(
                    title = UUID.randomUUID().toString(),
                    yearReleased = Random.nextInt(),
                    director = UUID.randomUUID().toString(),
                    producer = UUID.randomUUID().toString(),
                    studio = UUID.randomUUID().toString(),
                    colorProcess = ColorProcess.values().random(),
                    genre = Genre.values().random(),
                )
            }
                .sortedByDescending { it.yearReleased }
            TestSession.movieStore.insertMovies(expectedMovies)

            val expectedMovie = expectedMovies[0]
            val fakeGeneratedSql = MovieStore.EXPECTED_GET_MOVIES_PREFIX
                .plus(" WHERE t1.movie_title = '${expectedMovie.title}'")

            val results = TestSession.movieStore.getMovies(fakeGeneratedSql, 0, 10)
            Assertions.assertThat(results)
                .hasSize(1)
                .contains(expectedMovie)
        }
    }

    @Test
    fun getMovies_generatedSql_orderByGroupByIgnored() {
        runBlocking {
            val expectedMovies = (0 until 5).map {
                Movie(
                    title = UUID.randomUUID().toString(),
                    yearReleased = Random.nextInt(),
                    director = UUID.randomUUID().toString(),
                    producer = UUID.randomUUID().toString(),
                    studio = UUID.randomUUID().toString(),
                    colorProcess = ColorProcess.values().random(),
                    genre = Genre.values().random(),
                )
            }
                .sortedByDescending { it.yearReleased }
            TestSession.movieStore.insertMovies(expectedMovies)

            val fakeGeneratedSql = MovieStore.EXPECTED_GET_MOVIES_PREFIX
                // Group by and order by clauses should be ignored and not affect the results.
                .plus(" group by t1.year_released order by t1.movie_title ASC")

            val results = TestSession.movieStore.getMovies(fakeGeneratedSql, 0, 10)
            Assertions.assertThat(results)
                .hasSize(expectedMovies.size)
                .isEqualTo(expectedMovies)
        }
    }

    @Test
    fun getMovies_paging() {
        runBlocking {
            val expectedMovies = (0 until 10).map {
                Movie(
                    title = UUID.randomUUID().toString(),
                    yearReleased = Random.nextInt(),
                    director = UUID.randomUUID().toString(),
                    producer = UUID.randomUUID().toString(),
                    studio = UUID.randomUUID().toString(),
                    colorProcess = ColorProcess.values().random(),
                    genre = Genre.values().random(),
                )
            }
                .sortedByDescending { it.yearReleased }
            TestSession.movieStore.insertMovies(expectedMovies)

            val pageLimit = 2
            val pageCount = expectedMovies.size / pageLimit
            (0 until pageCount).forEach { pageIndex ->
                val offset = pageLimit * pageIndex
                val expectedPage = expectedMovies.subList(offset, offset + pageLimit)
                val results = TestSession.movieStore.getMovies(null, offset, pageLimit)
                Assertions.assertThat(results)
                    .hasSize(pageLimit)
                    .isEqualTo(expectedPage)
            }
        }
    }

    @Test
    fun getActorsInMovie() {
        runBlocking {
            val expectedMovies = (0 until 5).map {
                Movie(
                    title = UUID.randomUUID().toString(),
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

            val results = TestSession.movieStore.getActorsInMovie(expectedMovie.title)
            Assertions.assertThat(results)
                .hasSize(expectedActorsForMovie.size)
                .isEqualTo(expectedActorsForMovie)
        }
    }

    @Test
    fun getMoviesCount() {
        runBlocking {
            val expectedMovies = (0 until 5).map {
                Movie(
                    title = UUID.randomUUID().toString(),
                    yearReleased = Random.nextInt(),
                    director = UUID.randomUUID().toString(),
                    producer = UUID.randomUUID().toString(),
                    studio = UUID.randomUUID().toString(),
                    colorProcess = ColorProcess.values().random(),
                    genre = Genre.values().random(),
                )
            }
                .sortedByDescending { it.yearReleased }
            TestSession.movieStore.insertMovies(expectedMovies)

            val expectedMovie = expectedMovies[0]
            val fakeGeneratedSql = MovieStore.EXPECTED_GET_MOVIES_PREFIX
                .plus(" WHERE t1.movie_title = '${expectedMovie.title}'")

            TestSession.movieStore.getMovieCount(fakeGeneratedSql)
                .let { count ->
                    Assertions.assertThat(count).isEqualTo(1)
                }

            TestSession.movieStore.getMovieCount(null)
                .let { count ->
                    Assertions.assertThat(count).isEqualTo(expectedMovies.size)
                }
        }
    }
}