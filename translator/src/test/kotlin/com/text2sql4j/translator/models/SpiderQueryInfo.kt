package com.text2sql4j.translator.models

data class SpiderQueryInfo(
    val db_id: String,
    val query: String,
    val query_toks: List<String>,
    val query_toks_no_value: List<String>,
    val question: String,
    val question_toks: List<String>,
    val sql: Any
)