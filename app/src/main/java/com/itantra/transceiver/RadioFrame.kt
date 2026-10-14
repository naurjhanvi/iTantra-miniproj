package com.itantra.transceiver

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.CRC32

/** Compact binary frame used by every transport. Fragmentation belongs above this layer. */
data class RadioFrame(
    val messageId: UUID = UUID.randomUUID(), val languageId: String, val text: String,
    val timestamp: Long = System.currentTimeMillis(), val alert: Boolean = false, val toneTag: Byte = 0
) {
    fun encode(): ByteArray {
        val language = languageId.toByteArray(StandardCharsets.UTF_8)
        val body = text.toByteArray(StandardCharsets.UTF_8)
        require(languageId.isNotBlank() && language.size <= MAX_LANGUAGE_BYTES) { "Invalid language ID" }
        require(body.size <= MAX_TEXT_BYTES) { "Text payload exceeds radio limit" }
        val buffer = ByteBuffer.allocate(1 + 16 + 1 + language.size + 1 + 1 + 8 + 2 + body.size + 4)
            .order(ByteOrder.BIG_ENDIAN)
        buffer.put(PROTOCOL_VERSION); buffer.putLong(messageId.mostSignificantBits); buffer.putLong(messageId.leastSignificantBits)
        buffer.put(language.size.toByte()); buffer.put(language); buffer.put(if (alert) 1.toByte() else 0.toByte()); buffer.put(toneTag)
        buffer.putLong(timestamp); buffer.putShort(body.size.toShort()); buffer.put(body)
        val crc = CRC32().apply { update(buffer.array(), 0, buffer.position()) }.value
        buffer.putInt(crc.toInt()); return buffer.array()
    }

    companion object {
        const val PROTOCOL_VERSION: Byte = 1
        const val MAX_LANGUAGE_BYTES = 31
        const val MAX_TEXT_BYTES = 900
        private const val FIXED_BYTES_BEFORE_LANGUAGE = 18
        private const val FIXED_PAYLOAD_BYTES_AFTER_LANGUAGE = 12
        private const val CHECKSUM_BYTES = 4

        /** Decodes a complete frame and rejects corrupt, truncated, or unsupported payloads. */
        fun decode(encoded: ByteArray): RadioFrame {
            require(encoded.size >= FIXED_BYTES_BEFORE_LANGUAGE + 1 + FIXED_PAYLOAD_BYTES_AFTER_LANGUAGE + CHECKSUM_BYTES) {
                "Frame is truncated"
            }
            require(encoded.size <= FIXED_BYTES_BEFORE_LANGUAGE + MAX_LANGUAGE_BYTES +
                FIXED_PAYLOAD_BYTES_AFTER_LANGUAGE + MAX_TEXT_BYTES + CHECKSUM_BYTES) {
                "Frame exceeds maximum size"
            }

            val payloadLength = encoded.size - CHECKSUM_BYTES
            val expectedCrc = ByteBuffer.wrap(encoded, payloadLength, CHECKSUM_BYTES)
                .order(ByteOrder.BIG_ENDIAN)
                .int.toLong() and 0xFFFF_FFFFL
            val actualCrc = CRC32().apply { update(encoded, 0, payloadLength) }.value
            require(actualCrc == expectedCrc) { "Frame checksum mismatch" }

            val buffer = ByteBuffer.wrap(encoded, 0, payloadLength).order(ByteOrder.BIG_ENDIAN)
            val version = buffer.get()
            require(version == PROTOCOL_VERSION) { "Unsupported protocol version: ${version.toInt() and 0xFF}" }
            val messageId = UUID(buffer.getLong(), buffer.getLong())

            val languageLength = buffer.get().toInt() and 0xFF
            require(languageLength in 1..MAX_LANGUAGE_BYTES) { "Invalid language ID length" }
            require(buffer.remaining() >= languageLength + FIXED_PAYLOAD_BYTES_AFTER_LANGUAGE) { "Frame is truncated" }
            val language = ByteArray(languageLength).also(buffer::get).decodeUtf8()
            require(language.isNotBlank()) { "Language ID cannot be blank" }

            val alertFlag = buffer.get().toInt() and 0xFF
            require(alertFlag == 0 || alertFlag == 1) { "Invalid alert flag" }
            val toneTag = buffer.get()
            val timestamp = buffer.getLong()
            val textLength = buffer.short.toInt() and 0xFFFF
            require(textLength <= MAX_TEXT_BYTES) { "Text payload exceeds radio limit" }
            require(buffer.remaining() == textLength) { "Frame length does not match text payload" }
            val text = ByteArray(textLength).also(buffer::get).decodeUtf8()

            return RadioFrame(messageId, language, text, timestamp, alertFlag == 1, toneTag)
        }

        private fun ByteArray.decodeUtf8(): String =
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(this))
                .toString()
    }
}
