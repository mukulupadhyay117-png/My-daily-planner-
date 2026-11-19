package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.RepeatRule
import com.example.data.model.RoutineType
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority

class PlannerConverters {
    @TypeConverter
    fun fromPriority(value: TaskPriority): String = value.name

    @TypeConverter
    fun toPriority(raw: String): TaskPriority =
        enumValues<TaskPriority>().firstOrNull { it.name == raw } ?: TaskPriority.MEDIUM

    @TypeConverter
    fun fromCategory(value: TaskCategory): String = value.name

    @TypeConverter
    fun toCategory(raw: String): TaskCategory =
        enumValues<TaskCategory>().firstOrNull { it.name == raw } ?: TaskCategory.OTHER

    @TypeConverter
    fun fromRepeatRule(value: RepeatRule): String = value.name

    @TypeConverter
    fun toRepeatRule(raw: String): RepeatRule =
        enumValues<RepeatRule>().firstOrNull { it.name == raw } ?: RepeatRule.NONE

    @TypeConverter
    fun fromRoutineType(value: RoutineType): String = value.name

    @TypeConverter
    fun toRoutineType(raw: String): RoutineType =
        enumValues<RoutineType>().firstOrNull { it.name == raw } ?: RoutineType.CUSTOM
}
