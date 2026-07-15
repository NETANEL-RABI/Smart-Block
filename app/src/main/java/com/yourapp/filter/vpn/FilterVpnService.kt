package com.yourapp.filter.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.yourapp.filter.R
import com.yourapp.filter.data.BlockedDomainsRepository
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class FilterVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var domainsRepo: BlockedDomainsRepository
    private val activeConnections = mutableMapOf<String, TcpForwarder>()

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "filter_vpn_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.yourapp.filter.STOP_VPN"
    }

    override fun onCreate() {
        super.onCreate()
        domainsRepo = BlockedDomainsRepository.getInstance(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        val builder = Builder()
            .setSession("Filter VPN")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("8.8.8.8")
            .setMtu(1500)
            .setBlocking(false)
            .addDisallowedApplication(packageName)

        vpnInterface = builder.establish()
        serviceScope.launch { runPacketLoop() }
    }

    private suspend fun runPacketLoop() = withContext(Dispatchers.IO) {
        val fd = vpnInterface?.fileDescriptor ?: return@withContext
        val input = FileInputStream(fd)
        val output = FileOutputStream(fd)
        val buffer = ByteArray(32767)

        while (isActive && vpnInterface != null) {
            try {
                val length = input.read(buffer)
                if (length <= 0) {
                    delay(5)
                    continue
                }
                handlePacket(buffer.copyOf(length), output)
            } catch (e: IOException) {
                break
            }
        }
    }

    private fun handlePacket(packet: ByteArray, output: FileOutputStream) {
        val ipPacket = IpPacketParser.parse(packet) ?: return
        if (ipPacket.protocol != IpPacketParser.PROTOCOL_TCP) return

        val tcpPacket = TcpPacketParser.parse(ipPacket) ?: return

        if (tcpPacket.payload.isNotEmpty() && tcpPacket.destPort == 443) {
            val sni = SniExtractor.extractSni(tcpPacket.payload)
            if (sni != null) {
                serviceScope.launch {
                    val isBlocked = domainsRepo.isDomainBlocked(sni)
                    if (!isBlocked) {
                        forwardConnection(ipPacket, tcpPacket, output)
                    }
                }
                return
            }
        }

        val connectionKey = "${ipPacket.destAddress}:${tcpPacket.destPort}:${tcpPacket.sourcePort}"
        activeConnections[connectionKey]?.forwardFromTun(tcpPacket.payload)
    }

    private fun forwardConnection(
        ipPacket: IpPacketParser.IpPacket,
        tcpPacket: TcpPacketParser.TcpPacket,
        tunOutput: FileOutputStream
    ) {
        val connectionKey = "${ipPacket.destAddress}:${tcpPacket.destPort}:${tcpPacket.sourcePort}"
        val forwarder = TcpForwarder(
            destAddress = ipPacket.destAddress,
            destPort = tcpPacket.destPort,
            protectSocket = { socket -> protect(socket) },
            tunOutput = tunOutput
        )
        activeConnections[connectionKey] = forwarder
        forwarder.start(tcpPacket.payload)
    }

    private fun stopVpn() {
        serviceScope.coroutineContext.cancelChildren()
        activeConnections.values.forEach { it.close() }
        activeConnections.clear()
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, "סינון פעיל", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("סינון פעיל")
            .setContentText("האפליקציה מגנה על המכשיר כרגע")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
}
