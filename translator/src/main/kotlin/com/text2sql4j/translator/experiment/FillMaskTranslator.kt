package com.text2sql4j.translator.experiment

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.djl.modality.Classifications
import ai.djl.ndarray.NDList
import ai.djl.translate.*
import java.io.IOException
import java.util.*


/** The translator for Huggingface fill mask model.  */
class FillMaskTranslator internal constructor(
    private val tokenizer: HuggingFaceTokenizer,
    private val maskToken: String,
    private val topK: Int
) : Translator<String, Classifications> {
    private val maskTokenId: Long

    init {
        val encoding = tokenizer.encode(maskToken, true)
        maskTokenId = encoding.ids[0]
    }

    /** {@inheritDoc}  */
    @Throws(TranslateException::class)
    override fun processInput(ctx: TranslatorContext, input: String): NDList {
        val encoding = tokenizer.encode(input, false)
        val indices = encoding.ids
        val maskIndex = getMaskIndex(indices, maskToken, maskTokenId)
        ctx.setAttachment("maskIndex", maskIndex)
        return encoding.toNDList(ctx.ndManager, true)
    }

    /** {@inheritDoc}  */
    override fun processOutput(ctx: TranslatorContext, list: NDList): Classifications {
        val maskIndex = ctx.getAttachment("maskIndex") as Int
        return toClassifications(tokenizer, list, maskIndex, topK)
    }

    /** The builder for fill mask translator.  */
    class Builder internal constructor(private val tokenizer: HuggingFaceTokenizer) {
        private var maskedToken = "[MASK]"
        private var topK = 5

        /**
         * Sets the id of the mask [Translator].
         *
         * @param maskedToken the id of the mask
         * @return this builder
         */
        fun optMaskToken(maskedToken: String): Builder {
            this.maskedToken = maskedToken
            return this
        }

        /**
         * Set the topK number of classes to be displayed.
         *
         * @param topK the number of top classes to return
         * @return this builder
         */
        fun optTopK(topK: Int): Builder {
            this.topK = topK
            return this
        }

        /**
         * Configures the builder with the model arguments.
         *
         * @param arguments the model arguments
         */
        fun configure(arguments: Map<String, *>) {
            optMaskToken(ArgumentsUtil.stringValue(arguments, "maskToken", "[MASK]"))
            optTopK(ArgumentsUtil.intValue(arguments, "topK", 5))
        }

        /**
         * Builds the translator.
         *
         * @return the new translator
         * @throws IOException if I/O error occurs
         */
        @Throws(IOException::class)
        fun build(): FillMaskTranslator {
            return FillMaskTranslator(tokenizer, maskedToken, topK)
        }
    }

    companion object {
        @Throws(TranslateException::class)
        fun getMaskIndex(indices: LongArray, maskToken: String, maskTokenId: Long): Int {
            var maskIndex = -1
            for (i in indices.indices) {
                if (indices[i] == maskTokenId) {
                    if (maskIndex != -1) {
                        throw TranslateException("Only one mask supported.")
                    }
                    maskIndex = i
                }
            }
            if (maskIndex == -1) {
                throw TranslateException("Mask token $maskToken not found.")
            }
            return maskIndex
        }

        fun toClassifications(
            tokenizer: HuggingFaceTokenizer, output: NDList, maskIndex: Int, topK: Int
        ): Classifications {
            val prob = output[0][maskIndex.toLong()].softmax(0)
            val array = prob.argSort(0, false)
            val classIds = LongArray(topK)
            val probabilities: MutableList<Double> = ArrayList(topK)
            for (i in 0 until topK) {
                classIds[i] = array.getLong(i.toLong())
                probabilities.add(prob.getFloat(classIds[i]).toDouble())
            }
            val classes =
                tokenizer.decode(classIds).trim { it <= ' ' }.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            return Classifications(listOf(*classes), probabilities)
        }

        /**
         * Creates a builder to build a `FillMaskTranslator`.
         *
         * @param tokenizer the tokenizer
         * @return a new builder
         */
        fun builder(tokenizer: HuggingFaceTokenizer): Builder {
            return Builder(tokenizer)
        }

        /**
         * Creates a builder to build a `FillMaskTranslator`.
         *
         * @param tokenizer the tokenizer
         * @param arguments the models' arguments
         * @return a new builder
         */
        fun builder(tokenizer: HuggingFaceTokenizer, arguments: Map<String, *>): Builder {
            val builder = builder(tokenizer)
            builder.configure(arguments)
            return builder
        }
    }
}

