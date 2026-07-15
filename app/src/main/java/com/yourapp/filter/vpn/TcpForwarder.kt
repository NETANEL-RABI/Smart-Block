package com.yourapp.filter.vpn

import kotlinx.coroutines.*
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Socket

class TcpForwarder(
    private val destAddress: String,
    private val destPort: Int,
    private val protectSocket: (Socket) -> Boolean,
    private val tunOutput: FileOutputStream
) {
    private var socket: Socket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start(initialPayload: ByteArray) {
        scope.launch {
            try {
                val s = Socket()
                protectSocket(s)
                s.connect(InetSocketAddress(destAddress, destPort), 5000)
                socket = s

                if (initialPayload.isNotEmpty()) {
                    s.getOutputStream().write(initialPayload)
                }

                val buffer = ByteArray(32767)
                val input = s.getInputStream()
                while (isActive) {
                    val len = input.read(buffer)
                    if (len <= 0) break
                    synchronized(tunOutput) {
                        tunOutput.write(buffer, 0, len)
                    }
                }
            } catch (e: Exception) {
                // חיבור נכשל/נסגר
            } finally {
                close()
            }
        }
    }

    fun forwardFromTun(payload: ByteArray) {
        if (payload.isEmpty()) return
        scope.launch {
            try {
                socket?.getOutputStream()?.write(payload)
            } catch (e: Exception) {
                close()
            }
        }
    }

    fun close() {
        scope.cancel()
        try { socket?.close() } catch (e: Exception) {}
    }
}
