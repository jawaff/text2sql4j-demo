package com.text2sql4j.translator

import com.text2sql4j.translator.models.SqlTranslateInputs

interface SqlTranslator : AutoCloseable {
    fun translate(inputs: SqlTranslateInputs): String

    /**
     * This translates multiple SQL queries, but has some restrictions. First, `inputQueries` and `dbIds` must have
     * the same sizes and be relevant to each other. Second, `expectedPrefix` and `isIncremental` applies to each
     * of the translation jobs that are being done.
     */
    fun translateBatch(
        inputQueries: List<String>,
        dbIds: List<String>,
        expectedPrefix: String,
        isIncremental: Boolean
    ): List<String>
}