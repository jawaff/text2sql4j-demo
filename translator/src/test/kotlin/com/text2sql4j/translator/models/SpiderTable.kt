package com.text2sql4j.translator.models

data class SpiderTable(
    val db_id: String,
    // Ex: [[1, 'column name'], [1, 'other column name']]
    val column_names: List<List<Any>>,
    // Ex: [[1, 'column_name'], [1, 'other_column_name']]
    val column_names_original: List<List<Any>>,
    val column_types: List<String>,
    val foreign_keys: List<List<Int>>,
    val primary_keys: List<Int>,
    val table_names: List<String>,
    val table_names_original: List<String>
)