package com.text2sql4j.translator

import com.text2sql4j.translator.models.SqlTranslateInputs

interface SqlTranslator : AutoCloseable {
    fun translate(inputs: SqlTranslateInputs): String
}