package com.yourapp.filter.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.yourapp.filter.data.BlockedAppsRepository
import com.yourapp.filter.scheduler.ScheduleManager
import com.yourapp.filter.ui.BlockOverlayActivity
import kotlinx.coroutines.*

class AppBlockerService : AccessibilityService() {

    private lateinit var appsRepo: BlockedAppsRepository
    private lateinit var scheduleManager: ScheduleManager
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var lastBlockedPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        appsRepo = BlockedAppsRepository.getInstance(applicationContext)
        scheduleManager = ScheduleManager.getInstance(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return

        serviceScope.launch {
            val isBlocked = appsRepo.isAppBlocked(packageName)
            if (!isBlocked) return@launch

            val currentlyInBlockWindow = scheduleManager.isCurrentlyBlocked(packageName)
            if (currentlyInBlockWindow && lastBlockedPackage != packageName) {
                lastBlockedPackage = packageName
                withContext(Dispatchers.Main) {
                    launchBlockOverlay(packageName)
                }
            }
        }
    }

    private fun launchBlockOverlay(blockedPackage: String) {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(homeIntent)

        val overlayIntent = Intent(this, BlockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("blocked_package", blockedPackage)
        }
        startActivity(overlayIntent)
    }

    override fun onInterrupt() {}
}
