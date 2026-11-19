package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.HabitEntity
import com.example.data.model.RoutineBlockEntity
import com.example.data.model.TaskEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AddEditHabitDialog
import com.example.ui.components.AddEditRoutineDialog
import com.example.ui.components.AddEditTaskSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HabitsScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.SettingsAndProfileScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.DailyPlannerTheme
import com.example.util.DateTimeUtils

object PlannerRoutes {
    const val HOME = "home"
    const val TASKS = "tasks"
    const val SCHEDULE = "schedule"
    const val HABITS = "habits"
    const val ANALYTICS = "analytics"
    const val SETTINGS = "settings"
}

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val plannerTabs = listOf(
    NavTab(PlannerRoutes.HOME, "Today", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    NavTab(PlannerRoutes.TASKS, "Tasks", Icons.Rounded.CheckCircle, Icons.Outlined.CheckCircle, "nav_tasks"),
    NavTab(PlannerRoutes.SCHEDULE, "Schedule", Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_schedule"),
    NavTab(PlannerRoutes.HABITS, "Habits", Icons.Rounded.SelfImprovement, Icons.Outlined.SelfImprovement, "nav_habits"),
    NavTab(PlannerRoutes.ANALYTICS, "Analytics", Icons.Rounded.Analytics, Icons.Outlined.Analytics, "nav_analytics")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as PlannerApplication
        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.Factory(app.repository)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            DailyPlannerTheme(themeMode = uiState.preferences.themeMode) {
                DailyPlannerApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DailyPlannerApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredTasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val taskFilter by viewModel.taskFilter.collectAsStateWithLifecycle()
    val calendarFilter by viewModel.calendarFilter.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: PlannerRoutes.HOME

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    // Sub-tab inside ScheduleScreen (0 = Calendar, 1 = Daily Routine)
    var scheduleSubTab by remember { mutableIntStateOf(0) }

    // Modal editors
    var showTaskSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var taskInitialDateKey by remember { mutableStateOf(DateTimeUtils.todayKey()) }

    var showRoutineDialog by remember { mutableStateOf(false) }
    var editingRoutine by remember { mutableStateOf<RoutineBlockEntity?>(null) }

    var showHabitDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<HabitEntity?>(null) }

    fun openNewTask(dateKey: String = DateTimeUtils.todayKey()) {
        editingTask = null
        taskInitialDateKey = dateKey
        showTaskSheet = true
    }

    fun openEditTask(task: TaskEntity) {
        editingTask = task
        taskInitialDateKey = task.dateKey
        showTaskSheet = true
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val isSettingsRoute = currentRoute == PlannerRoutes.SETTINGS

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (currentRoute == PlannerRoutes.HOME || currentRoute == PlannerRoutes.TASKS) {
                    FloatingActionButton(
                        onClick = { openNewTask(DateTimeUtils.todayKey()) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_task")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add new task")
                    }
                }
            },
            bottomBar = {
                if (!useRail && !isSettingsRoute) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        plannerTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && !isSettingsRoute) {
                    NavigationRail(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        plannerTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        NavigationRailItem(
                            selected = false,
                            onClick = { navController.navigate(PlannerRoutes.SETTINGS) },
                            icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = PlannerRoutes.HOME,
                        enterTransition = { fadeIn() },
                        exitTransition = { fadeOut() }
                    ) {
                        composable(PlannerRoutes.HOME) {
                            DashboardScreen(
                                uiState = uiState,
                                onAddTaskClick = { openNewTask(uiState.todayKey) },
                                onEditTaskClick = { openEditTask(it) },
                                onToggleTask = { viewModel.toggleTaskCompletion(it) },
                                onDeleteTask = { viewModel.deleteTask(it) },
                                onToggleRoutine = { id, key, done ->
                                    viewModel.toggleRoutineCompletion(id, key, done)
                                },
                                onEditRoutineClick = {
                                    editingRoutine = it
                                    showRoutineDialog = true
                                },
                                onQuickIncrementHabit = { habit ->
                                    if (habit.targetCount > 1) {
                                        viewModel.incrementHabit(habit)
                                    } else {
                                        viewModel.toggleHabitDay(habit, uiState.todayKey)
                                    }
                                },
                                onNextQuote = { viewModel.nextQuote() },
                                onNavigateToTasks = { navigateToTab(PlannerRoutes.TASKS) },
                                onNavigateToRoutine = {
                                    scheduleSubTab = 1
                                    navigateToTab(PlannerRoutes.SCHEDULE)
                                },
                                onNavigateToHabits = { navigateToTab(PlannerRoutes.HABITS) },
                                onNavigateToAnalytics = { navigateToTab(PlannerRoutes.ANALYTICS) },
                                onNavigateToSettings = { navController.navigate(PlannerRoutes.SETTINGS) }
                            )
                        }

                        composable(PlannerRoutes.TASKS) {
                            TasksScreen(
                                tasks = filteredTasks,
                                filterState = taskFilter,
                                onSearchChange = viewModel::updateTaskSearch,
                                onStatusChange = viewModel::updateStatusFilter,
                                onCategoryToggle = viewModel::updateCategoryFilter,
                                onPriorityToggle = viewModel::updatePriorityFilter,
                                onToggleTask = viewModel::toggleTaskCompletion,
                                onEditTask = { openEditTask(it) },
                                onDeleteTask = viewModel::deleteTask
                            )
                        }

                        composable(PlannerRoutes.SCHEDULE) {
                            ScheduleScreen(
                                uiState = uiState,
                                calendarFilter = calendarFilter,
                                activeSubTab = scheduleSubTab,
                                onSubTabChange = { scheduleSubTab = it },
                                onSelectDate = viewModel::selectCalendarDate,
                                onCalendarModeChange = viewModel::setCalendarMode,
                                onShiftMonth = viewModel::shiftMonth,
                                onAddTaskForDate = { openNewTask(it) },
                                onEditTask = { openEditTask(it) },
                                onToggleTask = viewModel::toggleTaskCompletion,
                                onDeleteTask = viewModel::deleteTask,
                                onAddRoutine = {
                                    editingRoutine = null
                                    showRoutineDialog = true
                                },
                                onEditRoutine = {
                                    editingRoutine = it
                                    showRoutineDialog = true
                                },
                                onDeleteRoutine = viewModel::deleteRoutine,
                                onToggleRoutine = viewModel::toggleRoutineCompletion
                            )
                        }

                        composable(PlannerRoutes.HABITS) {
                            HabitsScreen(
                                uiState = uiState,
                                onAddHabit = {
                                    editingHabit = null
                                    showHabitDialog = true
                                },
                                onEditHabit = {
                                    editingHabit = it
                                    showHabitDialog = true
                                },
                                onIncrementHabit = { viewModel.incrementHabit(it) },
                                onDecrementHabit = { viewModel.decrementHabit(it) },
                                onToggleHabitDay = viewModel::toggleHabitDay
                            )
                        }

                        composable(PlannerRoutes.ANALYTICS) {
                            AnalyticsScreen(uiState = uiState)
                        }

                        composable(PlannerRoutes.SETTINGS) {
                            SettingsAndProfileScreen(
                                uiState = uiState,
                                onBack = { navController.popBackStack() },
                                onSaveProfile = viewModel::updateProfile,
                                onSetThemeMode = viewModel::setThemeMode,
                                onUpdateReminderSettings = viewModel::updateReminderSettings,
                                onSetShowQuotes = viewModel::setShowQuotes,
                                onExportBackupJson = viewModel::exportBackupJson,
                                onRestoreBackupJson = viewModel::restoreBackupJson,
                                onShowMessage = viewModel::showMessage
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTaskSheet) {
        AddEditTaskSheet(
            existingTask = editingTask,
            initialDateKey = taskInitialDateKey,
            defaultLeadMinutes = uiState.preferences.defaultLeadMinutes,
            onDismiss = { showTaskSheet = false },
            onSave = { task ->
                viewModel.saveTask(task)
                showTaskSheet = false
            }
        )
    }

    if (showRoutineDialog) {
        AddEditRoutineDialog(
            existingRoutine = editingRoutine,
            onDismiss = { showRoutineDialog = false },
            onSave = { routine ->
                viewModel.saveRoutine(routine)
                showRoutineDialog = false
            },
            onDelete = editingRoutine?.let { existing ->
                {
                    viewModel.deleteRoutine(existing.id)
                    showRoutineDialog = false
                }
            }
        )
    }

    if (showHabitDialog) {
        AddEditHabitDialog(
            existingHabit = editingHabit,
            onDismiss = { showHabitDialog = false },
            onSave = { habit ->
                viewModel.saveHabit(habit)
                showHabitDialog = false
            },
            onDelete = editingHabit?.let { existing ->
                {
                    viewModel.deleteHabit(existing.id)
                    showHabitDialog = false
                }
            }
        )
    }
}
