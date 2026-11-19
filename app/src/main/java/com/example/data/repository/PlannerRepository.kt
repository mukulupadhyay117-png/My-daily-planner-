package com.example.data.repository

import com.example.data.local.PlannerDao
import com.example.data.local.PreferencesRepository
import com.example.data.local.SeedData
import com.example.data.model.HabitEntity
import com.example.data.model.HabitLogEntity
import com.example.data.model.RepeatRule
import com.example.data.model.RoutineBlockEntity
import com.example.data.model.RoutineCompletionEntity
import com.example.data.model.RoutineType
import com.example.data.model.TaskCategory
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.ThemeMode
import com.example.data.model.UserPreferences
import com.example.notifications.ReminderScheduler
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class PlannerRepository(
    private val dao: PlannerDao,
    private val prefsRepo: PreferencesRepository,
    private val scheduler: ReminderScheduler
) {
    val tasksFlow: Flow<List<TaskEntity>> = dao.observeAllTasks()
    val routinesFlow: Flow<List<RoutineBlockEntity>> = dao.observeAllRoutines()
    val routineCompletionsFlow: Flow<List<RoutineCompletionEntity>> = dao.observeAllRoutineCompletions()
    val habitsFlow: Flow<List<HabitEntity>> = dao.observeActiveHabits()
    val habitLogsFlow: Flow<List<HabitLogEntity>> = dao.observeAllHabitLogs()
    val preferencesFlow: Flow<UserPreferences> = prefsRepo.preferencesFlow

    suspend fun seedInitialDataIfNeeded() {
        val prefs = prefsRepo.currentPreferences()
        if (!prefs.isSeeded) {
            if (dao.getAllRoutinesOnce().isEmpty()) {
                dao.insertRoutines(SeedData.defaultRoutines())
            }
            if (dao.getAllHabitsOnce().isEmpty()) {
                dao.insertHabits(SeedData.defaultHabits())
            }
            if (dao.getAllTasksOnce().isEmpty()) {
                dao.insertTasks(SeedData.defaultStarterTasks())
            }
            prefsRepo.markSeeded()
        }
        rescheduleAllAlarms()
    }

    suspend fun rescheduleAllAlarms() {
        val prefs = prefsRepo.currentPreferences()
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
    }

    // --- Task Operations ---
    suspend fun saveTask(task: TaskEntity): Long {
        val savedId = if (task.id == 0L) {
            dao.insertTask(task)
        } else {
            dao.updateTask(task)
            task.id
        }
        val persisted = task.copy(id = savedId)
        if (persisted.isCompleted) {
            scheduler.cancelTaskReminder(savedId)
        } else {
            scheduler.scheduleTaskReminder(persisted)
        }
        return savedId
    }

    suspend fun toggleTaskCompletion(task: TaskEntity) {
        val nowCompleted = !task.isCompleted
        val updated = task.copy(
            isCompleted = nowCompleted,
            completedAtMillis = if (nowCompleted) System.currentTimeMillis() else null
        )
        dao.updateTask(updated)

        if (nowCompleted) {
            scheduler.cancelTaskReminder(task.id)
            if (task.repeatRule != RepeatRule.NONE) {
                val nextDateKey = DateTimeUtils.nextOccurrenceDateKey(task.dateKey, task.repeatRule)
                val existingTasks = dao.getAllTasksOnce()
                val alreadyScheduled = existingTasks.any {
                    it.title == task.title && it.dateKey == nextDateKey && !it.isCompleted
                }
                if (!alreadyScheduled) {
                    val nextTask = task.copy(
                        id = 0L,
                        dateKey = nextDateKey,
                        isCompleted = false,
                        completedAtMillis = null,
                        missedNotified = false,
                        createdAtMillis = System.currentTimeMillis()
                    )
                    val newId = dao.insertTask(nextTask)
                    scheduler.scheduleTaskReminder(nextTask.copy(id = newId))
                }
            }
        } else {
            scheduler.scheduleTaskReminder(updated)
        }
    }

    suspend fun deleteTask(task: TaskEntity) {
        scheduler.cancelTaskReminder(task.id)
        dao.deleteTask(task)
    }

    // --- Routine Operations ---
    suspend fun saveRoutine(routine: RoutineBlockEntity) {
        val savedId = if (routine.id == 0L) {
            dao.insertRoutine(routine)
        } else {
            dao.updateRoutine(routine)
            routine.id
        }
        scheduler.scheduleRoutineReminder(routine.copy(id = savedId))
    }

    suspend fun deleteRoutine(routineId: Long) {
        dao.deleteRoutineById(routineId)
    }

    suspend fun toggleRoutineCompletion(routineId: Long, dateKey: String, currentlyCompleted: Boolean) {
        if (currentlyCompleted) {
            dao.deleteRoutineCompletion(routineId, dateKey)
        } else {
            dao.insertRoutineCompletion(RoutineCompletionEntity(routineId, dateKey))
        }
    }

    // --- Habit Operations ---
    suspend fun saveHabit(habit: HabitEntity) {
        val savedId = if (habit.id == 0L) {
            dao.insertHabit(habit)
        } else {
            dao.updateHabit(habit)
            habit.id
        }
        scheduler.scheduleHabitReminder(habit.copy(id = savedId))
    }

    suspend fun deleteHabit(habitId: Long) {
        dao.deleteHabitLogsForHabit(habitId)
        dao.deleteHabitById(habitId)
    }

    suspend fun incrementHabit(habit: HabitEntity, dateKey: String) {
        val current = dao.getHabitLog(habit.id, dateKey)
        val nextCount = ((current?.currentCount ?: 0) + 1).coerceAtMost(habit.targetCount)
        val completed = nextCount >= habit.targetCount
        dao.upsertHabitLog(
            HabitLogEntity(
                habitId = habit.id,
                dateKey = dateKey,
                currentCount = nextCount,
                isCompleted = completed
            )
        )
    }

    suspend fun decrementHabit(habit: HabitEntity, dateKey: String) {
        val current = dao.getHabitLog(habit.id, dateKey) ?: return
        val nextCount = (current.currentCount - 1).coerceAtLeast(0)
        dao.upsertHabitLog(
            HabitLogEntity(
                habitId = habit.id,
                dateKey = dateKey,
                currentCount = nextCount,
                isCompleted = nextCount >= habit.targetCount
            )
        )
    }

    suspend fun toggleHabitDay(habit: HabitEntity, dateKey: String) {
        val current = dao.getHabitLog(habit.id, dateKey)
        val currentlyCompleted = current?.isCompleted == true
        val newCompleted = !currentlyCompleted
        val newCount = if (newCompleted) habit.targetCount else 0
        dao.upsertHabitLog(
            HabitLogEntity(
                habitId = habit.id,
                dateKey = dateKey,
                currentCount = newCount,
                isCompleted = newCompleted
            )
        )
    }

    // --- Preferences ---
    suspend fun updateProfile(name: String, goal: String, avatarUri: String? = null) =
        prefsRepo.updateProfile(name, goal, avatarUri)

    suspend fun setThemeMode(mode: ThemeMode) = prefsRepo.setThemeMode(mode)

    suspend fun updateReminderSettings(
        dailyEnabled: Boolean,
        morningMinutes: Int,
        eveningMinutes: Int,
        missedEnabled: Boolean,
        defaultLeadMinutes: Int
    ) {
        prefsRepo.updateReminderSettings(
            dailyEnabled, morningMinutes, eveningMinutes, missedEnabled, defaultLeadMinutes
        )
        scheduler.scheduleDailyReminders(prefsRepo.currentPreferences())
    }

    suspend fun setShowQuotes(show: Boolean) = prefsRepo.setShowQuotes(show)

    // --- Backup & Restore (JSON) ---
    suspend fun exportBackupJson(): String {
        val prefs = prefsRepo.currentPreferences()
        val tasks = dao.getAllTasksOnce()
        val routines = dao.getAllRoutinesOnce()
        val routineCompletions = dao.getAllRoutineCompletionsOnce()
        val habits = dao.getAllHabitsOnce()
        val habitLogs = dao.getAllHabitLogsOnce()

        val root = JSONObject().apply {
            put("app", "My Daily Planner")
            put("version", 1)
            put("exportedAtMillis", System.currentTimeMillis())

            put("preferences", JSONObject().apply {
                put("userName", prefs.userName)
                put("userGoal", prefs.userGoal)
                put("avatarUri", prefs.avatarUri)
                put("themeMode", prefs.themeMode.name)
                put("dailyReminderEnabled", prefs.dailyReminderEnabled)
                put("morningReminderMinutes", prefs.morningReminderMinutes)
                put("eveningReminderMinutes", prefs.eveningReminderMinutes)
                put("missedTaskNotificationsEnabled", prefs.missedTaskNotificationsEnabled)
                put("defaultLeadMinutes", prefs.defaultLeadMinutes)
                put("showMotivationalQuotes", prefs.showMotivationalQuotes)
            })

            put("tasks", JSONArray().apply {
                tasks.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("description", t.description)
                        put("dateKey", t.dateKey)
                        put("timeMinutes", t.timeMinutes)
                        put("priority", t.priority.name)
                        put("category", t.category.name)
                        put("isCompleted", t.isCompleted)
                        put("completedAtMillis", t.completedAtMillis ?: JSONObject.NULL)
                        put("repeatRule", t.repeatRule.name)
                        put("reminderMinutesBefore", t.reminderMinutesBefore)
                        put("missedNotified", t.missedNotified)
                        put("createdAtMillis", t.createdAtMillis)
                    })
                }
            })

            put("routines", JSONArray().apply {
                routines.forEach { r ->
                    put(JSONObject().apply {
                        put("id", r.id)
                        put("title", r.title)
                        put("routineType", r.routineType.name)
                        put("startMinutes", r.startMinutes)
                        put("endMinutes", r.endMinutes)
                        put("daysOfWeekMask", r.daysOfWeekMask)
                        put("reminderEnabled", r.reminderEnabled)
                        put("notes", r.notes)
                        put("isEnabled", r.isEnabled)
                        put("sortOrder", r.sortOrder)
                    })
                }
            })

            put("routineCompletions", JSONArray().apply {
                routineCompletions.forEach { c ->
                    put(JSONObject().apply {
                        put("routineId", c.routineId)
                        put("dateKey", c.dateKey)
                        put("completedAtMillis", c.completedAtMillis)
                    })
                }
            })

            put("habits", JSONArray().apply {
                habits.forEach { h ->
                    put(JSONObject().apply {
                        put("id", h.id)
                        put("title", h.title)
                        put("iconKey", h.iconKey)
                        put("targetCount", h.targetCount)
                        put("unitLabel", h.unitLabel)
                        put("reminderMinutes", h.reminderMinutes)
                        put("accentIndex", h.accentIndex)
                        put("createdAtMillis", h.createdAtMillis)
                        put("isArchived", h.isArchived)
                    })
                }
            })

            put("habitLogs", JSONArray().apply {
                habitLogs.forEach { l ->
                    put(JSONObject().apply {
                        put("habitId", l.habitId)
                        put("dateKey", l.dateKey)
                        put("currentCount", l.currentCount)
                        put("isCompleted", l.isCompleted)
                    })
                }
            })
        }
        return root.toString(2)
    }

    suspend fun importBackupJson(rawJson: String): Result<String> = runCatching {
        val root = JSONObject(rawJson)
        val tasksArr = root.optJSONArray("tasks") ?: JSONArray()
        val routinesArr = root.optJSONArray("routines") ?: JSONArray()
        val routineCompArr = root.optJSONArray("routineCompletions") ?: JSONArray()
        val habitsArr = root.optJSONArray("habits") ?: JSONArray()
        val habitLogsArr = root.optJSONArray("habitLogs") ?: JSONArray()

        val tasks = buildList {
            for (i in 0 until tasksArr.length()) {
                val o = tasksArr.getJSONObject(i)
                add(
                    TaskEntity(
                        id = o.optLong("id", 0L),
                        title = o.getString("title"),
                        description = o.optString("description", ""),
                        dateKey = o.getString("dateKey"),
                        timeMinutes = o.optInt("timeMinutes", 9 * 60),
                        priority = enumValues<TaskPriority>().firstOrNull {
                            it.name == o.optString("priority")
                        } ?: TaskPriority.MEDIUM,
                        category = enumValues<TaskCategory>().firstOrNull {
                            it.name == o.optString("category")
                        } ?: TaskCategory.OTHER,
                        isCompleted = o.optBoolean("isCompleted", false),
                        completedAtMillis = if (o.isNull("completedAtMillis")) null else o.optLong("completedAtMillis"),
                        repeatRule = enumValues<RepeatRule>().firstOrNull {
                            it.name == o.optString("repeatRule")
                        } ?: RepeatRule.NONE,
                        reminderMinutesBefore = o.optInt("reminderMinutesBefore", 15),
                        missedNotified = o.optBoolean("missedNotified", false),
                        createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }
        }

        val routines = buildList {
            for (i in 0 until routinesArr.length()) {
                val o = routinesArr.getJSONObject(i)
                add(
                    RoutineBlockEntity(
                        id = o.optLong("id", 0L),
                        title = o.getString("title"),
                        routineType = enumValues<RoutineType>().firstOrNull {
                            it.name == o.optString("routineType")
                        } ?: RoutineType.CUSTOM,
                        startMinutes = o.optInt("startMinutes", 8 * 60),
                        endMinutes = o.optInt("endMinutes", 9 * 60),
                        daysOfWeekMask = o.optInt("daysOfWeekMask", 127),
                        reminderEnabled = o.optBoolean("reminderEnabled", true),
                        notes = o.optString("notes", ""),
                        isEnabled = o.optBoolean("isEnabled", true),
                        sortOrder = o.optInt("sortOrder", i)
                    )
                )
            }
        }

        val routineCompletions = buildList {
            for (i in 0 until routineCompArr.length()) {
                val o = routineCompArr.getJSONObject(i)
                add(
                    RoutineCompletionEntity(
                        routineId = o.getLong("routineId"),
                        dateKey = o.getString("dateKey"),
                        completedAtMillis = o.optLong("completedAtMillis", System.currentTimeMillis())
                    )
                )
            }
        }

        val habits = buildList {
            for (i in 0 until habitsArr.length()) {
                val o = habitsArr.getJSONObject(i)
                add(
                    HabitEntity(
                        id = o.optLong("id", 0L),
                        title = o.getString("title"),
                        iconKey = o.optString("iconKey", "star"),
                        targetCount = o.optInt("targetCount", 1),
                        unitLabel = o.optString("unitLabel", "time"),
                        reminderMinutes = o.optInt("reminderMinutes", -1),
                        accentIndex = o.optInt("accentIndex", 0),
                        createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
                        isArchived = o.optBoolean("isArchived", false)
                    )
                )
            }
        }

        val habitLogs = buildList {
            for (i in 0 until habitLogsArr.length()) {
                val o = habitLogsArr.getJSONObject(i)
                add(
                    HabitLogEntity(
                        habitId = o.getLong("habitId"),
                        dateKey = o.getString("dateKey"),
                        currentCount = o.optInt("currentCount", 0),
                        isCompleted = o.optBoolean("isCompleted", false)
                    )
                )
            }
        }

        dao.restoreSnapshot(tasks, routines, routineCompletions, habits, habitLogs)

        root.optJSONObject("preferences")?.let { p ->
            prefsRepo.restorePreferences(
                UserPreferences(
                    userName = p.optString("userName", "Planner User"),
                    userGoal = p.optString("userGoal", "Build calm, consistent daily momentum"),
                    avatarUri = p.optString("avatarUri", ""),
                    themeMode = enumValues<ThemeMode>().firstOrNull {
                        it.name == p.optString("themeMode")
                    } ?: ThemeMode.SYSTEM,
                    dailyReminderEnabled = p.optBoolean("dailyReminderEnabled", true),
                    morningReminderMinutes = p.optInt("morningReminderMinutes", 8 * 60),
                    eveningReminderMinutes = p.optInt("eveningReminderMinutes", 21 * 60),
                    missedTaskNotificationsEnabled = p.optBoolean("missedTaskNotificationsEnabled", true),
                    defaultLeadMinutes = p.optInt("defaultLeadMinutes", 15),
                    showMotivationalQuotes = p.optBoolean("showMotivationalQuotes", true),
                    isSeeded = true
                )
            )
        }

        rescheduleAllAlarms()
        "Restored ${tasks.size} tasks, ${routines.size} routines, and ${habits.size} habits."
    }

    companion object {
        fun calculateCurrentStreak(completedDates: Set<String>, todayKey: String): Int {
            if (completedDates.isEmpty()) return 0
            var cursor = if (completedDates.contains(todayKey)) {
                todayKey
            } else {
                val yesterday = DateTimeUtils.addDays(todayKey, -1)
                if (completedDates.contains(yesterday)) yesterday else return 0
            }
            var streak = 0
            while (completedDates.contains(cursor)) {
                streak++
                cursor = DateTimeUtils.addDays(cursor, -1)
            }
            return streak
        }

        fun calculateBestStreak(completedDates: Set<String>): Int {
            if (completedDates.isEmpty()) return 0
            val sorted = completedDates.sorted()
            var best = 1
            var run = 1
            for (i in 1 until sorted.size) {
                if (DateTimeUtils.addDays(sorted[i - 1], 1) == sorted[i]) {
                    run++
                    if (run > best) best = run
                } else {
                    run = 1
                }
            }
            return best
        }
    }
}
