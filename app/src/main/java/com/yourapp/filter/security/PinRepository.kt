package com.yourapp.filter.security

import android.content.Context
import java.security.MessageDigest

class PinRepository private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("pin_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"

        @Volatile private var INSTANCE: PinRepository? = null
        fun getInstance(context: Context): PinRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: PinRepository(context.applicationContext).also { INSTANCE = it }
            }
    }

    fun hasPin(): Boolean = prefs.contains(KEY_PIN_HASH)

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN_HASH, hash(pin)).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return stored == hash(pin)
    }

    private fun hash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
