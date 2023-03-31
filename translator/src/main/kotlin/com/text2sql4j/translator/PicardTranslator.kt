package com.text2sql4j.translator

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.djl.modality.nlp.DefaultVocabulary
import ai.djl.modality.nlp.Vocabulary
import ai.djl.modality.nlp.bert.BertTokenizer
import ai.djl.modality.nlp.qa.QAInput
import ai.djl.ndarray.NDList
import ai.djl.translate.Batchifier
import ai.djl.translate.Translator
import ai.djl.translate.TranslatorContext
import java.nio.file.Paths
import java.util.*


class PicardTranslator : Translator<QAInput, String> {
    private var tokens: MutableList<String>? = null
    private var vocabulary: Vocabulary? = null
    private var tokenizer: HuggingFaceTokenizer? = null

    override fun prepare(ctx: TranslatorContext?) {
        tokenizer = HuggingFaceTokenizer.newInstance(Paths.get("/home/jake/School/NLP/text2sql4j/raw-files/tokenizer.json"))

        val path = Paths.get("/YOUR PATH/vocab.txt")
        vocabulary = DefaultVocabulary.builder()
            .optMinFrequency(1)
            .addFromTextFile(path)
            .optUnknownToken("[UNK]")
            .build()
        //tokenizer = BertTokenizer()
    }

    override fun processInput(ctx: TranslatorContext, input: QAInput): NDList {
        val token = tokenizer!!.encode(
            input.question.lowercase(Locale.getDefault()),
            input.paragraph.lowercase(Locale.getDefault())
        )
        // get the encoded tokens used in precessOutput
        tokens = token.tokens.toMutableList()
        val manager = ctx.ndManager
        // map the tokens(String) to indices(long)
        //val indices = tokens!!.map { token: String ->
        //    vocabulary!!.getIndex(
         //       token
        //    )
        //}.toLongArray()

        // TODO Is token.ids the same as the above indices?
        val indicesArray = manager.create(token.ids)
        val attentionMaskArray = manager.create(token.attentionMask)
        val tokenTypeArray = manager.create(token.typeIds)
        // The order matters
        return NDList(
            indicesArray, attentionMaskArray,
            tokenTypeArray
        )
    }

    override fun processOutput(ctx: TranslatorContext, list: NDList): String {
        val startLogits = list[0]
        val endLogits = list[1]
        val startIdx = startLogits.argMax().getLong().toInt()
        val endIdx = endLogits.argMax().getLong().toInt()
        // Unsupported in HuggingfaceTokenizer
        //return tokenizer!!.tokenToString(tokens!!.subList(startIdx, endIdx + 1))
        // TODO Is this equivalent?
        return tokenizer!!.buildSentence(tokens!!.subList(startIdx, endIdx + 1))
    }
}