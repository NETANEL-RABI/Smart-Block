package com.yourapp.filter.data

import android.content.Context

class BlockedAppsRepository private constructor(context: Context) {

    private val dao = AppDatabase.getInstance(context).blockedAppDao()

    companion object {
        @Volatile private var INSTANCE: BlockedAppsRepository? = null
        fun getInstance(context: Context): BlockedAppsRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BlockedAppsRepository(context.applicationContext).also { INSTANCE = it }
            }
    }

    suspend fun isAppBlocked(packageName: String): Boolean = dao.isBlocked(packageName)
    suspend fun blockApp(packageName: String) = dao.insert(BlockedApp(packageName))
    suspend fun unblockApp(packageName: String) = dao.delete(BlockedApp(packageName))
    suspend fun getAllBlocked() = dao.getAll()
}
