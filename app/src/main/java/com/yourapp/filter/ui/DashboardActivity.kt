package com.yourapp.filter.ui

import android.content.ComponentName
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.app.admin.DevicePolicyManager
import com.yourapp.filter.R
import com.yourapp.filter.admin.FilterDeviceAdminReceiver
import com.yourapp.filter.vpn.FilterVpnService

class DashboardActivity : AppCompatActivity() {

    private var pendingAction: (() -> Unit)? = null

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) startVpnService()
        }

    private val pinVerifyLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                pendingAction?.invoke()
            }
            pendingAction = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        findViewById<android.widget.Button>(R.id.btnStartFilter).setOnClickListener {
            requestVpnPermission()
        }
        // עצירת הסינון דורשת PIN - אחרת כל אחד יכול לכבות את ההגנה
        findViewById<android.widget.Button>(R.id.btnStopFilter).setOnClickListener {
            requirePinThen { stopVpnService() }
        }
        findViewById<android.widget.Button>(R.id.btnEnableAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<android.widget.Button>(R.id.btnEnableDeviceAdmin).setOnClickListener {
            requestDeviceAdmin()
        }
        // כניסה ללוח הזמנים (עריכת/מחיקת חוקים) דורשת PIN
        findViewById<android.widget.Button>(R.id.btnSchedule).setOnClickListener {
            requirePinThen { startActivity(Intent(this, ScheduleActivity::class.java)) }
        }
    }

    private fun requirePinThen(action: () -> Unit) {
        pendingAction = action
        val intent = Intent(this, PinLockActivity::class.java).apply {
            putExtra(PinLockActivity.EXTRA_VERIFY_ONLY, true)
        }
        pinVerifyLauncher.launch(intent)
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        startForegroundService(Intent(this, FilterVpnService::class.java))
    }

    private fun stopVpnService() {
        val intent = Intent(this, FilterVpnService::class.java).apply {
            action = FilterVpnService.ACTION_STOP
        }
        startService(intent)
    }

    private fun requestDeviceAdmin() {
        val componentName = ComponentName(this, FilterDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "נדרש כדי למנוע הסרת האפליקציה")
        }
        startActivity(intent)
    }
}
