package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.PlannerDatabase
import com.example.data.local.PreferencesRepository
import com.example.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PlannerDatabase.getInstance(appContext)
                val dao = db.plannerDao()
                val prefsRepo = PreferencesRepository(appContext)
                val scheduler = ReminderScheduler(appContext)
                val prefs = prefsRepo.currentPreferences()

                when (action) {
                    ACTION_TASK_REMINDER -> {
                        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Task Reminder"
                        val body = intent.getStringExtra(EXTRA_BODY) ?: "Time to focus on your task."
                        if (taskId > 0) {
                            val task = dao.getTaskById(taskId)
                            if (task == null || !task.isCompleted) {
                                NotificationHelper.showTaskNotification(
                                    context = appContext,
                                    taskId = taskId,
                                    title = title,
                                    body = body,
                                    isMissed = false
                                )
                            }
                        }
                    }

                    ACTION_MISSED_TASK_CHECK -> {
                        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                        if (taskId > 0 && prefs.missedTaskNotificationsEnabled) {
                            val task = dao.getTaskById(taskId)
                            if (task != null && !task.isCompleted && !task.missedNotified) {
                                NotificationHelper.showTaskNotification(
                                    context = appContext,
                                    taskId = task.id,
                                    title = "Missed Task: ${task.title}",
                                    body = "Scheduled at ${DateTimeUtils.formatMinutes(task.timeMinutes)} — tap Done or Snooze 15m.",
                                    isMissed = true
                                )
                                dao.updateTask(task.copy(missedNotified = true))
                            }
                        }
                    }

                    ACTION_DAILY_MORNING -> {
                        if (prefs.dailyReminderEnabled) {
                            val today = DateTimeUtils.todayKey()
                            val pendingCount = dao.getAllTasksOnce().count {
                                it.dateKey == today && !it.isCompleted
                            }
                            NotificationHelper.showSimpleReminderNotification(
                                context = appContext,
                                notificationId = 300_001,
                                channelId = NotificationHelper.CHANNEL_DAILY,
                                title = "Good Morning, ${prefs.userName.substringBefore(' ')}",
                                body = if (pendingCount > 0) {
                                    "You have $pendingCount tasks scheduled today. Let's make steady progress!"
                                } else {
                                    "Fresh day ahead! Open My Daily Planner to set your top priorities."
                                }
                            )
                            scheduler.scheduleDailyReminders(prefs)
                        }
                    }

                    ACTION_DAILY_EVENING -> {
                        if (prefs.dailyReminderEnabled) {
                            val today = DateTimeUtils.todayKey()
                            val todayTasks = dao.getAllTasksOnce().filter { it.dateKey == today }
                            val done = todayTasks.count { it.isCompleted }
                            val total = todayTasks.size
                            NotificationHelper.showSimpleReminderNotification(
                                context = appContext,
                                notificationId = 300_002,
                                channelId = NotificationHelper.CHANNEL_DAILY,
                                title = "Evening Wrap-Up",
                                body = if (total > 0) {
                                    "You completed $done of $total tasks today. Log your habits and wind down peacefully."
                                } else {
                                    "Take a moment to check off your daily habits and plan tomorrow."
                                }
                            )
                            scheduler.scheduleDailyReminders(prefs)
                        }
                    }

                    ACTION_HABIT_REMINDER -> {
                        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
                        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Habit Reminder"
                        val body = intent.getStringExtra(EXTRA_BODY) ?: "Keep your streak going!"
                        val today = DateTimeUtils.todayKey()
                        val log = if (habitId > 0) dao.getHabitLog(habitId, today) else null
                        if (log == null || !log.isCompleted) {
                            NotificationHelper.showSimpleReminderNotification(
                                context = appContext,
                                notificationId = 400_000 + (habitId % 50_000L).toInt(),
                                channelId = NotificationHelper.CHANNEL_DAILY,
                                title = title,
                                body = body
                            )
                        }
                        if (habitId > 0) {
                            dao.getAllHabitsOnce().firstOrNull { it.id == habitId }?.let {
                                scheduler.scheduleHabitReminder(it)
                            }
                        }
                    }

                    ACTION_ROUTINE_REMINDER -> {
                        val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)
                        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Routine Block"
                        val body = intent.getStringExtra(EXTRA_BODY) ?: "Your scheduled routine block begins now."
                        val routine = dao.getAllRoutinesOnce().firstOrNull { it.id == routineId }
                        val today = DateTimeUtils.todayKey()
                        if (routine != null && routine.isEnabled &&
                            DateTimeUtils.isRoutineScheduledOnDate(routine.daysOfWeekMask, today)
                        ) {
                            NotificationHelper.showSimpleReminderNotification(
                                context = appContext,
                                notificationId = 500_000 + (routineId % 50_000L).toInt(),
                                channelId = NotificationHelper.CHANNEL_DAILY,
                                title = title,
                                body = body
                            )
                            scheduler.scheduleRoutineReminder(routine)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TASK_REMINDER = "com.example.ACTION_TASK_REMINDER"
        const val ACTION_MISSED_TASK_CHECK = "com.example.ACTION_MISSED_TASK_CHECK"
        const val ACTION_DAILY_MORNING = "com.example.ACTION_DAILY_MORNING"
        const val ACTION_DAILY_EVENING = "com.example.ACTION_DAILY_EVENING"
        const val ACTION_HABIT_REMINDER = "com.example.ACTION_HABIT_REMINDER"
        const val ACTION_ROUTINE_REMINDER = "com.example.ACTION_ROUTINE_REMINDER"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_ROUTINE_ID = "extra_routine_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
    }
}
