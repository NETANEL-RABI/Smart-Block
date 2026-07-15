package com.yourapp.filter.vpn

object TcpPacketParser {

    const val FLAG_SYN = 0x02
    const val FLAG_ACK = 0x10
    const val FLAG_FIN = 0x01
    const val FLAG_RST = 0x04

    data class TcpPacket(
        val sourcePort: Int,
        val destPort: Int,
        val flags: Int,
        val payload: ByteArray
    )

    fun parse(ipPacket: IpPacketParser.IpPacket): TcpPacket? {
        val data = ipPacket.payload
        if (data.size < 20) return null

        val sourcePort = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
        val destPort = ((data[2].toInt() and 0xFF) shl 8) or (data[3].toInt() and 0xFF)

        val dataOffsetByte = data[12].toInt() and 0xFF
        val headerLength = (dataOffsetByte shr 4) * 4
        val flags = data[13].toInt() and 0xFF

        if (data.size < headerLength) return null
        val payload = data.copyOfRange(headerLength, data.size)

        return TcpPacket(sourcePort, destPort, flags, payload)
    }
}
