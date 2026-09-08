package com.tomcat927.bemfacontrol.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsStore(private val context: Context) {

    private val uidKey = stringPreferencesKey("bemfa_uid")

    fun uidFlow(): Flow<String> =
        context.settingsDataStore.data.map { preferences -> preferences[uidKey].orEmpty() }

    suspend fun uid(): String = uidFlow().first()

    suspend fun setUid(value: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[uidKey] = value.trim()
        }
    }
}
