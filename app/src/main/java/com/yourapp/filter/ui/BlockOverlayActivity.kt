package com.yourapp.filter.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yourapp.filter.R
import com.yourapp.filter.scheduler.ScheduleManager
import kotlinx.coroutines.launch

class BlockOverlayActivity : AppCompatActivity() {

    private lateinit var scheduleManager: ScheduleManager
    private var blockedPackage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_overlay)

        scheduleManager = ScheduleManager.getInstance(this)
        blockedPackage = intent.getStringExtra("blocked_package") ?: ""

        findViewById<android.widget.TextView>(R.id.tvBlockedMessage).text =
            "האפליקציה חסומה כרגע לפי לוח הזמנים שהוגדר"

        findViewById<android.widget.Button>(R.id.btnClose).setOnClickListener { finish() }

        findViewById<android.widget.Button>(R.id.btn5Min).setOnClickListener { grantAccess(5) }
        findViewById<android.widget.Button>(R.id.btn10Min).setOnClickListener { grantAccess(10) }
        findViewById<android.widget.Button>(R.id.btn15Min).setOnClickListener { grantAccess(15) }
    }

    private fun grantAccess(minutes: Int) {
        lifecycleScope.launch {
            scheduleManager.grantTemporaryAccess(blockedPackage, minutes)
            launchTargetApp()
            finish()
        }
    }

    private fun launchTargetApp() {
        val launchIntent = packageManager.getLaunchIntentForPackage(blockedPackage)
        if (launchIntent != null) {
            startActivity(launchIntent)
        }
    }

    override fun onBackPressed() {
        finishAffinity()
    }
}
