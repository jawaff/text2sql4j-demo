package com.text2sql4j.translator.experiment

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import java.nio.file.Path

class PostgreSQLTranslator(modelPath: Path, tokenizerPath: Path) : SqlTranslator {
    private val tokenizer = HuggingFaceTokenizer.newInstance(tokenizerPath)

    override suspend fun translate(text: String): String {
        val encoding = tokenizer.encode(text)
        encoding.ids
        encoding.attentionMask
        return ""
    }
}