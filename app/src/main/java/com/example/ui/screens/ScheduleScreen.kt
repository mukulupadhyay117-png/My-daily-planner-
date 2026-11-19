package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CalendarMode
import com.example.data.model.RoutineBlockEntity
import com.example.data.model.TaskEntity
import com.example.ui.CalendarUiFilterState
import com.example.ui.DayTaskSummary
import com.example.ui.PlannerUiState
import com.example.ui.components.EmptyPlannerState
import com.example.ui.components.TaskItemCard
import com.example.ui.components.accentColor
import com.example.ui.components.icon
import com.example.util.DateTimeUtils

@Composable
fun ScheduleScreen(
    uiState: PlannerUiState,
    calendarFilter: CalendarUiFilterState,
    activeSubTab: Int, // 0 = Calendar, 1 = Daily Routine
    onSubTabChange: (Int) -> Unit,
    onSelectDate: (String) -> Unit,
    onCalendarModeChange: (CalendarMode) -> Unit,
    onShiftMonth: (Int) -> Unit,
    onAddTaskForDate: (String) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onAddRoutine: () -> Unit,
    onEditRoutine: (RoutineBlockEntity) -> Unit,
    onDeleteRoutine: (Long) -> Unit,
    onToggleRoutine: (Long, String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("schedule_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (activeSubTab == 0) "Calendar" else "Daily Routine Planner",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = activeSubTab == 0,
                    onClick = { onSubTabChange(0) },
                    icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    modifier = Modifier.testTag("subtab_calendar")
                ) {
                    Text("Calendar")
                }
                SegmentedButton(
                    selected = activeSubTab == 1,
                    onClick = { onSubTabChange(1) },
                    icon = { Icon(Icons.Rounded.ViewAgenda, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.testTag("subtab_routine")
                ) {
                    Text("Daily Routine")
                }
            }
        }

        if (activeSubTab == 0) {
            CalendarTabContent(
                uiState = uiState,
                calendarFilter = calendarFilter,
                onSelectDate = onSelectDate,
                onCalendarModeChange = onCalendarModeChange,
                onShiftMonth = onShiftMonth,
                onAddTaskForDate = onAddTaskForDate,
                onEditTask = onEditTask,
                onToggleTask = onToggleTask,
                onDeleteTask = onDeleteTask
            )
        } else {
            RoutineTabContent(
                uiState = uiState,
                onAddRoutine = onAddRoutine,
                onEditRoutine = onEditRoutine,
                onDeleteRoutine = onDeleteRoutine,
                onToggleRoutine = onToggleRoutine
            )
        }
    }
}

