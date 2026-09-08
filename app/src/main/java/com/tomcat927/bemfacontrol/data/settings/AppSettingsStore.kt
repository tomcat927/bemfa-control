package com.tomcat927.bemfacontrol.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsStore(private val context: Context) {

    private val uidKey = stringPreferencesKey("bemfa_uid")
    private val debugLoggingKey = booleanPreferencesKey("debug_logging")
    private val autoUpdateKey = booleanPreferencesKey("auto_update")
    private val proxyFirstKey = booleanPreferencesKey("proxy_first")

    fun uidFlow(): Flow<String> =
        context.settingsDataStore.data.map { preferences -> preferences[uidKey].orEmpty() }

    suspend fun uid(): String = uidFlow().first()
    fun debugLoggingFlow(): Flow<Boolean> =
        context.settingsDataStore.data.map { preferences -> preferences[debugLoggingKey] ?: false }

    suspend fun debugLogging(): Boolean = debugLoggingFlow().first()

    suspend fun setDebugLogging(value: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[debugLoggingKey] = value
        }
    }

    fun autoUpdateFlow(): Flow<Boolean> =
        context.settingsDataStore.data.map { preferences -> preferences[autoUpdateKey] ?: true }

    suspend fun autoUpdate(): Boolean = autoUpdateFlow().first()

    suspend fun setAutoUpdate(value: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[autoUpdateKey] = value
        }
    }

    fun proxyFirstFlow(): Flow<Boolean> =
        context.settingsDataStore.data.map { preferences -> preferences[proxyFirstKey] ?: true }

    suspend fun proxyFirst(): Boolean = proxyFirstFlow().first()

    suspend fun setProxyFirst(value: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[proxyFirstKey] = value
        }
    }

    suspend fun setUid(value: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[uidKey] = value.trim()
        }
    }
}
