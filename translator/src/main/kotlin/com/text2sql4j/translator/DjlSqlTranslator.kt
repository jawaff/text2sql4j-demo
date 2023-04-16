package com.text2sql4j.translator

import ai.djl.inference.Predictor
import ai.djl.modality.Input
import ai.djl.modality.Output
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ZooModel
import com.fasterxml.jackson.module.kotlin.readValue
import com.text2sql4j.translator.models.SqlTranslateInputs
import io.vertx.core.json.jackson.DatabindCodec
import java.nio.file.Path

class DjlSqlTranslator(modelPath: Path) : SqlTranslator {
    private val model: ZooModel<Input, Output>
    private val predictor: Predictor<Input, Output>

    init {
        val criteria = Criteria.builder()
            .setTypes(Input::class.java, Output::class.java)
            .optModelPath(modelPath)
            .optEngine("Python")
            .build()
        this.model = criteria.loadModel()
        this.predictor = model.newPredictor()
    }

    override fun translate(inputs: SqlTranslateInputs): String {
        val input = Input()
        input.add("expectedPrefix", inputs.expectedPrefix)
        input.add("query", inputs.query)
        input.add("isIncremental", if (inputs.isIncremental) "True" else "False")
        val output = predictor.predict(input)

        val sqlList: List<String> = DatabindCodec.mapper().readValue(output.getAsString("sql"))
        return sqlList[0]
    }

    override fun close() {
        this.predictor.close()
        this.model.close()
    }
}