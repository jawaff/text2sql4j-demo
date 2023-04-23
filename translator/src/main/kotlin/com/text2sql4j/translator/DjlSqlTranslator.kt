package com.text2sql4j.translator

import ai.djl.inference.Predictor
import ai.djl.modality.Input
import ai.djl.modality.Output
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ZooModel
import ai.djl.translate.TranslateException
import ai.djl.util.Utils
import com.text2sql4j.translator.models.SqlTranslateInputs
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import java.nio.file.Path

class DjlSqlTranslator(modelPath: Path) : SqlTranslator {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(DjlSqlTranslator::class.java)
        private const val PYTHON_EXE_ENV_VAR = "PYTHON_EXECUTABLE"
    }

    private val model: ZooModel<Input, Output>
    private val predictor: Predictor<Input, Output>

    init {
        val pythonExe = Utils.getenv(PYTHON_EXE_ENV_VAR)
            ?: throw IllegalStateException("'$PYTHON_EXE_ENV_VAR' environment variable has not been set!")

        val criteria = Criteria.builder()
            .setTypes(Input::class.java, Output::class.java)
            .optModelPath(modelPath)
            .optEngine("Python")
            .optOption("pythonExecutable", pythonExe)
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
        return predictor.predict(
            prepareInput(
                inputs.query,
                "${inputs.dbId} | ${inputs.expectedPrefix}",
                inputs.isIncremental
            )
        )
            .getAsString("sql").removePrefix("${inputs.dbId} | ")
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