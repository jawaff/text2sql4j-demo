package com.text2sql4j.translator

interface SqlTranslator {
    suspend fun translate(text: String): String
}