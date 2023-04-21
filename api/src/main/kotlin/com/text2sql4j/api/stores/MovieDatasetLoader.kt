package com.text2sql4j.api.stores

import com.text2sql4j.api.models.movies.*
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader

object MovieDatasetLoader {
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
                    columns[4].substringAfter("D:")
                } catch (t: Throwable) {
                    null
                }
                val producer = try {
                    columns[5].substringAfter("P:")
                } catch (t: Throwable) {
                    null
                }
                val studio = try {
                    // Attempts to strip 'St:' and 'S:'.
                    val tmp = columns[6].substringAfter("St:")
                        .substringAfter("S:")
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
                val colorProcess = ColorProcess.fromCode(columns[7])
                val genre = Genre.fromCode(columns[8])

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

    fun loadDataset(movieStore: MovieStore) {
        runBlocking {
            val actors = loadActorDataset()
            movieStore.insertActors(actors)
            val movies = loadMovieDataset()
            movieStore.insertMovies(movies)
            val casts = loadCastDataset()
            movieStore.insertCasts(casts)
        }
    }
}