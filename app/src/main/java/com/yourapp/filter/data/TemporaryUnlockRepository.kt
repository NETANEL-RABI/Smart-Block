package com.yourapp.filter.data

import android.content.Context

/**
 * שומר עבור כל אפליקציה/דומיין חסום את הזמן (timestamp) שעד אליו יש גישה זמנית מאושרת.
 * אחרי שהזמן הזה עובר, החסימה חוזרת אוטומטית לפי לוח הזמנים הרגיל.
 */
class TemporaryUnlockRepository private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("temporary_unlocks", Context.MODE_PRIVATE)

    companion object {
        @Volatile private var INSTANCE: TemporaryUnlockRepository? = null
        fun getInstance(context: Context): TemporaryUnlockRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: TemporaryUnlockRepository(context.applicationContext).also { INSTANCE = it }
            }
    }

    fun grantUnlockUntil(target: String, expiryTimeMillis: Long) {
        prefs.edit().putLong(target, expiryTimeMillis).apply()
    }

    fun getUnlockExpiry(target: String): Long {
        return prefs.getLong(target, 0L)
    }

    fun clearUnlock(target: String) {
        prefs.edit().remove(target).apply()
    }
}
