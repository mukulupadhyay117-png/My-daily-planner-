package com.example.data.model

enum class TaskPriority(val label: String, val weight: Int) {
    HIGH("High", 3),
    MEDIUM("Medium", 2),
    LOW("Low", 1)
}

enum class TaskCategory(val label: String) {
    STUDY("Study"),
    PERSONAL("Personal"),
    WORK("Work"),
    HEALTH("Health"),
    OTHER("Other")
}

enum class RepeatRule(val label: String) {
    NONE("No Repeat"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

enum class RoutineType(val label: String) {
    MORNING("Morning Routine"),
    STUDY("Study Time"),
    EXERCISE("Exercise Time"),
    WORK_SCHOOL("School / Work Time"),
    BREAK("Break Time"),
    EVENING("Evening Routine"),
    NIGHT("Night Routine"),
    CUSTOM("Custom Schedule")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

enum class CalendarMode(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

enum class TaskStatusFilter(val label: String) {
    PENDING("Pending"),
    COMPLETED("Completed"),
    ALL("All")
}
