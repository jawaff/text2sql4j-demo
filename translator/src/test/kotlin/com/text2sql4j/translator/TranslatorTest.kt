package com.text2sql4j.translator

import ai.djl.inference.Predictor
import ai.djl.modality.nlp.qa.QAInput
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ZooModel
import ai.djl.training.util.ProgressBar
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class TranslatorTest {
    @Test
    fun importModel() {
        val input = QAInput("asdf", "asdf")

        val translator: PicardTranslator = PicardTranslator()
        val criteria: Criteria<QAInput, String> = Criteria.builder()
            .setTypes(QAInput::class.java, String::class.java)
            .optModelPath(Paths.get("/home/jake/School/NLP/text2sql4j/raw-files/picard_model.pt"))
            .optTranslator(translator)
            .optProgress(ProgressBar()).build()
        val model: ZooModel<QAInput, String> = criteria.loadModel()
        val predictor: Predictor<QAInput, String> = model.newPredictor(translator)
        val output = predictor.predict(input)
        println(output)
    }
}