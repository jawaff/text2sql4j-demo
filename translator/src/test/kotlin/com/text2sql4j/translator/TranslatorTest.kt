package com.text2sql4j.translator

import ai.djl.Model
import ai.djl.modality.Classifications
import ai.djl.modality.nlp.qa.QAInput
import ai.djl.repository.zoo.Criteria
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths


class TranslatorTest {
    @Test
    fun importModel() {
        val input = QAInput("asdf", "asdf")

        //val translator = PicardTranslator()
        //val criteria: Criteria<QAInput, String> = Criteria.builder()
//            .setTypes(QAInput::class.java, String::class.java)
//            .optModelPath(Paths.get("/home/jake/School/NLP/text2sql4j/raw-files/picard_model.pt"))
//            .optTranslator(translator)
//            .optProgress(ProgressBar()).build()
//        val model: ZooModel<QAInput, String> = criteria.loadModel()
//        val predictor: Predictor<QAInput, String> = model.newPredictor(translator)
//        val output = predictor.predict(input)
//        println(output)
    }

    @Test
    fun importModel2() {
        val modelDir: Path = Paths.get("C:\\Users\\Cake\\Documents\\NLP\\text2sql4j\\raw-files\\picard_model.pt")
        val model = Model.newInstance("model name")
        model.load(modelDir)

        //val translator = PicardTranslator()
        //val predictor: Predictor<QAInput, String> = model.newPredictor(translator)
        //val output = predictor.predict(QAInput("asdf", "asdf"))
        //println(output)
    }

    @Test
    fun importModel3() {


        val criteria: Criteria<String, String> = Criteria.builder()
            .setTypes(String::class.java, String::class.java)
            .optModelPath(Paths.get("C:\\Users\\Cake\\Documents\\NLP\\text2sql4j\\raw-files\\picard_model.pt"))
            .optEngine("PyTorch")
            //.optArgument("tokenizer", Paths.get("C:\\Users\\Cake\\Documents\\NLP\\text2sql4j\\raw-files\\tokenizer.json"))
            .optOption("hasParameter", "false")
            .optTranslatorFactory(PicardTranslatorFactory())
            .build()

        //var input = "Text: For the game with 528 attendance, what was the result?"
        //var input = "<pad>test | select table.column from table</s>"
        var input = "Get concerts with short names. | concert_singer | stadium : stadium_id, location, name, capacity, highest, lowest, average | singer : singer_id, name, country, song_name, song_release_year, age, is_male | concert : concert_id, concert_name, theme, stadium_id, year | singer_in_concert : concert_id, singer_id"

        criteria.loadModel().use { model ->
            model.newPredictor().use { predictor ->
                for (i in 1..20) {
                    println(input)
                    val output = predictor.predict(input)
                    println(output)
                    input += " $output"
                }
            }
        }
    }

    @Test
    fun importModel4() {
        val maskToken = "<pad>"
        val criteria: Criteria<String, Classifications> = Criteria.builder()
            .setTypes(String::class.java, Classifications::class.java)
            .optModelPath(Paths.get("C:\\Users\\Cake\\Documents\\NLP\\text2sql4j\\raw-files\\picard_model.pt"))
            .optEngine("PyTorch")
            .optOption("hasParameter", "false")
            // Fill mask translator arguments
            .optArgument("maskToken", maskToken)
            .optArgument("topK", 10)
            .optTranslatorFactory(FillMaskTranslatorFactory())
            .build()

        criteria.loadModel().use { model ->
            model.newPredictor().use { predictor ->
                //var input = "Get concerts with short names. | concert_singer | stadium : stadium_id, location, name, capacity, highest, lowest, average | singer : singer_id, name, country, song_name, song_release_year, age, is_male | concert : concert_id, concert_name, theme, stadium_id, year | singer_in_concert : concert_id, singer_id | SELECT concert_name FROM concert where "
                var input = "Get away "

                (1..50).forEach {i ->
                    println(input + maskToken)
                    val output = predictor.predict(input + maskToken)
                    println(output)
                    input += output.best<Classifications.Classification>().className + " "
                }
            }
        }

    }
}