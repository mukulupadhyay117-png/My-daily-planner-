package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.HabitEntity
import com.example.data.model.RoutineBlockEntity
import com.example.data.model.TaskEntity
import com.example.data.model.UserPreferences
import com.example.util.DateTimeUtils
import java.util.Calendar

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleTaskReminder(task: TaskEntity) {
        cancelTaskReminder(task.id)
        if (task.isCompleted || task.timeMinutes < 0) return

        val now = System.currentTimeMillis()
        if (task.reminderMinutesBefore >= 0) {
            val triggerAt = DateTimeUtils.toTriggerMillis(
                dateKey = task.dateKey,
                timeMinutes = task.timeMinutes,
                leadMinutes = task.reminderMinutesBefore
            )
            if (triggerAt > now) {
                val leadLabel = if (task.reminderMinutesBefore == 0) {
                    "Scheduled for now (${DateTimeUtils.formatMinutes(task.timeMinutes)})"
                } else {
                    "Starts in ${task.reminderMinutesBefore} min (${DateTimeUtils.formatMinutes(task.timeMinutes)})"
                }
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    action = ReminderReceiver.ACTION_TASK_REMINDER
                    putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id)
                    putExtra(ReminderReceiver.EXTRA_TITLE, task.title)
                    putExtra(ReminderReceiver.EXTRA_BODY, leadLabel)
                }
                scheduleExactOrInexact(
                    requestCode = taskRequestCode(task.id),
                    triggerAtMillis = triggerAt,
                    intent = intent
                )
            }
        }

        // Missed task check 25 minutes after task time
        val missedTrigger = DateTimeUtils.toTriggerMillis(
            dateKey = task.dateKey,
            timeMinutes = task.timeMinutes,
            leadMinutes = -25
        )
        if (missedTrigger > now && !task.missedNotified) {
            val missedIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_MISSED_TASK_CHECK
                putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id)
            }
            scheduleExactOrInexact(
                requestCode = missedRequestCode(task.id),
                triggerAtMillis = missedTrigger,
                intent = missedIntent
            )
        }
    }

    fun cancelTaskReminder(taskId: Long) {
        cancelAlarm(taskRequestCode(taskId), ReminderReceiver.ACTION_TASK_REMINDER)
        cancelAlarm(missedRequestCode(taskId), ReminderReceiver.ACTION_MISSED_TASK_CHECK)
    }

    fun scheduleSnooze(taskId: Long, title: String, minutes: Int = 15) {
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TASK_REMINDER
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_BODY, "Snoozed reminder — ready to tackle this?")
        }
        scheduleExactOrInexact(
            requestCode = taskRequestCode(taskId) + 50_000,
            triggerAtMillis = triggerAt,
            intent = intent
        )
    }

    fun scheduleDailyReminders(prefs: UserPreferences) {
        cancelAlarm(REQ_DAILY_MORNING, ReminderReceiver.ACTION_DAILY_MORNING)
        cancelAlarm(REQ_DAILY_EVENING, ReminderReceiver.ACTION_DAILY_EVENING)
        if (!prefs.dailyReminderEnabled) return

        val morningMillis = nextTimeOccurrenceMillis(prefs.morningReminderMinutes)
        scheduleExactOrInexact(
            requestCode = REQ_DAILY_MORNING,
            triggerAtMillis = morningMillis,
            intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_DAILY_MORNING
            }
        )

        val eveningMillis = nextTimeOccurrenceMillis(prefs.eveningReminderMinutes)
        scheduleExactOrInexact(
            requestCode = REQ_DAILY_EVENING,
            triggerAtMillis = eveningMillis,
            intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_DAILY_EVENING
            }
        )
    }

    fun scheduleHabitReminder(habit: HabitEntity) {
        val reqCode = 400_000 + (habit.id % 50_000L).toInt()
        cancelAlarm(reqCode, ReminderReceiver.ACTION_HABIT_REMINDER)
        if (habit.isArchived || habit.reminderMinutes < 0) return

        val triggerAt = nextTimeOccurrenceMillis(habit.reminderMinutes)
        scheduleExactOrInexact(
            requestCode = reqCode,
            triggerAtMillis = triggerAt,
            intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_HABIT_REMINDER
                putExtra(ReminderReceiver.EXTRA_HABIT_ID, habit.id)
                putExtra(ReminderReceiver.EXTRA_TITLE, "Habit Check-In: ${habit.title}")
                putExtra(ReminderReceiver.EXTRA_BODY, "Keep your streak alive! Target: ${habit.targetCount} ${habit.unitLabel}.")
            }
        )
    }

    fun scheduleRoutineReminder(routine: RoutineBlockEntity) {
        val reqCode = 500_000 + (routine.id % 50_000L).toInt()
        cancelAlarm(reqCode, ReminderReceiver.ACTION_ROUTINE_REMINDER)
        if (!routine.isEnabled || !routine.reminderEnabled) return

        val triggerAt = nextTimeOccurrenceMillis(routine.startMinutes)
        scheduleExactOrInexact(
            requestCode = reqCode,
            triggerAtMillis = triggerAt,
            intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_ROUTINE_REMINDER
                putExtra(ReminderReceiver.EXTRA_ROUTINE_ID, routine.id)
                putExtra(ReminderReceiver.EXTRA_TITLE, routine.title)
                putExtra(
                    ReminderReceiver.EXTRA_BODY,
                    "Routine block starting now (${DateTimeUtils.formatMinutes(routine.startMinutes)} – ${DateTimeUtils.formatMinutes(routine.endMinutes)})"
                )
            }
        )
    }

    private fun scheduleExactOrInexact(requestCode: Int, triggerAtMillis: Long, intent: Intent) {
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }
    }

    private fun cancelAlarm(requestCode: Int, action: String) {
        val intent = Intent(context, ReminderReceiver::class.java).apply { this.action = action }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    private fun nextTimeOccurrenceMillis(minutesFromMidnight: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, (minutesFromMidnight / 60).coerceIn(0, 23))
            set(Calendar.MINUTE, (minutesFromMidnight % 60).coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis
    }

    private fun taskRequestCode(taskId: Long) = 100_000 + (taskId % 50_000L).toInt()
    private fun missedRequestCode(taskId: Long) = 200_000 + (taskId % 50_000L).toInt()

    companion object {
        private const val REQ_DAILY_MORNING = 300_001
        private const val REQ_DAILY_EVENING = 300_002
    }
}
