package com.yourapp.filter.vpn

object IpPacketParser {
    const val PROTOCOL_TCP = 6
    const val PROTOCOL_UDP = 17

    data class IpPacket(
        val version: Int,
        val headerLength: Int,
        val protocol: Int,
        val sourceAddress: String,
        val destAddress: String,
        val payload: ByteArray
    )

    fun parse(packet: ByteArray): IpPacket? {
        if (packet.isEmpty()) return null
        val versionAndIhl = packet[0].toInt() and 0xFF
        val version = versionAndIhl shr 4
        if (version != 4) return null

        val ihl = versionAndIhl and 0x0F
        val headerLength = ihl * 4
        if (packet.size < headerLength) return null

        val protocol = packet[9].toInt() and 0xFF
        val sourceAddress = ipToString(packet, 12)
        val destAddress = ipToString(packet, 16)
        val payload = packet.copyOfRange(headerLength, packet.size)

        return IpPacket(version, headerLength, protocol, sourceAddress, destAddress, payload)
    }

    private fun ipToString(packet: ByteArray, offset: Int): String {
        return "${packet[offset].toInt() and 0xFF}." +
                "${packet[offset + 1].toInt() and 0xFF}." +
                "${packet[offset + 2].toInt() and 0xFF}." +
                "${packet[offset + 3].toInt() and 0xFF}"
    }
}
