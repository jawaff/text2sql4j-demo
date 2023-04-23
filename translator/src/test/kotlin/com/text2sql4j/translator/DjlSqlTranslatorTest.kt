package com.text2sql4j.translator

import ai.djl.translate.TranslateException
import com.text2sql4j.translator.models.SqlTranslateInputs
import com.text2sql4j.translator.utils.SpiderUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import java.lang.NullPointerException
import java.time.Duration
import java.time.Instant

class DjlSqlTranslatorTest {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(DjlSqlTranslatorTest::class.java)

        /**
         * This is where the core of the content overlap evaluation. It must be noted that the generated SQL and
         * the expected SQL may have a different amount of spaces and formatting. This algorithm is not perfect
         * by any means, but gets a general idea of the content overlap.
         */
        fun countMatchedTokens(sql: String, expectedSqlTokens: List<String>): Int {
            var curIndex = 0
            var matchCount = 0
            expectedSqlTokens.forEach { token ->
                // Just for debugging to make sure the content overlap algorithm is working.
                LOGGER.trace("Token: '$token' SQL Substring: '${sql.substring(curIndex)}'")

                val matchIndex = sql.indexOf(string = token, startIndex = curIndex, ignoreCase = true)
                if (matchIndex != -1) {
                    // If there's a match at a much later index than expected, then that token won't count as a match!
                    if ((matchIndex - curIndex) < 5) {
                        matchCount++
                    }
                    curIndex = matchIndex + token.length
                }
            }

            return matchCount
        }

        /**
         * While running the spider evaluation timeouts caused by difficult queries result in the Python server
         * being restarted and this prevents subsequent tests from failing due to the startup time.
         */
        fun retryBatchTranslate(inputQueries: List<String>, dbIds: List<String>, maxRetries: Int = 5): List<String> {
            var result: List<String>? = null
            var retryCount = 0
            var lastError: Throwable? = null
            while (retryCount < maxRetries) {
                try {
                    val begin = Instant.now()
                    result = TestSession.translator.translateBatch(
                        inputQueries = inputQueries,
                        dbIds = dbIds,
                        expectedPrefix = "",
                        isIncremental = false
                    )
                    val end = Instant.now()
                    LOGGER.debug("Translation Time: ${Duration.between(begin, end).seconds} seconds")
                    break
                } catch (e: TranslateException) {
                    // This message implies that the Python backend is restarting.
                    if (
                        e.message?.contains("Backend Python process is stopped.") == true ||
                        e.message?.contains("Python worker disconnected.") == true
                    ) {
                        retryCount += 1
                        lastError = e
                        LOGGER.debug("Retrying translation due to Python process restarting...")
                        // It should take less than 10 seconds to restart the backend.
                        Thread.sleep(10_000)
                    } else {
                        throw e
                    }
                } catch (e: NullPointerException) {
                    // While the Python server is starting up we may get a null output.
                    retryCount += 1
                    lastError = e
                    LOGGER.debug("Retrying translation due to Python process restarting...")
                    // It should take less than 10 seconds to restart the backend.
                    Thread.sleep(10_000)
                } catch (t: Throwable) {
                    throw t
                }
            }
            return result ?: throw lastError ?: IllegalStateException("Failed to translate")
        }
    }

    @Test
    fun translate_expectedPrefix() {
        LOGGER.info("test translate_expectedPrefix")
        val expectedPrefix =  "select t1.concert_name from concert as t1"
        val begin = Instant.now()
        val sqlOutput = TestSession.translator.translate(
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
        LOGGER.info("test batchTranslate")
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

        val outputs = retryBatchTranslate(inputQueries, dbIds)
        for (output in outputs) {
            LOGGER.debug("Output: $output")
        }
        Assertions.assertEquals(count, outputs.size)
    }

    @Test
    fun spider_dataset() {
        LOGGER.info("test spider_dataset")
        val count = SpiderUtil.getQueryInfoCount().coerceAtMost(100)

        val shuffledIndices = (0 until SpiderUtil.getQueryInfoCount()).shuffled()
            .subList(0, count)

        var totalExpectedTokens = 0
        val totalMatchCount = shuffledIndices.sumOf { i ->
            val queryInfo = SpiderUtil.getQueryInfo(i)
            val inputQuery = SpiderUtil.getTranslatorInput(queryInfo)
            LOGGER.trace("Translator Input: '$inputQuery'")

            try {
                val sqlOutput = retryBatchTranslate(listOf(inputQuery), listOf(queryInfo.db_id))[0]

                LOGGER.trace("Output: '$sqlOutput'")
                LOGGER.trace("Expected Output: '${queryInfo.query}'")

                LOGGER.trace("${(i+1).toFloat() / count.toFloat() * 100f}% Completed")

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