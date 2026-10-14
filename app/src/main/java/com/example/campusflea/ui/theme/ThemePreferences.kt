package com.example.campusflea.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "theme_settings")

class ThemePreferences(private val context: Context) {
    companion object {
        private val THEME_MODE_KEY = intPreferencesKey("theme_mode")
        const val MODE_SYSTEM = 0
        const val MODE_LIGHT = 1
        const val MODE_DARK = 2
    }

    val themeMode: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE_KEY] ?: MODE_SYSTEM
    }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode
        }
    }
}
