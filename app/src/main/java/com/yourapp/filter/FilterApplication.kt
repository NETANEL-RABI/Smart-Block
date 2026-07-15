package com.yourapp.filter

import android.app.Application
import com.yourapp.filter.data.BlockedAppsRepository
import com.yourapp.filter.data.BlockedDomainsRepository
import com.yourapp.filter.scheduler.ScheduleManager

class FilterApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // אתחול מוקדם של המאגרים כדי שיהיו מוכנים ברגע שהשירותים עולים
        BlockedDomainsRepository.getInstance(this)
        BlockedAppsRepository.getInstance(this)
        ScheduleManager.getInstance(this)
    }
}
