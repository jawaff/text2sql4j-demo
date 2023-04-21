package com.text2sql4j.api.models.movies

data class Movie(
    val title: String,
    val yearReleased: Int,
    val director: String?,
    val producer: String?,
    val studio: String?,
    val colorProcess: ColorProcess,
    val genre: Genre
)