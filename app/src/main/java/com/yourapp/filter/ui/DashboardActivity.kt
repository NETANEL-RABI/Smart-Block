package com.yourapp.filter.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.yourapp.filter.admin.FilterDeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import com.yourapp.filter.R
import com.yourapp.filter.vpn.FilterVpnService

class DashboardActivity : AppCompatActivity() {

    private val vpnPermissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                startVpnService()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        findViewById<android.widget.Button>(R.id.btnStartFilter).setOnClickListener {
            requestVpnPermission()
        }
        findViewById<android.widget.Button>(R.id.btnStopFilter).setOnClickListener {
            stopVpnService()
        }
        findViewById<android.widget.Button>(R.id.btnEnableAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<android.widget.Button>(R.id.btnEnableDeviceAdmin).setOnClickListener {
            requestDeviceAdmin()
        }
        findViewById<android.widget.Button>(R.id.btnSchedule).setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
        }
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
        val intent = Intent(this, FilterVpnService::class.java)
        startForegroundService(intent)
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
