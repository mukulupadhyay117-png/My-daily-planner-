package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CalendarMode
import com.example.data.model.HabitEntity
import com.example.data.model.HabitLogEntity
import com.example.data.model.HabitWithProgress
import com.example.data.model.RepeatRule
import com.example.data.model.RoutineBlockEntity
import com.example.data.model.RoutineBlockWithState
import com.example.data.model.RoutineCompletionEntity
import com.example.data.model.TaskCategory
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatusFilter
import com.example.data.model.ThemeMode
import com.example.data.model.UserPreferences
import com.example.data.repository.MotivationalQuote
import com.example.data.repository.PlannerRepository
import com.example.data.repository.QuoteProvider
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class DayTaskSummary(
    val total: Int,
    val completed: Int,
    val pending: Int
)

data class DailyAnalyticsPoint(
    val dateKey: String,
    val label: String,
    val total: Int,
    val completed: Int
)

data class WeeklyTrendPoint(
    val label: String,
    val total: Int,
    val completed: Int,
    val percent: Int
)

data class CategoryStat(
    val category: TaskCategory,
    val total: Int,
    val completed: Int
)

data class PlannerUiState(
    val isLoading: Boolean = true,
    val todayKey: String = DateTimeUtils.todayKey(),
    val preferences: UserPreferences = UserPreferences(),
    val quote: MotivationalQuote = QuoteProvider.quoteOfTheDay(0),
    val allTasks: List<TaskEntity> = emptyList(),
    val todayTasks: List<TaskEntity> = emptyList(),
    val totalTodayTasks: Int = 0,
    val completedTodayTasks: Int = 0,
    val pendingTodayTasks: Int = 0,
    val overduePendingTasks: List<TaskEntity> = emptyList(),
    val dailyProgressPercent: Int = 0,
    val allRoutines: List<RoutineBlockEntity> = emptyList(),
    val todayRoutines: List<RoutineBlockWithState> = emptyList(),
    val habitsWithProgress: List<HabitWithProgress> = emptyList(),
    val weekDateKeys: List<String> = DateTimeUtils.weekDateKeys(DateTimeUtils.todayKey()),
    val tasksByDateSummary: Map<String, DayTaskSummary> = emptyMap(),
    val routineCompletions: List<RoutineCompletionEntity> = emptyList(),
    // Analytics
    val last7DaysAnalytics: List<DailyAnalyticsPoint> = emptyList(),
    val monthlyWeeklyTrends: List<WeeklyTrendPoint> = emptyList(),
    val weeklyCompletedCount: Int = 0,
    val weeklyTotalCount: Int = 0,
    val monthlyCompletedCount: Int = 0,
    val monthlyTotalCount: Int = 0,
    val categoryStats: List<CategoryStat> = emptyList(),
    val totalCompletedAllTime: Int = 0,
    val topHabitStreak: Int = 0
)

data class TaskFilterState(
    val searchQuery: String = "",
    val categoryFilter: TaskCategory? = null,
    val priorityFilter: TaskPriority? = null,
    val statusFilter: TaskStatusFilter = TaskStatusFilter.PENDING
)

data class CalendarUiFilterState(
    val selectedDateKey: String = DateTimeUtils.todayKey(),
    val calendarMode: CalendarMode = CalendarMode.MONTHLY,
    val displayedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val displayedMonthZeroBased: Int = Calendar.getInstance().get(Calendar.MONTH)
)

