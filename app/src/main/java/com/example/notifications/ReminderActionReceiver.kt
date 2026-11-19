package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.PlannerDatabase
import com.example.data.model.RepeatRule
import com.example.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val appContext = context.applicationContext

        if (notificationId != -1) {
            runCatching {
                NotificationManagerCompat.from(appContext).cancel(notificationId)
            }
        }

        if (taskId <= 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = PlannerDatabase.getInstance(appContext).plannerDao()
                val scheduler = ReminderScheduler(appContext)
                when (action) {
                    ACTION_MARK_DONE -> {
                        val task = dao.getTaskById(taskId)
                        if (task != null && !task.isCompleted) {
                            dao.updateTask(
                                task.copy(
                                    isCompleted = true,
                                    completedAtMillis = System.currentTimeMillis()
                                )
                            )
                            scheduler.cancelTaskReminder(taskId)
                            if (task.repeatRule != RepeatRule.NONE) {
                                val nextKey = DateTimeUtils.nextOccurrenceDateKey(task.dateKey, task.repeatRule)
                                val nextTask = task.copy(
                                    id = 0L,
                                    dateKey = nextKey,
                                    isCompleted = false,
                                    completedAtMillis = null,
                                    missedNotified = false,
                                    createdAtMillis = System.currentTimeMillis()
                                )
                                val newId = dao.insertTask(nextTask)
                                scheduler.scheduleTaskReminder(nextTask.copy(id = newId))
                            }
                        }
                    }

                    ACTION_SNOOZE_15 -> {
                        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Task Reminder"
                        scheduler.scheduleSnooze(taskId, title, minutes = 15)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_DONE = "com.example.ACTION_MARK_DONE"
        const val ACTION_SNOOZE_15 = "com.example.ACTION_SNOOZE_15"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
