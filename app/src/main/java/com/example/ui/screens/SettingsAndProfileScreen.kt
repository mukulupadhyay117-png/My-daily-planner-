package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ThemeMode
import com.example.notifications.NotificationHelper
import com.example.ui.PlannerUiState
import com.example.ui.components.SimpleTimePickerDialog
import com.example.util.DateTimeUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAndProfileScreen(
    uiState: PlannerUiState,
    onBack: () -> Unit,
    onSaveProfile: (String, String, String?) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onUpdateReminderSettings: (Boolean, Int, Int, Boolean, Int) -> Unit,
    onSetShowQuotes: (Boolean) -> Unit,
    onExportBackupJson: suspend () -> String,
    onRestoreBackupJson: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = uiState.preferences

    var nameInput by remember(prefs.userName) { mutableStateOf(prefs.userName) }
    var goalInput by remember(prefs.userGoal) { mutableStateOf(prefs.userGoal) }
    var pickMorningTime by remember { mutableStateOf(false) }
    var pickEveningTime by remember { mutableStateOf(false) }
    var hasNotifPerm by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPerm = granted
        onShowMessage(if (granted) "Notifications enabled!" else "Notification permission denied")
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onSaveProfile(nameInput, goalInput, uri.toString())
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = onExportBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                }.onSuccess {
                    onShowMessage("Backup exported successfully!")
                }.onFailure {
                    onShowMessage("Could not export backup file.")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Empty file")
                }.onSuccess { raw ->
                    onRestoreBackupJson(raw)
                }.onFailure {
                    onShowMessage("Could not read selected backup file.")
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.testTag("settings_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. User Profile Card
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
                        Text("User Profile", style = MaterialTheme.typography.titleLarge)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                            ) {
                                if (prefs.avatarUri.isNotBlank()) {
                                    AsyncImage(
                                        model = Uri.parse(prefs.avatarUri),
                                        contentDescription = "Profile photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_profile_avatar),
                                        contentDescription = "Default profile avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Change Photo")
                            }
                        }

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = goalInput,
                            onValueChange = { goalInput = it },
                            label = { Text("Personal Focus Motto") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = { onSaveProfile(nameInput, goalInput, null) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save Profile")
                        }
                    }
                }
            }

            // 2. Theme Appearance (Light / Dark / System)
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
                        Text("Appearance", style = MaterialTheme.typography.titleLarge)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            ThemeMode.entries.forEachIndexed { idx, mode ->
                                SegmentedButton(
                                    selected = prefs.themeMode == mode,
                                    onClick = { onSetThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(idx, ThemeMode.entries.size),
                                    modifier = Modifier.testTag("theme_mode_${mode.name}")
                                ) {
                                    Text(
                                        text = when (mode) {
                                            ThemeMode.SYSTEM -> "System"
                                            ThemeMode.LIGHT -> "Light"
                                            ThemeMode.DARK -> "Dark"
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Motivational Quote Card", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Show daily quote on Dashboard",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = prefs.showMotivationalQuotes,
                                onCheckedChange = onSetShowQuotes
                            )
                        }
                    }
                }
            }

            // 3. Reminders & Notifications
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
                        Text("Reminders & Notifications", style = MaterialTheme.typography.titleLarge)

                        if (!hasNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Allow notifications to receive task & daily alerts.",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                                    ) {
                                        Text("Allow")
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Daily Briefing Reminders", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Morning plan & evening wrap-up notifications",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = prefs.dailyReminderEnabled,
                                onCheckedChange = { enabled ->
                                    onUpdateReminderSettings(
                                        enabled,
                                        prefs.morningReminderMinutes,
                                        prefs.eveningReminderMinutes,
                                        prefs.missedTaskNotificationsEnabled,
                                        prefs.defaultLeadMinutes
                                    )
                                }
                            )
                        }

                        if (prefs.dailyReminderEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { pickMorningTime = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Morning: ${DateTimeUtils.formatMinutes(prefs.morningReminderMinutes)}")
                                }
                                OutlinedButton(
                                    onClick = { pickEveningTime = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Evening: ${DateTimeUtils.formatMinutes(prefs.eveningReminderMinutes)}")
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Missed Task Notifications", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Alert if a scheduled task stays pending",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = prefs.missedTaskNotificationsEnabled,
                                onCheckedChange = { enabled ->
                                    onUpdateReminderSettings(
                                        prefs.dailyReminderEnabled,
                                        prefs.morningReminderMinutes,
                                        prefs.eveningReminderMinutes,
                                        enabled,
                                        prefs.defaultLeadMinutes
                                    )
                                }
                            )
                        }

                        Text("Default Task Lead Time", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0 to "At time", 10 to "10m", 15 to "15m", 30 to "30m").forEach { (mins, label) ->
                                FilterChip(
                                    selected = prefs.defaultLeadMinutes == mins,
                                    onClick = {
                                        onUpdateReminderSettings(
                                            prefs.dailyReminderEnabled,
                                            prefs.morningReminderMinutes,
                                            prefs.eveningReminderMinutes,
                                            prefs.missedTaskNotificationsEnabled,
                                            mins
                                        )
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (!NotificationHelper.hasNotificationPermission(context) &&
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                                ) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    NotificationHelper.showSimpleReminderNotification(
                                        context = context,
                                        notificationId = 999_001,
                                        channelId = NotificationHelper.CHANNEL_TASKS,
                                        title = "My Daily Planner Reminder",
                                        body = "Notifications are active! You'll get timely alerts for tasks, routines & habits."
                                    )
                                    onShowMessage("Test notification sent!")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_notification_button")
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Send Test Notification")
                        }
                    }
                }
            }

            // 4. Backup & Restore
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
                        Text("Backup & Restore", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Export all tasks, routines, habits, streaks, and profile settings to a portable JSON file, or restore from a previous backup.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    exportLauncher.launch("my_daily_planner_backup_${uiState.todayKey}.json")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_backup_button")
                            ) {
                                Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Export")
                            }
                            OutlinedButton(
                                onClick = {
                                    importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("restore_backup_button")
                            ) {
                                Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Restore")
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (pickMorningTime) {
        SimpleTimePickerDialog(
            title = "Morning Briefing Time",
            initialMinutes = prefs.morningReminderMinutes,
            onDismiss = { pickMorningTime = false },
            onConfirm = { mins ->
                pickMorningTime = false
                onUpdateReminderSettings(
                    prefs.dailyReminderEnabled,
                    mins,
                    prefs.eveningReminderMinutes,
                    prefs.missedTaskNotificationsEnabled,
                    prefs.defaultLeadMinutes
                )
            }
        )
    }

    if (pickEveningTime) {
        SimpleTimePickerDialog(
            title = "Evening Wrap-Up Time",
            initialMinutes = prefs.eveningReminderMinutes,
            onDismiss = { pickEveningTime = false },
            onConfirm = { mins ->
                pickEveningTime = false
                onUpdateReminderSettings(
                    prefs.dailyReminderEnabled,
                    prefs.morningReminderMinutes,
                    mins,
                    prefs.missedTaskNotificationsEnabled,
                    prefs.defaultLeadMinutes
                )
            }
        )
    }
}
