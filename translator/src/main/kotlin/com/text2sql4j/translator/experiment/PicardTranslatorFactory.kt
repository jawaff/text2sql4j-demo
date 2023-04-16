package com.text2sql4j.translator.experiment

import ai.djl.Model
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.djl.translate.TranslateException
import ai.djl.translate.Translator
import ai.djl.translate.TranslatorFactory
import ai.djl.util.Pair
import java.io.IOException
import java.lang.reflect.Type


/** A [TranslatorFactory] that creates a [PicardTranslator] instance. */
class PicardTranslatorFactory : TranslatorFactory {
    companion object {
        private val SUPPORTED_TYPES: MutableSet<Pair<Type, Type>> = mutableSetOf(
            Pair(String::class.java, String::class.java)
        )
    }

    /** {@inheritDoc}  */
    override fun getSupportedTypes(): Set<Pair<Type, Type>> {
        return SUPPORTED_TYPES
    }

    /** {@inheritDoc}  */
    @Throws(TranslateException::class)
    override fun <I, O> newInstance(
        input: Class<I>, output: Class<O>, model: Model, arguments: Map<String?, *>?
    ): Translator<I, O> {
        val modelPath = model.modelPath
        return try {
            val tokenizer = HuggingFaceTokenizer.builder(arguments)
                .optTokenizerPath(modelPath)
                .optManager(model.ndManager)
                .build()
            val translator = PicardTranslator(tokenizer)//.builder(tokenizer, arguments).build()
            if (input == String::class.java && output == String::class.java) {
                translator as Translator<I, O>
            }
            //else if (input == Array<QAInput>::class.java && output == Array<String>::class.java) {
                //return translator.toBatchTranslator()
            //    throw UnsupportedOperationException()
            //} else if (input == Input::class.java && output == Output::class.java) {
                //return QaServingTranslator(translator) as Translator<I, O>
            //    throw UnsupportedOperationException()
            //}
            else {
                throw IllegalArgumentException("Unsupported input/output types.")
            }
        } catch (e: IOException) {
            throw TranslateException("Failed to load tokenizer.", e)
        }
    }
}

