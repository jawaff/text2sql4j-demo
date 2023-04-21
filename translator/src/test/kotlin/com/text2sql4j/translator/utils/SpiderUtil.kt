package com.text2sql4j.translator.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.text2sql4j.translator.models.SpiderQueryInfo
import com.text2sql4j.translator.models.SpiderTable
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object SpiderUtil {
    private val mapper = ObjectMapper()
        .registerKotlinModule()
    private val tables: List<SpiderTable> = mapper.readValue(
        javaClass.getResourceAsStream("/spiderSql/tables.json")!!
    )
    private val queryInfos: List<SpiderQueryInfo> = mapper.readValue(
        javaClass.getResourceAsStream("/spiderSql/dev.json")!!
    )

    fun getQueryInfoCount(): Int {
        return queryInfos.size
    }

    fun getQueryInfo(index: Int): SpiderQueryInfo {
        return queryInfos[index]
    }

    fun getTranslatorInput(queryInfo: SpiderQueryInfo): String {
        val table = tables.find { table -> table.db_id == queryInfo.db_id }!!

        // Adds each table and its columns to the resulting query.
        return "${queryInfo.question} | ${queryInfo.db_id} | " + table.table_names.mapIndexed { i, tableName ->
            // Fetches the column names relevant to the current table.
            "$tableName : " + table.column_names_original.filter { columnName ->
                (columnName[0] as Int) == i
            }.joinToString(", ") { columnName ->
                columnName[1] as String
            }
        }
            .joinToString(" | ")
    }
}