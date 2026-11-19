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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.HabitEntity
import com.example.ui.PlannerUiState
import com.example.ui.components.EmptyPlannerState
import com.example.ui.components.habitAccentForIndex
import com.example.ui.components.habitIconForKey
import com.example.util.DateTimeUtils

@Composable
fun HabitsScreen(
    uiState: PlannerUiState,
    onAddHabit: () -> Unit,
    onEditHabit: (HabitEntity) -> Unit,
    onIncrementHabit: (HabitEntity) -> Unit,
    onDecrementHabit: (HabitEntity) -> Unit,
    onToggleHabitDay: (HabitEntity, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val habits = uiState.habitsWithProgress
    val totalWeeklyPossible = habits.size * 7
    val totalWeeklyDone = habits.sumOf { h -> h.weekCompletionMap.values.count { it } }
    val weeklyPercent = if (totalWeeklyPossible == 0) 0 else (totalWeeklyDone * 100) / totalWeeklyPossible

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("habits_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Habit Tracker", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "Build lasting daily consistency",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = onAddHabit,
                    modifier = Modifier.testTag("add_habit_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Habit")
                }
            }
        }

        // Weekly Consistency Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("This Week's Consistency", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$totalWeeklyDone of $totalWeeklyPossible daily habit goals hit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { weeklyPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$weeklyPercent%",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Weekly",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (habits.isEmpty()) {
            item {
                EmptyPlannerState(
                    title = "No habits tracked yet",
                    subtitle = "Tap + New Habit to start tracking reading, studying, water, or exercise."
                )
            }
        } else {
            items(habits, key = { it.habit.id }) { item ->
                val accent = habitAccentForIndex(item.habit.accentIndex)
                val progressFraction = (item.todayCount.toFloat() / item.habit.targetCount.coerceAtLeast(1)).coerceIn(0f, 1f)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditHabit(item.habit) },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        if (item.isCompletedToday) accent.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = habitIconForKey(item.habit.iconKey),
                                    contentDescription = item.habit.title,
                                    tint = accent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.habit.title, style = MaterialTheme.typography.titleMedium)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.LocalFireDepartment,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "${item.currentStreak}d streak (Best ${item.bestStreak}d)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                    if (item.habit.reminderMinutes >= 0) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Alarm,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = DateTimeUtils.formatMinutes(item.habit.reminderMinutes),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            if (item.habit.targetCount > 1) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilledTonalIconButton(
                                        onClick = { onDecrementHabit(item.habit) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Rounded.Remove, contentDescription = "Decrease count", modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = "${item.todayCount}/${item.habit.targetCount}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    FilledTonalIconButton(
                                        onClick = { onIncrementHabit(item.habit) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = "Increase count", modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (item.isCompletedToday) accent else Color.Transparent)
                                        .border(
                                            width = 2.dp,
                                            color = if (item.isCompletedToday) accent else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            onToggleHabitDay(item.habit, uiState.todayKey)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item.isCompletedToday) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = "Habit checked in",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (item.habit.targetCount > 1) {
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                color = accent,
                                trackColor = accent.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(CircleShape)
                            )
                        }

                        // 7-day Weekly Matrix (Mon..Sun)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            uiState.weekDateKeys.forEach { dayKey ->
                                val dayDone = item.weekCompletionMap[dayKey] == true
                                val isToday = dayKey == uiState.todayKey
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onToggleHabitDay(item.habit, dayKey) }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = DateTimeUtils.shortDayName(dayKey).take(2),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isToday) accent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (dayDone) accent
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .then(
                                                if (isToday && !dayDone) {
                                                    Modifier.border(1.5.dp, accent, CircleShape)
                                                } else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (dayDone) {
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = "Completed on $dayKey",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
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

        item { Spacer(Modifier.height(76.dp)) }
    }
}
