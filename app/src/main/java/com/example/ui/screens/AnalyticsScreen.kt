package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.DailyAnalyticsPoint
import com.example.ui.PlannerUiState
import com.example.ui.WeeklyTrendPoint
import com.example.ui.components.AnimatedProgressRing
import com.example.ui.components.accentColor
import com.example.ui.components.habitAccentForIndex
import com.example.ui.components.habitIconForKey

@Composable
fun AnalyticsScreen(
    uiState: PlannerUiState,
    modifier: Modifier = Modifier
) {
    val weeklyRate = if (uiState.weeklyTotalCount == 0) 0 else {
        (uiState.weeklyCompletedCount * 100) / uiState.weeklyTotalCount
    }
    val monthlyRate = if (uiState.monthlyTotalCount == 0) 0 else {
        (uiState.monthlyCompletedCount * 100) / uiState.monthlyTotalCount
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Progress & Analytics", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Daily productivity, weekly reports & habit streaks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Top KPI Row: Daily Ring + All-Time & Streak Summary
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedProgressRing(
                        percentage = uiState.dailyProgressPercent,
                        size = 96.dp,
                        strokeWidth = 10.dp
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Today's Productivity", style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            KpiBox(
                                label = "Weekly",
                                value = "$weeklyRate%",
                                sub = "${uiState.weeklyCompletedCount}/${uiState.weeklyTotalCount}",
                                modifier = Modifier.weight(1f)
                            )
                            KpiBox(
                                label = "Monthly",
                                value = "$monthlyRate%",
                                sub = "${uiState.monthlyCompletedCount}/${uiState.monthlyTotalCount}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = "All-time completed tasks: ${uiState.totalCompletedAllTime} • Top streak: ${uiState.topHabitStreak}d",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Weekly Completion Bar Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("7-Day Task Completion", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${uiState.weeklyCompletedCount} done",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    WeeklyBarChart(
                        points = uiState.last7DaysAnalytics,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Monthly 4-Week Productivity Trend Line Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Monthly 4-Week Trend", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "$monthlyRate% avg",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    MonthlyTrendChart(
                        points = uiState.monthlyWeeklyTrends,
                        lineColor = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Category Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Tasks by Category", style = MaterialTheme.typography.titleLarge)
                    uiState.categoryStats.forEach { stat ->
                        val catColor = stat.category.accentColor()
                        val fraction = if (stat.total == 0) 0f else stat.completed.toFloat() / stat.total
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stat.category.label, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${stat.completed}/${stat.total}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LinearProgressIndicator(
                                progress = { fraction },
                                color = catColor,
                                trackColor = catColor.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }
            }
        }

        // Habit Streak Summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Habit Streaks", style = MaterialTheme.typography.titleLarge)
                    if (uiState.habitsWithProgress.isEmpty()) {
                        Text(
                            text = "Add habits in the Habits tab to see streak analytics.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.habitsWithProgress.forEach { h ->
                            val accent = habitAccentForIndex(h.habit.accentIndex)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        habitIconForKey(h.habit.iconKey),
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(h.habit.title, style = MaterialTheme.typography.bodyMedium)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${h.currentStreak}d (Best ${h.bestStreak}d)",
                                        style = MaterialTheme.typography.labelSmall
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

@Composable
private fun KpiBox(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeeklyBarChart(
    points: List<DailyAnalyticsPoint>,
    primaryColor: Color,
    trackColor: Color
) {
    val maxTotal = (points.maxOfOrNull { it.total } ?: 1).coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            if (points.isEmpty()) return@Canvas
            val slotWidth = size.width / points.size
            val barWidth = (slotWidth * 0.48f).coerceAtMost(36.dp.toPx())
            val maxBarHeight = size.height - 8.dp.toPx()

            points.forEachIndexed { index, pt ->
                val x = index * slotWidth + (slotWidth - barWidth) / 2f
                val totalHeight = (pt.total.toFloat() / maxTotal) * maxBarHeight
                val completedHeight = (pt.completed.toFloat() / maxTotal) * maxBarHeight

                // Track / Total outline bar
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(x, size.height - totalHeight.coerceAtLeast(10.dp.toPx())),
                    size = Size(barWidth, totalHeight.coerceAtLeast(10.dp.toPx())),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )

                if (completedHeight > 0f) {
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(x, size.height - completedHeight),
                        size = Size(barWidth, completedHeight),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEach { pt ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(pt.label, style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${pt.completed}/${pt.total}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyTrendChart(
    points: List<WeeklyTrendPoint>,
    lineColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            if (points.size < 2) return@Canvas
            val padX = 20.dp.toPx()
            val padY = 14.dp.toPx()
            val usableW = size.width - padX * 2
            val usableH = size.height - padY * 2
            val stepX = usableW / (points.size - 1)

            val coords = points.mapIndexed { idx, pt ->
                val x = padX + idx * stepX
                val y = padY + usableH * (1f - (pt.percent / 100f))
                Offset(x, y)
            }

            val areaPath = Path().apply {
                moveTo(coords.first().x, size.height - padY)
                coords.forEach { lineTo(it.x, it.y) }
                lineTo(coords.last().x, size.height - padY)
                close()
            }
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f))
                )
            )

            val linePath = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 1 until coords.size) {
                    lineTo(coords[i].x, coords[i].y)
                }
            }
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            coords.forEach { c ->
                drawCircle(color = lineColor, radius = 5.dp.toPx(), center = c)
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEach { pt ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(pt.label, style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${pt.percent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
