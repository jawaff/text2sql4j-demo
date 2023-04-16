package com.text2sql4j.translator.experiment

interface SqlTranslator {
    suspend fun translate(text: String): String
}