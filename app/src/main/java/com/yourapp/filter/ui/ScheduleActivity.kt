package com.yourapp.filter.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yourapp.filter.R
import com.yourapp.filter.scheduler.RuleType
import com.yourapp.filter.scheduler.ScheduleManager
import com.yourapp.filter.scheduler.ScheduleRule
import kotlinx.coroutines.launch

class ScheduleActivity : AppCompatActivity() {

    private lateinit var scheduleManager: ScheduleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)
        scheduleManager = ScheduleManager.getInstance(this)

        findViewById<android.widget.Button>(R.id.btnAddRule).setOnClickListener {
            addExampleRule()
        }
    }

    private fun addExampleRule() {
        val pkgEditText = findViewById<android.widget.EditText>(R.id.etTargetPackage)
        val target = pkgEditText.text.toString().trim()
        if (target.isEmpty()) return

        // דוגמת חוק ברירת מחדל: חסום כל יום, 22:00-07:00 (ניתן להרחיב עם UI לבחירת ימים/שעות)
        val rule = ScheduleRule(
            targetPackageOrDomain = target,
            type = RuleType.APP,
            daysOfWeekMask = 0b1111111, // כל השבוע
            startMinuteOfDay = 22 * 60,
            endMinuteOfDay = 24 * 60 - 1
        )
        lifecycleScope.launch {
            scheduleManager.addRule(rule)
            pkgEditText.text.clear()
        }
    }
}
