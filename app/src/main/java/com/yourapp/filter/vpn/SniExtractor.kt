package com.yourapp.filter.vpn

import java.nio.ByteBuffer

object SniExtractor {

    fun extractSni(tcpPayload: ByteArray): String? {
        try {
            if (tcpPayload.isEmpty()) return null
            val buffer = ByteBuffer.wrap(tcpPayload)

            if (buffer.remaining() < 5) return null
            val contentType = buffer.get().toInt() and 0xFF
            if (contentType != 0x16) return null

            buffer.short
            val recordLength = buffer.short.toInt() and 0xFFFF
            if (buffer.remaining() < recordLength) return null

            val handshakeType = buffer.get().toInt() and 0xFF
            if (handshakeType != 0x01) return null

            buffer.get(); buffer.get(); buffer.get()
            buffer.short
            val random = ByteArray(32); buffer.get(random)

            val sessionIdLen = buffer.get().toInt() and 0xFF
            repeat(sessionIdLen) { buffer.get() }

            val cipherSuitesLen = buffer.short.toInt() and 0xFFFF
            repeat(cipherSuitesLen) { buffer.get() }

            val compressionLen = buffer.get().toInt() and 0xFF
            repeat(compressionLen) { buffer.get() }

            if (buffer.remaining() < 2) return null
            val extensionsLen = buffer.short.toInt() and 0xFFFF
            val extensionsEnd = buffer.position() + extensionsLen

            while (buffer.position() < extensionsEnd && buffer.remaining() >= 4) {
                val extType = buffer.short.toInt() and 0xFFFF
                val extLen = buffer.short.toInt() and 0xFFFF

                if (extType == 0x00) {
                    val sniListLen = buffer.short.toInt() and 0xFFFF
                    val nameType = buffer.get().toInt() and 0xFF
                    if (nameType == 0x00) {
                        val nameLen = buffer.short.toInt() and 0xFFFF
                        val nameBytes = ByteArray(nameLen)
                        buffer.get(nameBytes)
                        return String(nameBytes, Charsets.US_ASCII)
                    }
                } else {
                    repeat(extLen) { buffer.get() }
                }
            }
            return null
        } catch (e: Exception) {
            return null
        }
    }
}
