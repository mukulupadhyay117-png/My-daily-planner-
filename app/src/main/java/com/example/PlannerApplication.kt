package com.example

import android.app.Application
import com.example.data.local.PlannerDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.repository.PlannerRepository
import com.example.notifications.NotificationHelper
import com.example.notifications.ReminderScheduler

class PlannerApplication : Application() {
    val database by lazy { PlannerDatabase.getInstance(this) }
    val preferencesRepository by lazy { PreferencesRepository(this) }
    val reminderScheduler by lazy { ReminderScheduler(this) }
    val repository by lazy {
        PlannerRepository(
            dao = database.plannerDao(),
            prefsRepo = preferencesRepository,
            scheduler = reminderScheduler
        )
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
    }
}
