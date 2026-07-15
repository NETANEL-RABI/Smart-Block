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
    private var currentForegroundPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        appsRepo = BlockedAppsRepository.getInstance(applicationContext)
        scheduleManager = ScheduleManager.getInstance(applicationContext)
        startPeriodicRecheck()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return

        currentForegroundPackage = packageName

        serviceScope.launch {
            val isBlocked = appsRepo.isAppBlocked(packageName)
            if (!isBlocked) return@launch

            val currentlyInBlockWindow = scheduleManager.isCurrentlyBlocked(packageName)
            if (currentlyInBlockWindow && lastBlockedPackage != packageName) {
                lastBlockedPackage = packageName
                withContext(Dispatchers.Main) {
                    launchBlockOverlay(packageName)
                }
            } else if (!currentlyInBlockWindow) {
                lastBlockedPackage = null
            }
        }
    }

    /**
     * בודק כל 10 שניות אם הגישה הזמנית של האפליקציה שנפתחה כרגע פגה -
     * כדי לחסום מחדש גם אם המשתמש נשאר בתוך האפליקציה בלי לעבור מסך.
     */
    private fun startPeriodicRecheck() {
        serviceScope.launch {
            while (isActive) {
                delay(10_000)
                val pkg = currentForegroundPackage ?: continue
                if (pkg == this@AppBlockerService.packageName) continue

                val isBlockedApp = appsRepo.isAppBlocked(pkg)
                if (!isBlockedApp) continue

                val blockedNow = scheduleManager.isCurrentlyBlocked(pkg)
                if (blockedNow && lastBlockedPackage != pkg) {
                    lastBlockedPackage = pkg
                    withContext(Dispatchers.Main) {
                        launchBlockOverlay(pkg)
                    }
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
