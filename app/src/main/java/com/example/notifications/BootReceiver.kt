package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.PlannerDatabase
import com.example.data.local.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = PlannerDatabase.getInstance(appContext).plannerDao()
                val prefs = PreferencesRepository(appContext).currentPreferences()
                val scheduler = ReminderScheduler(appContext)
                scheduler.scheduleDailyReminders(prefs)
                dao.getAllTasksOnce().filter { !it.isCompleted }.forEach {
                    scheduler.scheduleTaskReminder(it)
                }
                dao.getAllHabitsOnce().filter { !it.isArchived }.forEach {
                    scheduler.scheduleHabitReminder(it)
                }
                dao.getAllRoutinesOnce().filter { it.isEnabled && it.reminderEnabled }.forEach {
                    scheduler.scheduleRoutineReminder(it)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
