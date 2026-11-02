package com.tomcat927.bemfacontrol.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsStore(private val context: Context) {

    private val uidKey = stringPreferencesKey("bemfa_uid")
    private val debugLoggingKey = booleanPreferencesKey("debug_logging")
    private val autoUpdateKey = booleanPreferencesKey("auto_update")
    private val proxyFirstKey = booleanPreferencesKey("proxy_first")
    private val roomOrderKey = stringPreferencesKey("room_order")
    private val roomOrderUpdatedAtKey = longPreferencesKey("room_order_updated_at")
    private val lastRoomKey = stringPreferencesKey("last_selected_room")
    private val gistIdKey = stringPreferencesKey("room_order_gist_id")
    private val githubTokenKey = stringPreferencesKey("room_order_github_token")

    private val roomOrderJson = Json { encodeDefaults = true }

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

    fun roomOrderFlow(): Flow<List<String>> =
        context.settingsDataStore.data.map { preferences ->
            val raw = preferences[roomOrderKey].orEmpty()
            if (raw.isBlank()) emptyList()
            else roomOrderJson.decodeFromString(ListSerializer(String.serializer()), raw)
        }

    suspend fun roomOrder(): List<String> = roomOrderFlow().first()

    suspend fun roomOrderUpdatedAt(): Long =
        context.settingsDataStore.data.map { preferences ->
            preferences[roomOrderUpdatedAtKey] ?: 0L
        }.first()

    suspend fun setRoomOrder(order: List<String>, updatedAt: Long = System.currentTimeMillis()) {
        val encoded = roomOrderJson.encodeToString(ListSerializer(String.serializer()), order)
        context.settingsDataStore.edit { preferences ->
            preferences[roomOrderKey] = encoded
            preferences[roomOrderUpdatedAtKey] = updatedAt
        }
    }

    suspend fun gistId(): String = context.settingsDataStore.data.map { preferences ->
        preferences[gistIdKey].orEmpty()
    }.first()

    suspend fun githubToken(): String = context.settingsDataStore.data.map { preferences ->
        preferences[githubTokenKey].orEmpty()
    }.first()

    suspend fun gistConfig(): Pair<String, String>? = context.settingsDataStore.data.map { preferences ->
        val id = preferences[gistIdKey].orEmpty().trim()
        val token = preferences[githubTokenKey].orEmpty().trim()
        if (id.isBlank() || token.isBlank()) null else id to token
    }.first()

    suspend fun setGistConfig(gistId: String, token: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[gistIdKey] = gistId.trim()
            preferences[githubTokenKey] = token.trim()
        }
    }

    fun lastRoomFlow(): Flow<String?> =
        context.settingsDataStore.data.map { preferences ->
            preferences[lastRoomKey]?.takeIf { it.isNotBlank() }
        }

    suspend fun lastRoom(): String? = lastRoomFlow().first()

    suspend fun setLastRoom(room: String?) {
        context.settingsDataStore.edit { preferences ->
            if (room.isNullOrBlank()) {
                preferences.remove(lastRoomKey)
            } else {
                preferences[lastRoomKey] = room
            }
        }
    }
}
