package com.text2sql4j.translator

import com.text2sql4j.translator.models.SqlTranslateInputs
import com.text2sql4j.translator.utils.SpiderUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

class DjlSqlTranslatorTest {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(DjlSqlTranslatorTest::class.java)

        fun countMatchedTokens(sql: String, expectedSqlTokens: List<String>): Int {
            var curIndex = 0
            var matchCount = 0
            expectedSqlTokens.forEach { token ->
                // Just for debugging to make sure the content overlap algorithm is working.
                LOGGER.trace("Token: '$token' SQL Substring: '${sql.substring(curIndex)}'")

                val matchIndex = sql.indexOf(string = token, startIndex = curIndex, ignoreCase = true)
                if (matchIndex != -1) {
                    matchCount++
                    curIndex = matchIndex + token.length
                }
            }

            return matchCount
        }
    }

    @Test
    fun translate_expectedPrefix() {
        val expectedPrefix =  "select t1.concert_name from concert as t1"
        val begin = Instant.now()
        val sqlOutput = VertxSession.translator.translate(
            SqlTranslateInputs(
                query = "Get concerts with the largest stadiums. | concert_singer | stadium : stadium_id, location, name, capacity, highest, lowest, average | singer : singer_id, name, country, song_name, song_release_year, age, is_male | concert : concert_id, concert_name, theme, stadium_id, year | singer_in_concert : concert_id, singer_id",
                dbId = "concert_singer",
                expectedPrefix = expectedPrefix,
                isIncremental = false
            )
        )
        val end = Instant.now()
        LOGGER.debug("Translation Time: ${Duration.between(begin, end).seconds} seconds")

        LOGGER.debug("Output: '$sqlOutput'")

        // At the very least the resulting SQL should start with the expected prefix that we provided to the translator.
        Assertions.assertTrue(sqlOutput.contains(expectedPrefix))
    }

    @Test
    fun batchTranslate() {
        val count = 5
        val queryInfos = (0 until count).map { i -> SpiderUtil.getQueryInfo(i) }
        val inputBatches: List<SqlTranslateInputs> = queryInfos.map { queryInfo ->
            val inputQuery = SpiderUtil.getTranslatorInput(queryInfo)

            SqlTranslateInputs(
                expectedPrefix = "",
                query = inputQuery,
                dbId = queryInfo.db_id,
                isIncremental = false
            )
        }

        val inputQueries = inputBatches.map { input -> input.query }
        val dbIds = inputBatches.map { input -> input.dbId }

        val begin = Instant.now()
        val outputs = VertxSession.translator.translateBatch(
            inputQueries = inputQueries,
            dbIds = dbIds,
            expectedPrefix = "",
            isIncremental = false
        )
        val end = Instant.now()
        LOGGER.debug("Batch Translation Time: ${Duration.between(begin, end).seconds} seconds")
        for (output in outputs) {
            LOGGER.debug("Output: $output")
        }
        Assertions.assertEquals(count, outputs.size)
    }

    @Test
    fun spider_dataset() {
        val count = SpiderUtil.getQueryInfoCount().coerceAtMost(100)

        var totalExpectedTokens = 0
        val totalMatchCount = (0 until count).sumOf { i ->
            val queryInfo = SpiderUtil.getQueryInfo(i)
            val inputQuery = SpiderUtil.getTranslatorInput(queryInfo)
            LOGGER.trace("Translator Input: '$inputQuery'")

            try {
                val begin = Instant.now()
                val sqlOutput = VertxSession.translator.translateBatch(
                    inputQueries = listOf(inputQuery),
                    dbIds = listOf(queryInfo.db_id),
                    expectedPrefix = "",
                    isIncremental = false
                )[0]
                val end = Instant.now()
                LOGGER.debug("Translation Time: ${Duration.between(begin, end).seconds} seconds")

                LOGGER.trace("Output: '$sqlOutput'")
                LOGGER.trace("Expected Output: '${queryInfo.query}'")

                LOGGER.debug("${(i+1).toFloat() / count.toFloat() * 100f}% Completed")

                totalExpectedTokens += queryInfo.query_toks.size
                countMatchedTokens(sqlOutput, queryInfo.query_toks)
            } catch (t: Throwable) {
                LOGGER.error("Failed translating: $inputQuery", t)
                0
            }
        }

        LOGGER.info("Prediction Accuracy: ${totalMatchCount.toFloat() / totalExpectedTokens.toFloat() * 100f}%")
    }
}