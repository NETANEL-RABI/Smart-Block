package com.yourapp.filter.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.yourapp.filter.R

class BlockOverlayActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_overlay)

        val blockedPackage = intent.getStringExtra("blocked_package") ?: ""
        findViewById<android.widget.TextView>(R.id.tvBlockedMessage).text =
            "האפליקציה חסומה כרגע לפי לוח הזמנים שהוגדר"

        findViewById<android.widget.Button>(R.id.btnClose).setOnClickListener { finish() }
    }

    override fun onBackPressed() {
        // מונע חזרה אחורה אל האפליקציה החסומה
        finishAffinity()
    }
}
