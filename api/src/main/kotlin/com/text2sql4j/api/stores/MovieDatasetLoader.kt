package com.text2sql4j.api.stores

import com.text2sql4j.api.models.movies.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader

object MovieDatasetLoader {
    private val LOGGER: Logger = LoggerFactory.getLogger(MovieDatasetLoader::class.java)

    private fun loadResource(path: String): String {
        return javaClass.getResourceAsStream(path)
            .bufferedReader().use(BufferedReader::readText)
    }

    private fun splitColumns(line: String): List<String> {
        return line.removePrefix("<tr><td>")
            .split("<td>")
            .map { it.trim() }
            .filter { it != "|" }
    }

    private fun loadActorDataset(): List<Actor> {
        val content = loadResource("/movie-dataset/actors.html")
        val lines = content.split("\n")
        return lines.asSequence()
            .filter { line -> line.startsWith("<tr><td>") }
            .map { line -> splitColumns(line) }
            .filter { columns -> columns.size >= 6 }
            .map { columns -> columns.map { column -> column.trim() }}
            .map { columns ->
                val name = columns[0]
                val startYearOfWork = try {
                    columns[1].substringBefore("-", "")
                        .trim()
                        .ifBlank { null }
                        ?.toInt()
                } catch (t: Throwable) {
                    null
                }
                val endYearOfWork = try {
                    columns[1].substringAfter("-", "")
                        .trim()
                        .ifBlank { null }
                        ?.toInt()
                } catch (t: Throwable) {
                    null
                }
                val gender = if (columns[4] == "M") {
                    "male"
                } else if (columns[4] == "F") {
                    "female"
                } else {
                    null
                }
                val yearOfBirth = try {
                    columns[5].toInt()
                } catch (t: Throwable) {
                    null
                }
                Actor(
                    name = name,
                    startYearOfWork = startYearOfWork,
                    endYearOfWork = endYearOfWork,
                    gender = gender,
                    yearOfBirth = yearOfBirth
                )
            }
            .toList()
    }

    private fun loadMovieDataset(): List<Movie> {
        val content = loadResource("/movie-dataset/main.html")
        val lines = content.split("\n")
        return lines.asSequence()
            .filter { line -> line.startsWith("<tr><td>") }
            .map { line -> splitColumns(line) }
            .filter { columns -> columns.size >= 9 }
            .map { columns -> columns.map { column -> column.trim() }}
            .mapNotNull { columns ->
                val title = try {
                    columns[1].substringAfter("T:")
                } catch (t: Throwable) {
                    null
                }
                val yearReleased = try {
                    columns[2].toInt()
                } catch (t: Throwable) {
                    null
                }
                val director = try {
                    columns[3].substringAfter("D:")
                } catch (t: Throwable) {
                    null
                }
                val producer = try {
                    columns[4].substringAfter("P:")
                        .substringAfter("PN:")
                        .substringAfter("PZ:")
                        .substringAfter("PU:")
                        .trim()
                } catch (t: Throwable) {
                    null
                }
                val studio = try {
                    // Attempts to strip 'St:' and 'S:'.
                    val tmp = columns[5].substringAfter("St:")
                        .substringAfter("S:")
                        .substringAfter("SU:")
                        .trim()
                    // 'SD:' Shows up after the studio name sometimes.
                    if (tmp.contains(":")) {
                        tmp.substringBefore("SD:").trim()
                    } else {
                        tmp
                    }
                } catch (t: Throwable) {
                    null
                }
                val colorProcess = ColorProcess.fromCode(columns[6])
                val genre = Genre.fromCode(columns[7])

                if (title != null && yearReleased != null) {
                    Movie(
                        title = title,
                        yearReleased = yearReleased,
                        director = director,
                        producer = producer,
                        studio = studio,
                        colorProcess = colorProcess,
                        genre = genre
                    )
                } else {
                    null
                }
            }
            .toList()
    }

    private fun loadCastDataset(): List<Cast> {
        val content = loadResource("/movie-dataset/casts.html")
        val lines = content.split("\n")
        return lines.asSequence()
            .filter { line -> line.startsWith("<tr><td>") }
            .map { line -> splitColumns(line) }
            .filter { columns -> columns.size >= 5 }
            .map { columns -> columns.map { column -> column.trim() }}
            .mapNotNull { columns ->
                val title = try {
                    columns[1].substringAfter("T:")
                } catch (t: Throwable) {
                    null
                }
                val actor = try {
                    columns[2]
                } catch (t: Throwable) {
                    null
                }
                val role = ActorRole.fromCode(columns[4])
                if (title != null && actor != null) {
                    Cast(
                        movieTitle = title,
                        actorName = actor,
                        role = role
                    )
                } else {
                    null
                }
            }
            .toList()
    }

    suspend fun loadDataset(movieStore: MovieStore) {
        val actors = loadActorDataset()
        LOGGER.info("Inserting ${actors.size} Actors")
        movieStore.insertActors(actors)
        LOGGER.info("Inserted ${actors.size} Actors")
        val movies = loadMovieDataset()
        LOGGER.info("Inserting ${movies.size} Movies")
        movieStore.insertMovies(movies)
        LOGGER.info("Inserted ${movies.size} Movies")
        val casts = loadCastDataset()
        LOGGER.info("Inserting ${casts.size} Casts")
        movieStore.insertCasts(casts)
        LOGGER.info("Inserted ${casts.size} Casts")
    }
}