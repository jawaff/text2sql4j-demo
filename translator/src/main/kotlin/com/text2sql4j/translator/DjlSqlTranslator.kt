package com.text2sql4j.translator

import ai.djl.inference.Predictor
import ai.djl.modality.Input
import ai.djl.modality.Output
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ZooModel
import com.fasterxml.jackson.module.kotlin.readValue
import com.text2sql4j.translator.models.SqlTranslateInputs
import io.vertx.core.json.jackson.DatabindCodec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

class DjlSqlTranslator(modelPath: Path) : SqlTranslator {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(DjlSqlTranslator::class.java)
    }

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

    private fun prepareInput(query: String, expectedPrefix: String, isIncremental: Boolean): Input {
        val input = Input()
        input.add("expectedPrefix", expectedPrefix)
        input.add("query", query)
        input.add("isIncremental", if (isIncremental) "True" else "False")
        return input
    }

    override fun translate(inputs: SqlTranslateInputs): String {
        return try {
            val output = predictor.predict(
                prepareInput(
                    inputs.query,
                    "${inputs.dbId} | ${inputs.expectedPrefix}",
                    inputs.isIncremental
                )
            )

            output.getAsString("sql").removePrefix("${inputs.dbId} | ")
        } catch (t: Throwable) {
            t.printStackTrace()
            throw t
        }
    }

    override fun translateBatch(
        inputQueries: List<String>,
        dbIds: List<String>,
        expectedPrefix: String,
        isIncremental: Boolean
    ): List<String> {
        return predictor.batchPredict(
            inputQueries.map { query ->
                prepareInput(
                    query,
                    if (expectedPrefix.isBlank()) "" else "${dbIds[0]} | $expectedPrefix",
                    isIncremental
                )
            }
        )
            .mapIndexed { i, output -> output.getAsString("sql").removePrefix("${dbIds[i]} | ") }
    }

    override fun close() {
        this.predictor.close()
        this.model.close()
    }
}