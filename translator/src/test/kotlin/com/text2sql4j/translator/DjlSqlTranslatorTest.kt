package com.text2sql4j.translator

import com.text2sql4j.translator.models.SqlTranslateInputs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant


class DjlSqlTranslatorTest {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(DjlSqlTranslatorTest::class.java)
    }

    @Test
    fun translateTest() {
        val translator = DjlSqlTranslator(Paths.get("../python-translator/"))

        val expectedPrefix =  "concert_singer | select t1.concert_name from concert as t1"
        val begin = Instant.now()
        val output = translator.translate(
            SqlTranslateInputs(
                expectedPrefix = expectedPrefix,
                query = "Get concerts with the largest stadiums. | concert_singer | stadium : stadium_id, location, name, capacity, highest, lowest, average | singer : singer_id, name, country, song_name, song_release_year, age, is_male | concert : concert_id, concert_name, theme, stadium_id, year | singer_in_concert : concert_id, singer_id",
                isIncremental = false
            )
        )
        val end = Instant.now()
        LOGGER.debug("Translation Time: ${Duration.between(begin, end).seconds} seconds")

        LOGGER.debug("SQL Output: '$output'")

        // At the very least the resulting SQL should start with the expected prefix that we provided to the translator.
        Assertions.assertTrue(output.startsWith(expectedPrefix))
    }
}