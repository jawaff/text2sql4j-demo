package com.text2sql4j.translator

import ai.djl.huggingface.tokenizers.Encoding
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.djl.modality.nlp.qa.QAInput
import ai.djl.ndarray.NDArray
import ai.djl.ndarray.NDList
import ai.djl.ndarray.index.NDIndex
import ai.djl.repository.zoo.ModelZoo
import ai.djl.training.ParameterStore
import ai.djl.translate.Translator
import ai.djl.translate.TranslatorContext
import java.nio.file.Paths
import java.util.*


class PicardTranslator(private val tokenizer: HuggingFaceTokenizer) : Translator<String, String> {
    private var tokens: MutableList<String>? = null

    override fun prepare(ctx: TranslatorContext) {
        //tokenizer = HuggingFaceTokenizer.newInstance(Paths.get("/home/jake/School/NLP/text2sql4j/raw-files/tokenizer.json"))
        //tokenizer = HuggingFaceTokenizer.newInstance(Paths.get("C:\\Users\\Cake\\Documents\\NLP\\text2sql4j\\raw-files\\tokenizer.json"))
        //tokenizer = BertTokenizer()
    }

    override fun processInput(ctx: TranslatorContext, input: String): NDList {
        val encoding = tokenizer.encode(input)
        ctx.setAttachment("encoding", encoding)
        val includeTokenTypes = true
        return encoding.toNDList(ctx.ndManager, includeTokenTypes)
    }

    override fun processOutput(ctx: TranslatorContext, list: NDList): String {
        // list[0] represents the final output of the model with shape [inputSize, vocabSize].
        println(list[1].shape)
        // Intermediate layer outputs have shape [batchSize (32 by default?), inputSize, ?]
        //val outputIds = LongArray(list[0].size(0).toInt())
        val outputIds =  Array(list[0].size(0).toInt()) { FloatArray(32102) }
        (0 until list[0].size(0)).map { i ->
            println("")
            print("[")
            // This softmax/argmax solution is incorrect. It produces the same values each time!
            //outputIds[i.toInt()] = list[0][i].softmax(0).argMax().getLong()
            //outputIds[i.toInt()] = list[0][i].toFloatArray()
            val curProbs = list[0][i]
            val sortedProbIds = list[0][i].softmax(0).argSort(0, false)
            for (j in 300 until 310) {
                val curIndex = sortedProbIds.getLong(j.toLong())
                val classId = LongArray(1)
                classId[0] = curIndex
                val nextToken = tokenizer.decode(classId).trim { it <= ' ' }
                print("$curIndex $nextToken ${curProbs.getFloat(curIndex).toDouble()}, ")
            }
            print("]")
        }
        return ""
        //println(tokenizer.decode(outputIds))
        //println(tokenizer.batchDecode(outputIds))
        //val zxcv = list[0][0]
        //zxcv.toLongArray()

        // TODO Figure out how picard does its logits processing!

        //val outputLogits = list[0]
        //val nextTokens = mutableListOf<String>()
        //for (i in 0 until (outputLogits.size(0)-1)) {
//            val possibleClassIds = nextToken(outputLogits[5])
//            val classId = LongArray(1)
//            classId[0] = possibleClassIds[0]
//            val nextToken = tokenizer.decode(classId).trim { it <= ' ' }
//            nextTokens.add(nextToken)
        //}

        //ctx.model.block.forward(ctx.model.block.parameters, )
        //return nextTokens.joinToString(separator = " ")

        //val result = ctx.block.forward(ParameterStore(), list, false)
        //println(result)
        // PyTorch InferenceMode tensor is read only, must clone it
        // PyTorch InferenceMode tensor is read only, must clone it
        //val startLogits = list[0].duplicate()
        //val endLogits = list[1].duplicate()
        // exclude <CLS>, TODO: exclude impossible ids properly and handle max answer length
        // exclude <CLS>, TODO: exclude impossible ids properly and handle max answer length
        //startLogits[NDIndex(0)] = -100000
        //endLogits[NDIndex(0)] = -100000
        //val asdf = startLogits.argMin().getLong().toInt()
        //val qwer = endLogits.argMin().getLong().toInt()
        //println(asdf)
        //println(qwer)
        //var startIdx = startLogits.argMax().getLong().toInt()
        //var endIdx = endLogits.argMax().getLong().toInt()
        //if (startIdx > endIdx) {
            //val tmp = startIdx
            //startIdx = endIdx
            //    endIdx = tmp
            //}
        //val encoding: Encoding = ctx.getAttachment("encoding") as Encoding
        //val indices: LongArray = encoding.ids
        //val len = endIdx - startIdx + 1
        //val ids = LongArray(len)
        //System.arraycopy(indices, startIdx, ids, 0, len)
        //return tokenizer.decode(ids).trim { it <= ' ' }
    }

    private fun nextToken(tokenProbabilities: NDArray): LongArray {
        val prob = tokenProbabilities.softmax(0)
        val array = prob.argSort(0, false)
        val topK = 3
        val classIds = LongArray(topK)
        val probabilities: MutableList<Double> = ArrayList(topK)
        for (i in 0 until topK) {
            classIds[i] = array.getLong(i.toLong())
            probabilities.add(prob.getFloat(classIds[i]).toDouble())
        }
        return classIds
    }
}