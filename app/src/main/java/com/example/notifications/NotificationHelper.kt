package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_TASKS = "task_reminders"
    const val CHANNEL_DAILY = "daily_briefing"
    const val CHANNEL_MISSED = "missed_tasks"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val taskChannel = NotificationChannel(
                CHANNEL_TASKS,
                context.getString(R.string.notification_channel_tasks),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_tasks_desc)
            }

            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY,
                context.getString(R.string.notification_channel_daily),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_daily_desc)
            }

            val missedChannel = NotificationChannel(
                CHANNEL_MISSED,
                context.getString(R.string.notification_channel_missed),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_missed_desc)
            }

            manager.createNotificationChannel(taskChannel)
            manager.createNotificationChannel(dailyChannel)
            manager.createNotificationChannel(missedChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showTaskNotification(
        context: Context,
        taskId: Long,
        title: String,
        body: String,
        isMissed: Boolean = false
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_MARK_DONE
            putExtra(ReminderActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationIdForTask(taskId, isMissed))
        }
        val donePending = PendingIntent.getBroadcast(
            context,
            ("done_$taskId").hashCode(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_SNOOZE_15
            putExtra(ReminderActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderActionReceiver.EXTRA_TITLE, title)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationIdForTask(taskId, isMissed))
        }
        val snoozePending = PendingIntent.getBroadcast(
            context,
            ("snooze_$taskId").hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (isMissed) CHANNEL_MISSED else CHANNEL_TASKS
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .addAction(0, "Mark Done", donePending)
            .addAction(0, "Snooze 15m", snoozePending)
            .build()

        runCatching {
            NotificationManagerCompat.from(context)
                .notify(notificationIdForTask(taskId, isMissed), notification)
        }
    }

    fun showSimpleReminderNotification(
        context: Context,
        notificationId: Int,
        channelId: String,
        title: String,
        body: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    fun notificationIdForTask(taskId: Long, isMissed: Boolean): Int {
        val base = (taskId % 100000L).toInt()
        return if (isMissed) 200_000 + base else 100_000 + base
    }
}
