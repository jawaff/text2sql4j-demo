package com.text2sql4j.api.models.movies

data class MoviesResponse(
    val generatedSql: String?,
    val generatedSqlValid: Boolean?,
    val moviesWithActors: List<MovieWithActors>
)