class MainViewModel(
    private val repository: PlannerRepository
) : ViewModel() {

    private val _quoteOffset = MutableStateFlow(0)
    private val _taskFilter = MutableStateFlow(TaskFilterState())
    val taskFilter: StateFlow<TaskFilterState> = _taskFilter.asStateFlow()

    private val _calendarFilter = MutableStateFlow(CalendarUiFilterState())
    val calendarFilter: StateFlow<CalendarUiFilterState> = _calendarFilter.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
        }
    }

    private val baseDataFlow = combine(
        repository.tasksFlow,
        repository.routinesFlow,
        repository.routineCompletionsFlow,
        repository.habitsFlow,
        repository.habitLogsFlow
    ) { tasks, routines, routineCompletions, habits, habitLogs ->
        RawPlannerData(tasks, routines, routineCompletions, habits, habitLogs)
    }

    val uiState: StateFlow<PlannerUiState> = combine(
        baseDataFlow,
        repository.preferencesFlow,
        _quoteOffset
    ) { raw, prefs, quoteOffset ->
        buildPlannerUiState(raw, prefs, quoteOffset)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = PlannerUiState()
    )

    val filteredTasks: StateFlow<List<TaskEntity>> = combine(
        repository.tasksFlow,
        _taskFilter
    ) { tasks, filter ->
        tasks.filter { task ->
            val matchesSearch = filter.searchQuery.isBlank() ||
                task.title.contains(filter.searchQuery, ignoreCase = true) ||
                task.description.contains(filter.searchQuery, ignoreCase = true)
            val matchesCategory = filter.categoryFilter == null || task.category == filter.categoryFilter
            val matchesPriority = filter.priorityFilter == null || task.priority == filter.priorityFilter
            val matchesStatus = when (filter.statusFilter) {
                TaskStatusFilter.PENDING -> !task.isCompleted
                TaskStatusFilter.COMPLETED -> task.isCompleted
                TaskStatusFilter.ALL -> true
            }
            matchesSearch && matchesCategory && matchesPriority && matchesStatus
        }.sortedWith(
            compareBy<TaskEntity>(
                { it.isCompleted },
                { it.dateKey },
                { -it.priority.weight },
                { if (it.timeMinutes < 0) 9999 else it.timeMinutes }
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    private fun buildPlannerUiState(
        raw: RawPlannerData,
        prefs: UserPreferences,
        quoteOffset: Int
    ): PlannerUiState {
        val today = DateTimeUtils.todayKey()
        val nowMins = DateTimeUtils.currentTimeMinutes()
        val todayTasks = raw.tasks.filter { it.dateKey == today }
            .sortedBy { if (it.timeMinutes < 0) 9999 else it.timeMinutes }
        val completedToday = todayTasks.count { it.isCompleted }
        val pendingToday = todayTasks.count { !it.isCompleted }

        val overdue = raw.tasks.filter { task ->
            !task.isCompleted && (
                task.dateKey < today ||
                    (task.dateKey == today && task.timeMinutes in 0 until nowMins)
                )
        }

        val todayCompletionIds = raw.routineCompletions
            .filter { it.dateKey == today }
            .map { it.routineId }
            .toSet()

        val todayRoutines = raw.routines
            .filter { it.isEnabled && DateTimeUtils.isRoutineScheduledOnDate(it.daysOfWeekMask, today) }
            .map { block ->
                RoutineBlockWithState(
                    block = block,
                    isCompletedToday = todayCompletionIds.contains(block.id)
                )
            }

        val totalUnits = todayTasks.size + todayRoutines.size
        val completedUnits = completedToday + todayRoutines.count { it.isCompletedToday }
        val progressPercent = if (totalUnits == 0) 0 else ((completedUnits * 100f) / totalUnits).toInt().coerceIn(0, 100)

        val weekKeys = DateTimeUtils.weekDateKeys(today)
        val logsByHabit = raw.habitLogs.groupBy { it.habitId }
        val habitsWithProgress = raw.habits.map { habit ->
            val logs = logsByHabit[habit.id].orEmpty()
            val todayLog = logs.firstOrNull { it.dateKey == today }
            val completedDates = logs.filter { it.isCompleted }.map { it.dateKey }.toSet()
            val weekMap = weekKeys.associateWith { key -> completedDates.contains(key) }
            HabitWithProgress(
                habit = habit,
                todayCount = todayLog?.currentCount ?: 0,
                isCompletedToday = todayLog?.isCompleted == true,
                currentStreak = PlannerRepository.calculateCurrentStreak(completedDates, today),
                bestStreak = PlannerRepository.calculateBestStreak(completedDates),
                weekCompletionMap = weekMap
            )
        }

        val tasksByDateSummary = raw.tasks.groupBy { it.dateKey }.mapValues { (_, list) ->
            val done = list.count { it.isCompleted }
            DayTaskSummary(total = list.size, completed = done, pending = list.size - done)
        }

        // Analytics: Last 7 days
        val last7Keys = DateTimeUtils.lastNDaysKeys(7, today)
        val last7Analytics = last7Keys.map { key ->
            val dayTasks = raw.tasks.filter { it.dateKey == key }
            DailyAnalyticsPoint(
                dateKey = key,
                label = DateTimeUtils.shortDayName(key).take(2),
                total = dayTasks.size,
                completed = dayTasks.count { it.isCompleted }
            )
        }

        // Monthly 4-week breakdown (last 28 days in 4 buckets)
        val last28Keys = DateTimeUtils.lastNDaysKeys(28, today)
        val monthlyTasks = raw.tasks.filter { it.dateKey in last28Keys }
        val weeklyTrend = (0 until 4).map { weekIdx ->
            val slice = last28Keys.subList(weekIdx * 7, (weekIdx + 1) * 7)
            val weekTasks = raw.tasks.filter { it.dateKey in slice }
            val done = weekTasks.count { it.isCompleted }
            val total = weekTasks.size
            WeeklyTrendPoint(
                label = "W${weekIdx + 1}",
                total = total,
                completed = done,
                percent = if (total == 0) 0 else ((done * 100f) / total).toInt()
            )
        }

        val categoryStats = TaskCategory.entries.map { cat ->
            val catTasks = raw.tasks.filter { it.category == cat }
            CategoryStat(
                category = cat,
                total = catTasks.size,
                completed = catTasks.count { it.isCompleted }
            )
        }

        return PlannerUiState(
            isLoading = false,
            todayKey = today,
            preferences = prefs,
            quote = QuoteProvider.quoteOfTheDay(quoteOffset),
            allTasks = raw.tasks,
            todayTasks = todayTasks,
            totalTodayTasks = todayTasks.size,
            completedTodayTasks = completedToday,
            pendingTodayTasks = pendingToday,
            overduePendingTasks = overdue,
            dailyProgressPercent = progressPercent,
            allRoutines = raw.routines,
            todayRoutines = todayRoutines,
            habitsWithProgress = habitsWithProgress,
            weekDateKeys = weekKeys,
            tasksByDateSummary = tasksByDateSummary,
            routineCompletions = raw.routineCompletions,
            last7DaysAnalytics = last7Analytics,
            monthlyWeeklyTrends = weeklyTrend,
            weeklyCompletedCount = last7Analytics.sumOf { it.completed },
            weeklyTotalCount = last7Analytics.sumOf { it.total },
            monthlyCompletedCount = monthlyTasks.count { it.isCompleted },
            monthlyTotalCount = monthlyTasks.size,
            categoryStats = categoryStats,
            totalCompletedAllTime = raw.tasks.count { it.isCompleted },
            topHabitStreak = habitsWithProgress.maxOfOrNull { it.bestStreak } ?: 0
        )
    }

    // --- UI Actions ---
    fun nextQuote() {
        _quoteOffset.value = _quoteOffset.value + 1
    }

    fun updateTaskSearch(query: String) {
        _taskFilter.value = _taskFilter.value.copy(searchQuery = query)
    }

    fun updateCategoryFilter(category: TaskCategory?) {
        _taskFilter.value = _taskFilter.value.copy(
            categoryFilter = if (_taskFilter.value.categoryFilter == category) null else category
        )
    }

    fun updatePriorityFilter(priority: TaskPriority?) {
        _taskFilter.value = _taskFilter.value.copy(
            priorityFilter = if (_taskFilter.value.priorityFilter == priority) null else priority
        )
    }

    fun updateStatusFilter(status: TaskStatusFilter) {
        _taskFilter.value = _taskFilter.value.copy(statusFilter = status)
    }

    fun selectCalendarDate(dateKey: String) {
        val cal = DateTimeUtils.keyToCalendar(dateKey)
        _calendarFilter.value = _calendarFilter.value.copy(
            selectedDateKey = dateKey,
            displayedYear = cal.get(Calendar.YEAR),
            displayedMonthZeroBased = cal.get(Calendar.MONTH)
        )
    }

    fun setCalendarMode(mode: CalendarMode) {
        _calendarFilter.value = _calendarFilter.value.copy(calendarMode = mode)
    }

    fun shiftMonth(deltaMonths: Int) {
        val current = _calendarFilter.value
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, current.displayedYear)
            set(Calendar.MONTH, current.displayedMonthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, deltaMonths)
        }
        _calendarFilter.value = current.copy(
            displayedYear = cal.get(Calendar.YEAR),
            displayedMonthZeroBased = cal.get(Calendar.MONTH)
        )
    }

    // --- Mutations ---
    fun saveTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.saveTask(task)
            _snackbarMessage.value = if (task.id == 0L) "Task added" else "Task updated"
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task)
            if (!task.isCompleted && task.repeatRule != RepeatRule.NONE) {
                _snackbarMessage.value = "Completed! Next ${task.repeatRule.label.lowercase()} occurrence scheduled."
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            _snackbarMessage.value = "Task deleted"
        }
    }

    fun saveRoutine(routine: RoutineBlockEntity) {
        viewModelScope.launch {
            repository.saveRoutine(routine)
            _snackbarMessage.value = if (routine.id == 0L) "Routine block added" else "Routine block updated"
        }
    }

    fun deleteRoutine(routineId: Long) {
        viewModelScope.launch {
            repository.deleteRoutine(routineId)
            _snackbarMessage.value = "Routine block removed"
        }
    }

    fun toggleRoutineCompletion(routineId: Long, dateKey: String, currentlyCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleRoutineCompletion(routineId, dateKey, currentlyCompleted)
        }
    }

    fun saveHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.saveHabit(habit)
            _snackbarMessage.value = if (habit.id == 0L) "Habit created" else "Habit updated"
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
            _snackbarMessage.value = "Habit deleted"
        }
    }

    fun incrementHabit(habit: HabitEntity, dateKey: String = DateTimeUtils.todayKey()) {
        viewModelScope.launch { repository.incrementHabit(habit, dateKey) }
    }

    fun decrementHabit(habit: HabitEntity, dateKey: String = DateTimeUtils.todayKey()) {
        viewModelScope.launch { repository.decrementHabit(habit, dateKey) }
    }

    fun toggleHabitDay(habit: HabitEntity, dateKey: String) {
        viewModelScope.launch { repository.toggleHabitDay(habit, dateKey) }
    }

    fun updateProfile(name: String, goal: String, avatarUri: String? = null) {
        viewModelScope.launch {
            repository.updateProfile(name, goal, avatarUri)
            _snackbarMessage.value = "Profile saved"
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun updateReminderSettings(
        dailyEnabled: Boolean,
        morningMinutes: Int,
        eveningMinutes: Int,
        missedEnabled: Boolean,
        defaultLeadMinutes: Int
    ) {
        viewModelScope.launch {
            repository.updateReminderSettings(
                dailyEnabled, morningMinutes, eveningMinutes, missedEnabled, defaultLeadMinutes
            )
            _snackbarMessage.value = "Reminder settings updated"
        }
    }

    fun setShowQuotes(show: Boolean) {
        viewModelScope.launch { repository.setShowQuotes(show) }
    }

    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    fun restoreBackupJson(rawJson: String) {
        viewModelScope.launch {
            val result = repository.importBackupJson(rawJson)
            _snackbarMessage.value = result.fold(
                onSuccess = { it },
                onFailure = { "Restore failed: invalid backup file." }
            )
        }
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun clearMessage() {
        _snackbarMessage.value = null
    }

    private data class RawPlannerData(
        val tasks: List<TaskEntity>,
        val routines: List<RoutineBlockEntity>,
        val routineCompletions: List<RoutineCompletionEntity>,
        val habits: List<HabitEntity>,
        val habitLogs: List<HabitLogEntity>
    )

    class Factory(private val repository: PlannerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
