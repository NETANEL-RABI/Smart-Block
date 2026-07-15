package com.yourapp.filter.ntp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ntp.NTPUDPClient
import java.net.InetAddress

/**
 * מסנכרן זמן מול שרת NTP חיצוני כדי למנוע עקיפת חסימות
 * ע"י שינוי ידני של שעון המכשיר.
 */
class NtpTimeSync private constructor() {

    private var offsetMillis: Long = 0L // ההפרש בין הזמן האמיתי לזמן המכשיר
    private var lastSyncTime: Long = 0L

    companion object {
        private const val NTP_SERVER = "time.google.com"
        private const val SYNC_INTERVAL_MS = 15 * 60 * 1000L // סנכרון מחדש כל 15 דקות

        @Volatile private var INSTANCE: NtpTimeSync? = null
        fun getInstance(): NtpTimeSync =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: NtpTimeSync().also { INSTANCE = it }
            }
    }

    suspend fun getReliableTime(): Long = withContext(Dispatchers.IO) {
        val deviceNow = System.currentTimeMillis()
        if (deviceNow - lastSyncTime > SYNC_INTERVAL_MS) {
            trySync()
        }
        deviceNow + offsetMillis
    }

    private fun trySync() {
        try {
            val client = NTPUDPClient()
            client.defaultTimeout = 5000
            val address = InetAddress.getByName(NTP_SERVER)
            val info = client.getTime(address)
            info.computeDetails()
            offsetMillis = info.offset ?: 0L
            lastSyncTime = System.currentTimeMillis()
            client.close()
        } catch (e: Exception) {
            // אין רשת/שרת לא זמין - ממשיכים עם ה-offset הקודם שכבר חושב
        }
    }
}
