package com.text2sql4j.translator

import com.text2sql4j.translator.models.SqlTranslateInputs
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

class TranslatorVerticle(private val modelPath: Path) : CoroutineVerticle() {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(TranslatorVerticle::class.java)
        const val SQL_TRANSLATE_ADDR = "translate.sql"
    }

    private lateinit var translator: SqlTranslator

    override suspend fun start() {
        translator = DjlSqlTranslator(modelPath)

        vertx.eventBus().consumer<SqlTranslateInputs>(SQL_TRANSLATE_ADDR)
            .handler { message ->
                try {
                    val output = translator.translate(message.body())
                    message.reply(output)
                } catch (t: Throwable) {
                    LOGGER.error("Failed to generate Sql", t)
                    message.fail(500, "Failed to generate Sql: ${t.message}")
                }
            }
    }

    override suspend fun stop() {
        translator.close()
    }
}