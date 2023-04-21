package com.text2sql4j.api.models.codecs

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.jackson.DatabindCodec
import java.nio.charset.StandardCharsets

abstract class JacksonCodec<T>(private val clazz: Class<T>): MessageCodec<T, T> {
    override fun encodeToWire(buffer: Buffer, message: T) {
        val json = DatabindCodec.mapper().writeValueAsString(message)
        buffer.appendInt(json.toByteArray(StandardCharsets.UTF_8).size)
        buffer.appendString(json)
    }

    override fun decodeFromWire(pos: Int, buffer: Buffer): T {
        var curPos = pos

        val length = buffer.getInt(curPos)

        // Get JSON string by its length
        // Jump 4 because getInt() == 4 bytes
        val json = buffer.getString(4.let { curPos += it; curPos }, length.let { curPos += it; curPos })
        return DatabindCodec.mapper().readValue(json, clazz)
    }

    override fun transform(message: T): T {
        // Transformations only apply to a clustered event bus.
        return message
    }

    override fun name(): String {
        // Each codec needs a unique name.
        return this.javaClass.simpleName
    }

    override fun systemCodecID(): Byte {
        // Always -1 for some reason
        return -1
    }


}