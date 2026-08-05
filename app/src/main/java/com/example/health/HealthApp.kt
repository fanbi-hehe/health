package com.example.health

import android.app.Application
import com.example.health.data.local.AppDatabase
import com.example.health.data.preference.AppPreferences

class HealthApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferences: AppPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        preferences = AppPreferences(this)
    }
}
