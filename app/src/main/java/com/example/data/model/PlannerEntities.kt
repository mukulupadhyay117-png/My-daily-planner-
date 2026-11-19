package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String = "",
    val dateKey: String, // Format: yyyy-MM-dd
    val timeMinutes: Int = 9 * 60, // 0..1439, or -1 for any time
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: TaskCategory = TaskCategory.PERSONAL,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val repeatRule: RepeatRule = RepeatRule.NONE,
    val reminderMinutesBefore: Int = 15, // -1 = Off, 0 = At time, 5, 10, 15, 30, 60
    val missedNotified: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "routine_blocks")
data class RoutineBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val routineType: RoutineType = RoutineType.CUSTOM,
    val startMinutes: Int, // 0..1439
    val endMinutes: Int,   // 0..1439
    val daysOfWeekMask: Int = 127, // Bit 0 = Mon .. Bit 6 = Sun (127 = all days)
    val reminderEnabled: Boolean = true,
    val notes: String = "",
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "routine_completions",
    primaryKeys = ["routineId", "dateKey"]
)
data class RoutineCompletionEntity(
    val routineId: Long,
    val dateKey: String,
    val completedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val iconKey: String = "star", // book, study, exercise, water, meditation, star, sleep, code
    val targetCount: Int = 1,
    val unitLabel: String = "time",
    val reminderMinutes: Int = -1, // -1 = Off
    val accentIndex: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)

@Entity(
    tableName = "habit_logs",
    primaryKeys = ["habitId", "dateKey"]
)
data class HabitLogEntity(
    val habitId: Long,
    val dateKey: String,
    val currentCount: Int = 0,
    val isCompleted: Boolean = false
)

data class UserPreferences(
    val userName: String = "Alex Morgan",
    val userGoal: String = "Build calm, consistent daily momentum",
    val avatarUri: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dailyReminderEnabled: Boolean = true,
    val morningReminderMinutes: Int = 8 * 60, // 08:00 AM
    val eveningReminderMinutes: Int = 21 * 60, // 09:00 PM
    val missedTaskNotificationsEnabled: Boolean = true,
    val defaultLeadMinutes: Int = 15,
    val showMotivationalQuotes: Boolean = true,
    val isSeeded: Boolean = false
)

data class HabitWithProgress(
    val habit: HabitEntity,
    val todayCount: Int,
    val isCompletedToday: Boolean,
    val currentStreak: Int,
    val bestStreak: Int,
    val weekCompletionMap: Map<String, Boolean> // dateKey -> completed
)

data class RoutineBlockWithState(
    val block: RoutineBlockEntity,
    val isCompletedToday: Boolean
)
