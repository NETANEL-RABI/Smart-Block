package com.yourapp.filter.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yourapp.filter.R
import com.yourapp.filter.security.PinRepository

/**
 * שני מצבים:
 * 1. מצב רגיל (launcher) - אם אין PIN מוגדר, מבקש להגדיר אחד; אם יש, מבקש להזין ואז עובר ל-Dashboard.
 * 2. מצב verify_only - משמש כשמסך אחר (כמו Dashboard) רוצה לוודא שהמשתמש יודע את ה-PIN
 *    לפני ביצוע פעולה רגישה (עצירת סינון / כניסה ללוח זמנים), בלי לעבור ל-Dashboard.
 */
class PinLockActivity : AppCompatActivity() {

    private lateinit var pinRepository: PinRepository
    private var isSetupMode = false
    private var verifyOnly = false
    private var firstEnteredPin: String? = null

    companion object {
        const val EXTRA_VERIFY_ONLY = "verify_only"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_lock)

        pinRepository = PinRepository.getInstance(this)
        verifyOnly = intent.getBooleanExtra(EXTRA_VERIFY_ONLY, false)
        isSetupMode = !pinRepository.hasPin() && !verifyOnly

        updateTitle()

        findViewById<android.widget.Button>(R.id.btnConfirmPin).setOnClickListener {
            handleConfirm()
        }
    }

    private fun updateTitle() {
        val title = findViewById<android.widget.TextView>(R.id.tvPinTitle)
        title.text = when {
            isSetupMode && firstEnteredPin == null -> "הגדר קוד PIN חדש (4 ספרות ומעלה)"
            isSetupMode && firstEnteredPin != null -> "הזן שוב לאישור"
            else -> "הזן קוד PIN"
        }
    }

    private fun handleConfirm() {
        val editText = findViewById<android.widget.EditText>(R.id.etPinInput)
        val input = editText.text.toString().trim()
        editText.text.clear()

        if (input.length < 4) {
            Toast.makeText(this, "PIN חייב להכיל לפחות 4 ספרות", Toast.LENGTH_SHORT).show()
            return
        }

        if (isSetupMode) {
            if (firstEnteredPin == null) {
                firstEnteredPin = input
                updateTitle()
            } else if (firstEnteredPin == input) {
                pinRepository.setPin(input)
                Toast.makeText(this, "הקוד נשמר בהצלחה", Toast.LENGTH_SHORT).show()
                goToDashboard()
            } else {
                Toast.makeText(this, "הקודים לא תואמים, נסה שוב", Toast.LENGTH_SHORT).show()
                firstEnteredPin = null
                updateTitle()
            }
        } else {
            if (pinRepository.verifyPin(input)) {
                if (verifyOnly) {
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    goToDashboard()
                }
            } else {
                Toast.makeText(this, "קוד שגוי", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    override fun onBackPressed() {
        if (verifyOnly) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        } else {
            // לא מאפשרים לעקוף את מסך הנעילה בלחיצת "חזרה" - שולחים הביתה במקום
            moveTaskToBack(true)
        }
    }
}
