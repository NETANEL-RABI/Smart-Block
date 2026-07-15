package com.yourapp.filter.scheduler

import android.content.Context
import com.yourapp.filter.data.AppDatabase
import com.yourapp.filter.ntp.NtpTimeSync
import java.util.Calendar

class ScheduleManager private constructor(context: Context) {

    private val dao = AppDatabase.getInstance(context).scheduleRuleDao()
    private val ntpTimeSync = NtpTimeSync.getInstance()

    companion object {
        @Volatile private var INSTANCE: ScheduleManager? = null
        fun getInstance(context: Context): ScheduleManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ScheduleManager(context.applicationContext).also { INSTANCE = it }
            }
    }

    suspend fun isCurrentlyBlocked(targetPackageOrDomain: String): Boolean {
        val rules = dao.getRulesFor(targetPackageOrDomain)
        if (rules.isEmpty()) return true // אין חוק זמן = חסום תמיד כברירת מחדל

        // שימוש בזמן מסונכרן מול NTP כדי למנוע עקיפה ע"י שינוי שעון המכשיר
        val now = ntpTimeSync.getReliableTime()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }

        val dayBit = dayOfWeekToBit(calendar.get(Calendar.DAY_OF_WEEK))
        val minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        return rules.any { rule ->
            rule.isEnabled &&
            (rule.daysOfWeekMask and dayBit) != 0 &&
            minuteOfDay in rule.startMinuteOfDay..rule.endMinuteOfDay
        }
    }

    private fun dayOfWeekToBit(calendarDay: Int): Int {
        // Calendar.SUNDAY=1 ... Calendar.SATURDAY=7  ->  ביט 1..64
        return 1 shl (calendarDay - 1)
    }

    suspend fun addRule(rule: ScheduleRule) = dao.insert(rule)
    suspend fun deleteRule(rule: ScheduleRule) = dao.delete(rule)
    suspend fun getAllRules() = dao.getAll()
}
