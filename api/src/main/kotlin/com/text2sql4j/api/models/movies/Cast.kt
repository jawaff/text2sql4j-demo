package com.text2sql4j.api.models.movies

data class Cast(
    val movieTitle: String,
    val actorName: String,
    val role: ActorRole
)