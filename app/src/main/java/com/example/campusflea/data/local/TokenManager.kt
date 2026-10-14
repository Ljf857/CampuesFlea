package com.example.campusflea.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "campus_flea_tokens")

class TokenManager(private val context: Context) {

    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val EXPIRE_TIME = longPreferencesKey("expire_time")

        @Volatile
        private var INSTANCE: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            prefs[REFRESH_TOKEN] = refreshToken
            prefs[EXPIRE_TIME] = System.currentTimeMillis() + expiresIn * 1000
        }
    }

    suspend fun getAccessToken(): String? {
        return context.dataStore.data.map { it[ACCESS_TOKEN] }.first()
    }

    suspend fun getRefreshToken(): String? {
        return context.dataStore.data.map { it[REFRESH_TOKEN] }.first()
    }

    suspend fun isAccessTokenExpired(): Boolean {
        val expireTime = context.dataStore.data.map { it[EXPIRE_TIME] }.first() ?: return true
        return System.currentTimeMillis() >= expireTime - 60_000
    }

    suspend fun isLoggedIn(): Boolean {
        val token = getAccessToken()
        return !token.isNullOrEmpty()
    }

    suspend fun clearTokens() {
        context.dataStore.edit { it.clear() }
    }
}
