package com.text2sql4j.api.models.codecs

import com.text2sql4j.translator.models.SqlTranslateInputs

class SqlTranslateInputsCodec: JacksonCodec<SqlTranslateInputs>(SqlTranslateInputs::class.java)