@Composable
private fun CalendarTabContent(
    uiState: PlannerUiState,
    calendarFilter: CalendarUiFilterState,
    onSelectDate: (String) -> Unit,
    onCalendarModeChange: (CalendarMode) -> Unit,
    onShiftMonth: (Int) -> Unit,
    onAddTaskForDate: (String) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit
) {
    val selectedDate = calendarFilter.selectedDateKey
    val selectedDayTasks = uiState.allTasks
        .filter { it.dateKey == selectedDate }
        .sortedBy { if (it.timeMinutes < 0) 9999 else it.timeMinutes }

    val dayCompletedIds = uiState.routineCompletions
        .filter { it.dateKey == selectedDate }
        .map { it.routineId }
        .toSet()

    val selectedDayRoutines = uiState.allRoutines.filter {
        it.isEnabled && DateTimeUtils.isRoutineScheduledOnDate(it.daysOfWeekMask, selectedDate)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Daily / Weekly / Monthly switcher
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                CalendarMode.entries.forEachIndexed { idx, mode ->
                    SegmentedButton(
                        selected = calendarFilter.calendarMode == mode,
                        onClick = { onCalendarModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(idx, CalendarMode.entries.size),
                        modifier = Modifier.testTag("calendar_mode_${mode.name}")
                    ) {
                        Text(mode.label)
                    }
                }
            }
        }

        // Calendar Grid / Week Strip / Day Navigator
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    when (calendarFilter.calendarMode) {
                        CalendarMode.MONTHLY -> {
                            MonthGridView(
                                year = calendarFilter.displayedYear,
                                monthZeroBased = calendarFilter.displayedMonthZeroBased,
                                selectedDateKey = selectedDate,
                                todayKey = uiState.todayKey,
                                summaryMap = uiState.tasksByDateSummary,
                                onSelectDate = onSelectDate,
                                onShiftMonth = onShiftMonth
                            )
                        }

                        CalendarMode.WEEKLY -> {
                            WeekStripView(
                                anchorDateKey = selectedDate,
                                todayKey = uiState.todayKey,
                                summaryMap = uiState.tasksByDateSummary,
                                onSelectDate = onSelectDate
                            )
                        }

                        CalendarMode.DAILY -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { onSelectDate(DateTimeUtils.addDays(selectedDate, -1)) }) {
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous day")
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = DateTimeUtils.formatReadableDate(selectedDate),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = DateTimeUtils.formatFullDate(selectedDate),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onSelectDate(DateTimeUtils.addDays(selectedDate, 1)) }) {
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next day")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Date Action Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = DateTimeUtils.formatReadableDate(selectedDate),
                        style = MaterialTheme.typography.titleLarge
                    )
                    val doneCount = selectedDayTasks.count { it.isCompleted }
                    val pendingCount = selectedDayTasks.size - doneCount
                    Text(
                        text = "$pendingCount pending • $doneCount completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = { onAddTaskForDate(selectedDate) },
                    modifier = Modifier.testTag("calendar_add_task_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Task")
                }
            }
        }

        if (selectedDayTasks.isEmpty()) {
            item {
                EmptyPlannerState(
                    title = "Clear schedule on ${DateTimeUtils.formatReadableDate(selectedDate)}",
                    subtitle = "Tap + Add Task above to schedule a task directly on this day."
                )
            }
        } else {
            items(selectedDayTasks, key = { "cal_task_${it.id}" }) { task ->
                TaskItemCard(
                    task = task,
                    onToggleComplete = { onToggleTask(task) },
                    onEdit = { onEditTask(task) },
                    onDelete = { onDeleteTask(task) }
                )
            }
        }

        if (selectedDayRoutines.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Routine Blocks (${selectedDayRoutines.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(selectedDayRoutines, key = { "cal_routine_${it.id}" }) { block ->
                val done = dayCompletedIds.contains(block.id)
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = block.routineType.icon(),
                            contentDescription = null,
                            tint = block.routineType.accentColor(),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = block.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${DateTimeUtils.formatMinutes(block.startMinutes)} – ${DateTimeUtils.formatMinutes(block.endMinutes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(76.dp)) }
    }
}

@Composable
private fun MonthGridView(
    year: Int,
    monthZeroBased: Int,
    selectedDateKey: String,
    todayKey: String,
    summaryMap: Map<String, DayTaskSummary>,
    onSelectDate: (String) -> Unit,
    onShiftMonth: (Int) -> Unit
) {
    val cells = DateTimeUtils.buildMonthGrid(year, monthZeroBased)
    val dayHeaders = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onShiftMonth(-1) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Text(
                text = DateTimeUtils.formatMonthYear(year, monthZeroBased),
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = { onShiftMonth(1) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            dayHeaders.forEach { d ->
                Text(
                    text = d,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { cell ->
                    val isSelected = cell.dateKey == selectedDateKey
                    val isToday = cell.dateKey == todayKey
                    val summary = summaryMap[cell.dateKey]

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    else -> Color.Transparent
                                }
                            )
                            .clickable { onSelectDate(cell.dateKey) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = cell.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    !cell.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                            if (summary != null && summary.total > 0) {
                                Spacer(Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    if (summary.pending > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.primary
                                                )
                                        )
                                    }
                                    if (summary.completed > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                                    else MaterialTheme.colorScheme.secondary
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekStripView(
    anchorDateKey: String,
    todayKey: String,
    summaryMap: Map<String, DayTaskSummary>,
    onSelectDate: (String) -> Unit
) {
    val weekKeys = DateTimeUtils.weekDateKeys(anchorDateKey)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onSelectDate(DateTimeUtils.addDays(anchorDateKey, -7)) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous week")
            }
            Text(
                text = "${DateTimeUtils.formatReadableDate(weekKeys.first())} – ${DateTimeUtils.formatReadableDate(weekKeys.last())}",
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = { onSelectDate(DateTimeUtils.addDays(anchorDateKey, 7)) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next week")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            weekKeys.forEach { key ->
                val selected = key == anchorDateKey
                val isToday = key == todayKey
                val summary = summaryMap[key]
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                selected -> MaterialTheme.colorScheme.primary
                                isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                        .clickable { onSelectDate(key) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = DateTimeUtils.shortDayName(key).take(2),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = DateTimeUtils.dayNumber(key).toString(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (summary != null && summary.total > 0) "${summary.completed}/${summary.total}" else "—",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineTabContent(
    uiState: PlannerUiState,
    onAddRoutine: () -> Unit,
    onEditRoutine: (RoutineBlockEntity) -> Unit,
    onDeleteRoutine: (Long) -> Unit,
    onToggleRoutine: (Long, String, Boolean) -> Unit
) {
    val today = uiState.todayKey
    val completedSet = uiState.routineCompletions
        .filter { it.dateKey == today }
        .map { it.routineId }
        .toSet()

    val scheduledToday = uiState.allRoutines.filter {
        it.isEnabled && DateTimeUtils.isRoutineScheduledOnDate(it.daysOfWeekMask, today)
    }
    val completedToday = scheduledToday.count { completedSet.contains(it.id) }
    val routineProgress = if (scheduledToday.isEmpty()) 0f else completedToday.toFloat() / scheduledToday.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Today's Routine Flow", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "$completedToday of ${scheduledToday.size} blocks checked off",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = onAddRoutine,
                            modifier = Modifier.testTag("add_routine_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Custom Block")
                        }
                    }
                    LinearProgressIndicator(
                        progress = { routineProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }

        if (uiState.allRoutines.isEmpty()) {
            item {
                EmptyPlannerState(
                    title = "No routine blocks yet",
                    subtitle = "Create morning, study, exercise, break, or evening blocks to structure your day."
                )
            }
        } else {
            items(uiState.allRoutines, key = { it.id }) { block ->
                val isCompletedToday = completedSet.contains(block.id)
                val accent = block.routineType.accentColor()
                val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditRoutine(block) },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCompletedToday) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = block.routineType.icon(),
                                    contentDescription = block.routineType.label,
                                    tint = accent
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = block.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        textDecoration = if (isCompletedToday) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${DateTimeUtils.formatMinutes(block.startMinutes)} – ${DateTimeUtils.formatMinutes(block.endMinutes)} (${DateTimeUtils.formatDurationMinutes(block.startMinutes, block.endMinutes)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCompletedToday) MaterialTheme.colorScheme.secondary
                                        else Color.Transparent
                                    )
                                    .border(
                                        2.dp,
                                        if (isCompletedToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                        CircleShape
                                    )
                                    .clickable {
                                        onToggleRoutine(block.id, today, isCompletedToday)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCompletedToday) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = "Completed",
                                        tint = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (block.notes.isNotBlank()) {
                            Text(
                                text = block.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                dayLetters.forEachIndexed { idx, letter ->
                                    val active = (block.daysOfWeekMask and (1 shl idx)) != 0
                                    Surface(
                                        color = if (active) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = letter,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (block.reminderEnabled) {
                                    Icon(
                                        Icons.Rounded.Alarm,
                                        contentDescription = "Reminder active",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(onClick = { onEditRoutine(block) }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit routine", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onDeleteRoutine(block.id) }) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete routine",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(76.dp)) }
    }
}
