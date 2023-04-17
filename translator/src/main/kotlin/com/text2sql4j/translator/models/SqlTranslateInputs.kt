package com.text2sql4j.translator.models

data class SqlTranslateInputs(val expectedPrefix: String, val query: String, val dbId: String, val isIncremental: Boolean)