package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.HabitEntity
import com.example.data.model.RepeatRule
import com.example.data.model.RoutineBlockEntity
import com.example.data.model.RoutineType
import com.example.data.model.TaskCategory
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.util.DateTimeUtils
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTaskSheet(
    existingTask: TaskEntity?,
    initialDateKey: String,
    defaultLeadMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var description by remember { mutableStateOf(existingTask?.description ?: "") }
    var dateKey by remember { mutableStateOf(existingTask?.dateKey ?: initialDateKey) }
    var timeMinutes by remember { mutableIntStateOf(existingTask?.timeMinutes ?: (9 * 60)) }
    var priority by remember { mutableStateOf(existingTask?.priority ?: TaskPriority.MEDIUM) }
    var category by remember { mutableStateOf(existingTask?.category ?: TaskCategory.PERSONAL) }
    var repeatRule by remember { mutableStateOf(existingTask?.repeatRule ?: RepeatRule.NONE) }
    var reminderMinutesBefore by remember {
        mutableIntStateOf(existingTask?.reminderMinutesBefore ?: defaultLeadMinutes)
    }
    var titleError by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (existingTask == null) "New Task" else "Edit Task",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError && it.isNotBlank()) titleError = false
                },
                label = { Text("Task title *") },
                placeholder = { Text("e.g., Complete calculus worksheet") },
                singleLine = true,
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Please enter a task title") }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notes / Description") },
                placeholder = { Text("Add details, links, or sub-steps...") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_desc_input")
            )

            // Date & Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("task_date_button"),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(DateTimeUtils.formatReadableDate(dateKey))
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("task_time_button"),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(DateTimeUtils.formatMinutes(timeMinutes))
                }
            }

            // Priority
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Priority", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.label) },
                            modifier = Modifier.testTag("priority_chip_${p.name}")
                        )
                    }
                }
            }

            // Category
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Category", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskCategory.entries.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            leadingIcon = {
                                Icon(c.icon(), contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(c.label) },
                            modifier = Modifier.testTag("category_chip_${c.name}")
                        )
                    }
                }
            }

            // Repeat
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Repeat Schedule", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepeatRule.entries.forEach { r ->
                        FilterChip(
                            selected = repeatRule == r,
                            onClick = { repeatRule = r },
                            label = { Text(r.label) }
                        )
                    }
                }
            }

            // Reminder lead time
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Reminder Notification", style = MaterialTheme.typography.titleSmall)
                }
                val options = listOf(-1 to "Off", 0 to "At time", 10 to "10m before", 15 to "15m before", 30 to "30m before", 60 to "1h before")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    options.forEach { (mins, label) ->
                        FilterChip(
                            selected = reminderMinutesBefore == mins,
                            onClick = { reminderMinutesBefore = mins },
                            label = { Text(label) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            titleError = true
                            return@Button
                        }
                        val updated = (existingTask ?: TaskEntity(title = "", dateKey = dateKey)).copy(
                            title = title.trim(),
                            description = description.trim(),
                            dateKey = dateKey,
                            timeMinutes = timeMinutes,
                            priority = priority,
                            category = category,
                            repeatRule = repeatRule,
                            reminderMinutesBefore = reminderMinutesBefore,
                            missedNotified = false
                        )
                        onSave(updated)
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("save_task_button")
                ) {
                    Text(if (existingTask == null) "Create Task" else "Save Changes")
                }
            }
        }
    }

    if (showDatePicker) {
        val initUtcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            val local = DateTimeUtils.keyToCalendar(dateKey)
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initUtcCal.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMillis ->
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = utcMillis
                        }
                        val localCal = Calendar.getInstance().apply {
                            set(
                                utcCal.get(Calendar.YEAR),
                                utcCal.get(Calendar.MONTH),
                                utcCal.get(Calendar.DAY_OF_MONTH),
                                0, 0, 0
                            )
                        }
                        dateKey = DateTimeUtils.calendarToKey(localCal)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val initHour = if (timeMinutes >= 0) timeMinutes / 60 else 9
        val initMin = if (timeMinutes >= 0) timeMinutes % 60 else 0
        val timePickerState = rememberTimePickerState(initialHour = initHour, initialMinute = initMin)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Task Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    timeMinutes = timePickerState.hour * 60 + timePickerState.minute
                    showTimePicker = false
                }) { Text("Set Time") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditRoutineDialog(
    existingRoutine: RoutineBlockEntity?,
    onDismiss: () -> Unit,
    onSave: (RoutineBlockEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(existingRoutine?.title ?: "") }
    var routineType by remember { mutableStateOf(existingRoutine?.routineType ?: RoutineType.CUSTOM) }
    var startMinutes by remember { mutableIntStateOf(existingRoutine?.startMinutes ?: (8 * 60)) }
    var endMinutes by remember { mutableIntStateOf(existingRoutine?.endMinutes ?: (9 * 60)) }
    var daysMask by remember { mutableIntStateOf(existingRoutine?.daysOfWeekMask ?: 127) }
    var reminderEnabled by remember { mutableStateOf(existingRoutine?.reminderEnabled ?: true) }
    var notes by remember { mutableStateOf(existingRoutine?.notes ?: "") }
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingRoutine == null) "Add Routine Block" else "Edit Routine Block") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("Block name *") },
                    placeholder = { Text("e.g., Morning Routine or Deep Study") },
                    singleLine = true,
                    isError = titleError,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Routine Type", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RoutineType.entries.forEach { type ->
                        FilterChip(
                            selected = routineType == type,
                            onClick = {
                                routineType = type
                                if (title.isBlank()) title = type.label
                            },
                            label = { Text(type.label, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickStart = true }, modifier = Modifier.weight(1f)) {
                        Text("Start: ${DateTimeUtils.formatMinutes(startMinutes)}", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(onClick = { pickEnd = true }, modifier = Modifier.weight(1f)) {
                        Text("End: ${DateTimeUtils.formatMinutes(endMinutes)}", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Text("Active Days", style = MaterialTheme.typography.labelLarge)
                val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dayLabels.forEachIndexed { idx, label ->
                        val selected = (daysMask and (1 shl idx)) != 0
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    daysMask = daysMask xor (1 shl idx)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Notify at start time", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }

                if (existingRoutine != null && onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete Routine Block", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isBlank()) {
                    titleError = true
                    return@Button
                }
                val finalEnd = if (endMinutes <= startMinutes) (startMinutes + 30).coerceAtMost(1439) else endMinutes
                onSave(
                    (existingRoutine ?: RoutineBlockEntity(title = "", startMinutes = startMinutes, endMinutes = finalEnd)).copy(
                        title = title.trim(),
                        routineType = routineType,
                        startMinutes = startMinutes,
                        endMinutes = finalEnd,
                        daysOfWeekMask = if (daysMask == 0) 127 else daysMask,
                        reminderEnabled = reminderEnabled,
                        notes = notes.trim()
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (pickStart || pickEnd) {
        val currentMins = if (pickStart) startMinutes else endMinutes
        val state = rememberTimePickerState(initialHour = currentMins / 60, initialMinute = currentMins % 60)
        AlertDialog(
            onDismissRequest = { pickStart = false; pickEnd = false },
            title = { Text(if (pickStart) "Routine Start Time" else "Routine End Time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val chosen = state.hour * 60 + state.minute
                    if (pickStart) {
                        startMinutes = chosen
                        if (endMinutes <= startMinutes) endMinutes = (startMinutes + 60).coerceAtMost(1439)
                    } else {
                        endMinutes = chosen
                    }
                    pickStart = false
                    pickEnd = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { pickStart = false; pickEnd = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditHabitDialog(
    existingHabit: HabitEntity?,
    onDismiss: () -> Unit,
    onSave: (HabitEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(existingHabit?.title ?: "") }
    var iconKey by remember { mutableStateOf(existingHabit?.iconKey ?: "star") }
    var targetCount by remember { mutableIntStateOf(existingHabit?.targetCount ?: 1) }
    var unitLabel by remember { mutableStateOf(existingHabit?.unitLabel ?: "time") }
    var reminderMinutes by remember { mutableIntStateOf(existingHabit?.reminderMinutes ?: -1) }
    var accentIndex by remember { mutableIntStateOf(existingHabit?.accentIndex ?: 0) }
    var showTimePicker by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }

    val iconChoices = listOf(
        "book" to "Reading",
        "study" to "Study",
        "exercise" to "Exercise",
        "water" to "Water",
        "meditation" to "Meditate",
        "sleep" to "Sleep",
        "star" to "Goal"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingHabit == null) "New Daily Habit" else "Edit Habit") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("Habit name *") },
                    placeholder = { Text("e.g., Reading books, Drinking water") },
                    singleLine = true,
                    isError = titleError,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Icon", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    iconChoices.forEachIndexed { idx, (key, label) ->
                        FilterChip(
                            selected = iconKey == key,
                            onClick = {
                                iconKey = key
                                accentIndex = idx
                            },
                            leadingIcon = {
                                Icon(habitIconForKey(key), contentDescription = label, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Daily Target: $targetCount", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { if (targetCount > 1) targetCount-- },
                            shape = CircleShape
                        ) { Text("-") }
                        OutlinedButton(
                            onClick = { if (targetCount < 20) targetCount++ },
                            shape = CircleShape
                        ) { Text("+") }
                    }
                }

                OutlinedTextField(
                    value = unitLabel,
                    onValueChange = { unitLabel = it },
                    label = { Text("Unit (e.g., glasses, pages, session)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Daily Habit Reminder", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (reminderMinutes >= 0) DateTimeUtils.formatMinutes(reminderMinutes) else "Off",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (reminderMinutes >= 0) {
                            TextButton(onClick = { reminderMinutes = -1 }) { Text("Off") }
                        }
                        OutlinedButton(onClick = { showTimePicker = true }) {
                            Text(if (reminderMinutes >= 0) "Change" else "Set Time")
                        }
                    }
                }

                if (existingHabit != null && onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete Habit", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isBlank()) {
                    titleError = true
                    return@Button
                }
                onSave(
                    (existingHabit ?: HabitEntity(title = "")).copy(
                        title = title.trim(),
                        iconKey = iconKey,
                        targetCount = targetCount,
                        unitLabel = unitLabel.trim().ifEmpty { "time" },
                        reminderMinutes = reminderMinutes,
                        accentIndex = accentIndex
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showTimePicker) {
        val initMins = if (reminderMinutes >= 0) reminderMinutes else 9 * 60
        val state = rememberTimePickerState(initialHour = initMins / 60, initialMinute = initMins % 60)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Habit Reminder Time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    reminderMinutes = state.hour * 60 + state.minute
                    showTimePicker = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTimePickerDialog(
    title: String,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = (initialMinutes / 60).coerceIn(0, 23),
        initialMinute = (initialMinutes % 60).coerceIn(0, 59)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
