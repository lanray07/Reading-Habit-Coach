package com.readinghabitcoach.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("coach_settings")
data class Settings(val onboarded: Boolean = false, val minutes: Int = 20, val weeklyDays: Int = 4,
    val dark: Boolean = false, val weekStart: Int = 1, val reminder: Boolean = false,
    val reminderHour: Int = 19, val language: String = "en-GB", val yearlyBooks: Int = 12)
class Preferences(private val context: Context) {
    val flow = context.store.data.map { p -> Settings(p[booleanPreferencesKey("onboarded")] ?: false,
        p[intPreferencesKey("minutes")] ?: 20, p[intPreferencesKey("weeklyDays")] ?: 4,
        p[booleanPreferencesKey("dark")] ?: false, p[intPreferencesKey("weekStart")] ?: 1,
        p[booleanPreferencesKey("reminder")] ?: false, p[intPreferencesKey("reminderHour")] ?: 19,
        p[stringPreferencesKey("language")] ?: "en-GB", p[intPreferencesKey("yearlyBooks")] ?: 12) }
    suspend fun save(s: Settings) { context.store.edit { p ->
        p[booleanPreferencesKey("onboarded")] = s.onboarded; p[intPreferencesKey("minutes")] = s.minutes
        p[intPreferencesKey("weeklyDays")] = s.weeklyDays; p[booleanPreferencesKey("dark")] = s.dark
        p[intPreferencesKey("weekStart")] = s.weekStart; p[booleanPreferencesKey("reminder")] = s.reminder
        p[intPreferencesKey("reminderHour")] = s.reminderHour; p[stringPreferencesKey("language")] = s.language
        p[intPreferencesKey("yearlyBooks")] = s.yearlyBooks
    } }
    suspend fun clear() { context.store.edit { it.clear() } }
}
