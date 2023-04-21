package com.text2sql4j.api.models.movies

data class MoviesResponse(
    val generatedSql: String?,
    val moviesWithActors: List<MovieWithActors>
)