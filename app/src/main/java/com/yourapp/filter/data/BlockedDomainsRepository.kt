package com.yourapp.filter.data

import android.content.Context

class BlockedDomainsRepository private constructor(context: Context) {

    private val dao = AppDatabase.getInstance(context).blockedDomainDao()

    companion object {
        @Volatile private var INSTANCE: BlockedDomainsRepository? = null
        fun getInstance(context: Context): BlockedDomainsRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BlockedDomainsRepository(context.applicationContext).also { INSTANCE = it }
            }
    }

    suspend fun isDomainBlocked(hostname: String): Boolean {
        // בודקים גם את הדומיין המדויק וגם דומיינים הוריים (למשל m.youtube.com -> youtube.com)
        val parts = hostname.split(".")
        for (i in parts.indices) {
            val candidate = parts.subList(i, parts.size).joinToString(".")
            if (dao.isBlocked(candidate)) return true
        }
        return false
    }

    suspend fun blockDomain(domain: String) = dao.insert(BlockedDomain(domain))
    suspend fun unblockDomain(domain: String) = dao.delete(BlockedDomain(domain))
    suspend fun getAllBlocked() = dao.getAll()
}
