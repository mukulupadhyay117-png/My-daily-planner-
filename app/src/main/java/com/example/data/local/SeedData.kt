package com.example.data.local

import com.example.data.model.HabitEntity
import com.example.data.model.RepeatRule
import com.example.data.model.RoutineBlockEntity
import com.example.data.model.RoutineType
import com.example.data.model.TaskCategory
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.util.DateTimeUtils

object SeedData {
    fun defaultRoutines(): List<RoutineBlockEntity> = listOf(
        RoutineBlockEntity(
            title = "Morning Routine",
            routineType = RoutineType.MORNING,
            startMinutes = 6 * 60 + 30,
            endMinutes = 7 * 60 + 30,
            daysOfWeekMask = 127,
            reminderEnabled = true,
            notes = "Hydrate, stretch, review today's top 3 priorities",
            sortOrder = 1
        ),
        RoutineBlockEntity(
            title = "Deep Study Session",
            routineType = RoutineType.STUDY,
            startMinutes = 7 * 60 + 45,
            endMinutes = 9 * 60,
            daysOfWeekMask = 127,
            reminderEnabled = true,
            notes = "Focused distraction-free learning block",
            sortOrder = 2
        ),
        RoutineBlockEntity(
            title = "School / Work Block",
            routineType = RoutineType.WORK_SCHOOL,
            startMinutes = 9 * 60 + 30,
            endMinutes = 13 * 60,
            daysOfWeekMask = 31, // Mon-Fri
            reminderEnabled = true,
            notes = "Classes, core projects, and deep work",
            sortOrder = 3
        ),
        RoutineBlockEntity(
            title = "Mindful Break & Lunch",
            routineType = RoutineType.BREAK,
            startMinutes = 13 * 60,
            endMinutes = 14 * 60,
            daysOfWeekMask = 127,
            reminderEnabled = false,
            notes = "Step away from screens, nourish, and recharge",
            sortOrder = 4
        ),
        RoutineBlockEntity(
            title = "Exercise & Movement",
            routineType = RoutineType.EXERCISE,
            startMinutes = 17 * 60 + 30,
            endMinutes = 18 * 60 + 30,
            daysOfWeekMask = 127,
            reminderEnabled = true,
            notes = "Strength, brisk walk, or mobility workout",
            sortOrder = 5
        ),
        RoutineBlockEntity(
            title = "Evening Wind-Down",
            routineType = RoutineType.EVENING,
            startMinutes = 19 * 60 + 30,
            endMinutes = 20 * 60 + 45,
            daysOfWeekMask = 127,
            reminderEnabled = true,
            notes = "Dinner, tidy desk, prep tomorrow's checklist",
            sortOrder = 6
        ),
        RoutineBlockEntity(
            title = "Night Routine & Reading",
            routineType = RoutineType.NIGHT,
            startMinutes = 21 * 60 + 30,
            endMinutes = 22 * 60 + 30,
            daysOfWeekMask = 127,
            reminderEnabled = true,
            notes = "Screens off, read 20 pages, restful sleep",
            sortOrder = 7
        )
    )

    fun defaultHabits(): List<HabitEntity> = listOf(
        HabitEntity(
            title = "Reading Books",
            iconKey = "book",
            targetCount = 1,
            unitLabel = "session",
            reminderMinutes = 21 * 60 + 30,
            accentIndex = 0
        ),
        HabitEntity(
            title = "Studying",
            iconKey = "study",
            targetCount = 1,
            unitLabel = "block",
            reminderMinutes = 8 * 60,
            accentIndex = 1
        ),
        HabitEntity(
            title = "Exercise",
            iconKey = "exercise",
            targetCount = 1,
            unitLabel = "workout",
            reminderMinutes = 17 * 60 + 30,
            accentIndex = 2
        ),
        HabitEntity(
            title = "Drinking Water",
            iconKey = "water",
            targetCount = 8,
            unitLabel = "glasses",
            reminderMinutes = 11 * 60,
            accentIndex = 3
        ),
        HabitEntity(
            title = "Meditation",
            iconKey = "meditation",
            targetCount = 1,
            unitLabel = "session",
            reminderMinutes = 7 * 60,
            accentIndex = 4
        )
    )

    fun defaultStarterTasks(): List<TaskEntity> {
        val today = DateTimeUtils.todayKey()
        val tomorrow = DateTimeUtils.addDays(today, 1)
        return listOf(
            TaskEntity(
                title = "Finalize weekly study notes",
                description = "Summarize key concepts and highlight practice questions",
                dateKey = today,
                timeMinutes = 10 * 60,
                priority = TaskPriority.HIGH,
                category = TaskCategory.STUDY,
                isCompleted = false,
                repeatRule = RepeatRule.NONE,
                reminderMinutesBefore = 15
            ),
            TaskEntity(
                title = "30-minute brisk outdoor walk",
                description = "Afternoon energy reset without screens",
                dateKey = today,
                timeMinutes = 17 * 60 + 30,
                priority = TaskPriority.MEDIUM,
                category = TaskCategory.HEALTH,
                isCompleted = false,
                repeatRule = RepeatRule.DAILY,
                reminderMinutesBefore = 15
            ),
            TaskEntity(
                title = "Plan tomorrow's top 3 priorities",
                description = "Keep the next morning calm and focused",
                dateKey = today,
                timeMinutes = 20 * 60 + 30,
                priority = TaskPriority.LOW,
                category = TaskCategory.PERSONAL,
                isCompleted = true,
                completedAtMillis = System.currentTimeMillis(),
                repeatRule = RepeatRule.DAILY,
                reminderMinutesBefore = 10
            ),
            TaskEntity(
                title = "Review project milestones",
                description = "Check upcoming deadlines and update task estimates",
                dateKey = tomorrow,
                timeMinutes = 11 * 60,
                priority = TaskPriority.HIGH,
                category = TaskCategory.WORK,
                isCompleted = false,
                repeatRule = RepeatRule.WEEKLY,
                reminderMinutesBefore = 30
            )
        )
    }
}
