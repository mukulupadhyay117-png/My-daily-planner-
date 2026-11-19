package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.TaskCategory
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatusFilter
import com.example.ui.TaskFilterState
import com.example.ui.components.EmptyPlannerState
import com.example.ui.components.TaskItemCard
import com.example.ui.components.icon

@Composable
fun TasksScreen(
    tasks: List<TaskEntity>,
    filterState: TaskFilterState,
    onSearchChange: (String) -> Unit,
    onStatusChange: (TaskStatusFilter) -> Unit,
    onCategoryToggle: (TaskCategory?) -> Unit,
    onPriorityToggle: (TaskPriority?) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("tasks_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Task Manager",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search tasks by title or notes...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (filterState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_search_input")
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TaskStatusFilter.entries.forEachIndexed { idx, status ->
                    SegmentedButton(
                        selected = filterState.statusFilter == status,
                        onClick = { onStatusChange(status) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = idx,
                            count = TaskStatusFilter.entries.size
                        ),
                        modifier = Modifier.testTag("status_filter_${status.name}")
                    ) {
                        Text(status.label)
                    }
                }
            }

            // Category Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterState.categoryFilter == null,
                    onClick = { onCategoryToggle(null) },
                    label = { Text("All Categories") }
                )
                TaskCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = filterState.categoryFilter == cat,
                        onClick = { onCategoryToggle(cat) },
                        leadingIcon = {
                            Icon(cat.icon(), contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(cat.label) }
                    )
                }
            }

            // Priority Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterState.priorityFilter == null,
                    onClick = { onPriorityToggle(null) },
                    label = { Text("Any Priority") }
                )
                TaskPriority.entries.forEach { pri ->
                    FilterChip(
                        selected = filterState.priorityFilter == pri,
                        onClick = { onPriorityToggle(pri) },
                        label = { Text("${pri.label} Priority") }
                    )
                }
            }
        }

        if (tasks.isEmpty()) {
            EmptyPlannerState(
                title = when (filterState.statusFilter) {
                    TaskStatusFilter.PENDING -> "No pending tasks match"
                    TaskStatusFilter.COMPLETED -> "No completed tasks yet"
                    TaskStatusFilter.ALL -> "No tasks found"
                },
                subtitle = "Try clearing filters or tap + to add a new scheduled task."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskItemCard(
                        task = task,
                        onToggleComplete = { onToggleTask(task) },
                        onEdit = { onEditTask(task) },
                        onDelete = { onDeleteTask(task) }
                    )
                }
                item { Spacer(Modifier.height(76.dp)) }
            }
        }
    }
}
