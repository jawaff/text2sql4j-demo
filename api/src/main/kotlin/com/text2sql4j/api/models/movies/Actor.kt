package com.text2sql4j.api.models.movies

data class Actor(
    val name: String,
    val startYearOfWork: Int?,
    val endYearOfWork: Int?,
    val gender: String?,
    val yearOfBirth: Int?
)
