package com.yourapp.filter.scheduler

import android.content.Context
import com.yourapp.filter.data.AppDatabase
import com.yourapp.filter.data.TemporaryUnlockRepository
import com.yourapp.filter.ntp.NtpTimeSync
import java.util.Calendar

class ScheduleManager private constructor(context: Context) {

    private val dao = AppDatabase.getInstance(context).scheduleRuleDao()
    private val ntpTimeSync = NtpTimeSync.getInstance()
    private val temporaryUnlockRepository = TemporaryUnlockRepository.getInstance(context)

    companion object {
        @Volatile private var INSTANCE: ScheduleManager? = null
        fun getInstance(context: Context): ScheduleManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ScheduleManager(context.applicationContext).also { INSTANCE = it }
            }
    }

    suspend fun isCurrentlyBlocked(targetPackageOrDomain: String): Boolean {
        val now = ntpTimeSync.getReliableTime()

        // אם יש גישה זמנית פעילה ועדיין לא פגה - לא חוסמים כרגע
        val unlockExpiry = temporaryUnlockRepository.getUnlockExpiry(targetPackageOrDomain)
        if (unlockExpiry > now) return false

        val rules = dao.getRulesFor(targetPackageOrDomain)
        if (rules.isEmpty()) return true // אין חוק זמן = חסום תמיד כברירת מחדל

        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val dayBit = dayOfWeekToBit(calendar.get(Calendar.DAY_OF_WEEK))
        val minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        return rules.any { rule ->
            if (!rule.isEnabled || (rule.daysOfWeekMask and dayBit) == 0) return@any false

            val crossesMidnight = rule.endMinuteOfDay >= 1440
            if (crossesMidnight) {
                val normalizedEnd = rule.endMinuteOfDay % 1440
                minuteOfDay >= rule.startMinuteOfDay || minuteOfDay <= normalizedEnd
            } else {
                minuteOfDay in rule.startMinuteOfDay..rule.endMinuteOfDay
            }
        }
    }

    /** מעניק גישה זמנית לדקות ספציפיות, גם אם כרגע יש חוק חסימה פעיל. */
    suspend fun grantTemporaryAccess(target: String, minutes: Int) {
        val now = ntpTimeSync.getReliableTime()
        temporaryUnlockRepository.grantUnlockUntil(target, now + minutes * 60_000L)
    }

    private fun dayOfWeekToBit(calendarDay: Int): Int {
        return 1 shl (calendarDay - 1)
    }

    suspend fun addRule(rule: ScheduleRule) = dao.insert(rule)
    suspend fun deleteRule(rule: ScheduleRule) = dao.delete(rule)
    suspend fun getAllRules() = dao.getAll()
}
