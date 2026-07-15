package com.yourapp.filter.data

import android.content.Context
import android.content.Intent

data class InstalledAppInfo(
    val packageName: String,
    val appName: String
)

object InstalledAppsProvider {

    fun getInstalledApps(context: Context): List<InstalledAppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

        return resolveInfos
            .map { InstalledAppInfo(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }
}
