package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ThemeMode
import com.example.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.plannerDataStore: DataStore<Preferences> by preferencesDataStore(name = "planner_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_GOAL = stringPreferencesKey("user_goal")
        val AVATAR_URI = stringPreferencesKey("avatar_uri")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val MORNING_REMINDER_MINUTES = intPreferencesKey("morning_reminder_minutes")
        val EVENING_REMINDER_MINUTES = intPreferencesKey("evening_reminder_minutes")
        val MISSED_TASK_NOTIFICATIONS = booleanPreferencesKey("missed_task_notifications")
        val DEFAULT_LEAD_MINUTES = intPreferencesKey("default_lead_minutes")
        val SHOW_MOTIVATIONAL_QUOTES = booleanPreferencesKey("show_motivational_quotes")
        val IS_SEEDED = booleanPreferencesKey("is_seeded")
    }

    val preferencesFlow: Flow<UserPreferences> = context.plannerDataStore.data.map { prefs ->
        UserPreferences(
            userName = prefs[Keys.USER_NAME] ?: "Alex Morgan",
            userGoal = prefs[Keys.USER_GOAL] ?: "Build calm, consistent daily momentum",
            avatarUri = prefs[Keys.AVATAR_URI] ?: "",
            themeMode = enumValues<ThemeMode>().firstOrNull {
                it.name == prefs[Keys.THEME_MODE]
            } ?: ThemeMode.SYSTEM,
            dailyReminderEnabled = prefs[Keys.DAILY_REMINDER_ENABLED] ?: true,
            morningReminderMinutes = prefs[Keys.MORNING_REMINDER_MINUTES] ?: (8 * 60),
            eveningReminderMinutes = prefs[Keys.EVENING_REMINDER_MINUTES] ?: (21 * 60),
            missedTaskNotificationsEnabled = prefs[Keys.MISSED_TASK_NOTIFICATIONS] ?: true,
            defaultLeadMinutes = prefs[Keys.DEFAULT_LEAD_MINUTES] ?: 15,
            showMotivationalQuotes = prefs[Keys.SHOW_MOTIVATIONAL_QUOTES] ?: true,
            isSeeded = prefs[Keys.IS_SEEDED] ?: false
        )
    }

    suspend fun currentPreferences(): UserPreferences = preferencesFlow.first()

    suspend fun updateProfile(name: String, goal: String, avatarUri: String? = null) {
        context.plannerDataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = name.trim().ifEmpty { "Planner User" }
            prefs[Keys.USER_GOAL] = goal.trim()
            if (avatarUri != null) {
                prefs[Keys.AVATAR_URI] = avatarUri
            }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.plannerDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun updateReminderSettings(
        dailyEnabled: Boolean,
        morningMinutes: Int,
        eveningMinutes: Int,
        missedEnabled: Boolean,
        defaultLeadMinutes: Int
    ) {
        context.plannerDataStore.edit { prefs ->
            prefs[Keys.DAILY_REMINDER_ENABLED] = dailyEnabled
            prefs[Keys.MORNING_REMINDER_MINUTES] = morningMinutes
            prefs[Keys.EVENING_REMINDER_MINUTES] = eveningMinutes
            prefs[Keys.MISSED_TASK_NOTIFICATIONS] = missedEnabled
            prefs[Keys.DEFAULT_LEAD_MINUTES] = defaultLeadMinutes
        }
    }

    suspend fun setShowQuotes(show: Boolean) {
        context.plannerDataStore.edit { prefs ->
            prefs[Keys.SHOW_MOTIVATIONAL_QUOTES] = show
        }
    }

    suspend fun markSeeded() {
        context.plannerDataStore.edit { prefs ->
            prefs[Keys.IS_SEEDED] = true
        }
    }

    suspend fun restorePreferences(userPrefs: UserPreferences) {
        context.plannerDataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = userPrefs.userName
            prefs[Keys.USER_GOAL] = userPrefs.userGoal
            prefs[Keys.AVATAR_URI] = userPrefs.avatarUri
            prefs[Keys.THEME_MODE] = userPrefs.themeMode.name
            prefs[Keys.DAILY_REMINDER_ENABLED] = userPrefs.dailyReminderEnabled
            prefs[Keys.MORNING_REMINDER_MINUTES] = userPrefs.morningReminderMinutes
            prefs[Keys.EVENING_REMINDER_MINUTES] = userPrefs.eveningReminderMinutes
            prefs[Keys.MISSED_TASK_NOTIFICATIONS] = userPrefs.missedTaskNotificationsEnabled
            prefs[Keys.DEFAULT_LEAD_MINUTES] = userPrefs.defaultLeadMinutes
            prefs[Keys.SHOW_MOTIVATIONAL_QUOTES] = userPrefs.showMotivationalQuotes
            prefs[Keys.IS_SEEDED] = true
        }
    }
}
