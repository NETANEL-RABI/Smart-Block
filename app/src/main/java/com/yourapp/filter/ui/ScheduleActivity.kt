package com.yourapp.filter.ui

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yourapp.filter.R
import com.yourapp.filter.scheduler.RuleType
import com.yourapp.filter.scheduler.ScheduleManager
import com.yourapp.filter.scheduler.ScheduleRule
import kotlinx.coroutines.launch

class ScheduleActivity : AppCompatActivity() {

    private lateinit var scheduleManager: ScheduleManager
    private var selectedPackage: String? = null
    private var startHour = 22
    private var startMinute = 0
    private var endHour = 7
    private var endMinute = 0

    private val appPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pkg = result.data?.getStringExtra("selected_package")
            val name = result.data?.getStringExtra("selected_name")
            selectedPackage = pkg
            findViewById<TextView>(R.id.tvSelectedApp).text = "נבחר: $name ($pkg)"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)
        scheduleManager = ScheduleManager.getInstance(this)

        findViewById<android.widget.Button>(R.id.btnPickApp).setOnClickListener {
            appPickerLauncher.launch(Intent(this, AppPickerActivity::class.java))
        }

        findViewById<android.widget.Button>(R.id.btnStartTime).setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                startHour = hour; startMinute = minute
                findViewById<android.widget.Button>(R.id.btnStartTime).text =
                    "התחלה: %02d:%02d".format(hour, minute)
            }, startHour, startMinute, true).show()
        }

        findViewById<android.widget.Button>(R.id.btnEndTime).setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                endHour = hour; endMinute = minute
                findViewById<android.widget.Button>(R.id.btnEndTime).text =
                    "סיום: %02d:%02d".format(hour, minute)
            }, endHour, endMinute, true).show()
        }

        findViewById<android.widget.Button>(R.id.btnAddRule).setOnClickListener {
            addRule()
        }
    }

    private fun addRule() {
        val target = selectedPackage
        if (target == null) {
            Toast.makeText(this, "בחר קודם אפליקציה", Toast.LENGTH_SHORT).show()
            return
        }

        var mask = 0
        val dayCheckboxIds = listOf(
            R.id.cbSunday, R.id.cbMonday, R.id.cbTuesday, R.id.cbWednesday,
            R.id.cbThursday, R.id.cbFriday, R.id.cbSaturday
        )
        dayCheckboxIds.forEachIndexed { index, id ->
            if (findViewById<CheckBox>(id).isChecked) {
                mask = mask or (1 shl index)
            }
        }
        if (mask == 0) mask = 0b1111111 // אם לא סומן יום ספציפי - חל על כל השבוע

        val startMinuteOfDay = startHour * 60 + startMinute
        var endMinuteOfDay = endHour * 60 + endMinute
        if (endMinuteOfDay <= startMinuteOfDay) {
            endMinuteOfDay += 24 * 60 // תמיכה בטווח שחוצה חצות, למשל 22:00-07:00
        }

        val rule = ScheduleRule(
            targetPackageOrDomain = target,
            type = RuleType.APP,
            daysOfWeekMask = mask,
            startMinuteOfDay = startMinuteOfDay,
            endMinuteOfDay = endMinuteOfDay
        )

        lifecycleScope.launch {
            scheduleManager.addRule(rule)
            Toast.makeText(this@ScheduleActivity, "חוק נוסף בהצלחה", Toast.LENGTH_SHORT).show()
        }
    }
}
