package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.RoutineType
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.CategoryHealthColor
import com.example.ui.theme.CategoryOtherColor
import com.example.ui.theme.CategoryPersonalColor
import com.example.ui.theme.CategoryStudyColor
import com.example.ui.theme.CategoryWorkColor
import com.example.ui.theme.PriorityHighColor
import com.example.ui.theme.PriorityLowColor
import com.example.ui.theme.PriorityMediumColor
import com.example.ui.theme.SageSecondary
import com.example.ui.theme.TerracottaPrimary

fun TaskPriority.accentColor(): Color = when (this) {
    TaskPriority.HIGH -> PriorityHighColor
    TaskPriority.MEDIUM -> PriorityMediumColor
    TaskPriority.LOW -> PriorityLowColor
}

fun TaskCategory.accentColor(): Color = when (this) {
    TaskCategory.STUDY -> CategoryStudyColor
    TaskCategory.PERSONAL -> CategoryPersonalColor
    TaskCategory.WORK -> CategoryWorkColor
    TaskCategory.HEALTH -> CategoryHealthColor
    TaskCategory.OTHER -> CategoryOtherColor
}

fun TaskCategory.icon(): ImageVector = when (this) {
    TaskCategory.STUDY -> Icons.Rounded.School
    TaskCategory.PERSONAL -> Icons.Rounded.Home
    TaskCategory.WORK -> Icons.Rounded.Work
    TaskCategory.HEALTH -> Icons.Rounded.Favorite
    TaskCategory.OTHER -> Icons.Rounded.Label
}

fun RoutineType.icon(): ImageVector = when (this) {
    RoutineType.MORNING -> Icons.Rounded.WbSunny
    RoutineType.STUDY -> Icons.Rounded.MenuBook
    RoutineType.EXERCISE -> Icons.Rounded.FitnessCenter
    RoutineType.WORK_SCHOOL -> Icons.Rounded.Work
    RoutineType.BREAK -> Icons.Rounded.Coffee
    RoutineType.EVENING -> Icons.Rounded.NightsStay
    RoutineType.NIGHT -> Icons.Rounded.Bedtime
    RoutineType.CUSTOM -> Icons.Rounded.Schedule
}

fun RoutineType.accentColor(): Color = when (this) {
    RoutineType.MORNING -> AmberTertiary
    RoutineType.STUDY -> CategoryStudyColor
    RoutineType.EXERCISE -> SageSecondary
    RoutineType.WORK_SCHOOL -> CategoryWorkColor
    RoutineType.BREAK -> TerracottaPrimary
    RoutineType.EVENING -> Color(0xFF5D62B5)
    RoutineType.NIGHT -> Color(0xFF3F51B5)
    RoutineType.CUSTOM -> CategoryOtherColor
}

fun habitIconForKey(iconKey: String): ImageVector = when (iconKey) {
    "book" -> Icons.Rounded.AutoStories
    "study" -> Icons.Rounded.School
    "exercise" -> Icons.Rounded.DirectionsRun
    "water" -> Icons.Rounded.WaterDrop
    "meditation" -> Icons.Rounded.SelfImprovement
    "sleep" -> Icons.Rounded.Bedtime
    else -> Icons.Rounded.Star
}

fun habitAccentForIndex(index: Int): Color = when (Math.floorMod(index, 5)) {
    0 -> TerracottaPrimary
    1 -> CategoryStudyColor
    2 -> SageSecondary
    3 -> Color(0xFF1E88E5)
    else -> CategoryWorkColor
}
