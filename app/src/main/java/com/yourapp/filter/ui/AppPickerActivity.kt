package com.yourapp.filter.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.yourapp.filter.R
import com.yourapp.filter.data.InstalledAppsProvider

class AppPickerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_picker)

        val listView = findViewById<ListView>(R.id.lvApps)
        val apps = InstalledAppsProvider.getInstalledApps(this)
        val names = apps.map { "${it.appName}\n${it.packageName}" }

        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
        listView.setOnItemClickListener { _, _, position, _ ->
            val selected = apps[position]
            val resultIntent = Intent().apply {
                putExtra("selected_package", selected.packageName)
                putExtra("selected_name", selected.appName)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}
