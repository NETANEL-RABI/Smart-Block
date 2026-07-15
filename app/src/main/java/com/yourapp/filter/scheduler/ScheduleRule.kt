package com.yourapp.filter.scheduler

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RuleType { APP, DOMAIN }

@Entity(tableName = "schedule_rules")
data class ScheduleRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val targetPackageOrDomain: String,
    val type: RuleType,
    val daysOfWeekMask: Int,      // ביט לכל יום: ראשון=1, שני=2, שלישי=4 ... שבת=64
    val startMinuteOfDay: Int,    // דקות מחצות, למשל 22:00 = 1320
    val endMinuteOfDay: Int,
    val isEnabled: Boolean = true
)
