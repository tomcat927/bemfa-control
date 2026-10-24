package com.tomcat927.bemfacontrol.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tomcat927.bemfacontrol.data.model.DeviceCommand
import com.tomcat927.bemfacontrol.data.model.DiagResult
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.diagCacheDataStore by preferencesDataStore(name = "diag_cache")

/**
 * 持久化诊断查询结果，按 topic + command 缓存。
 * 用于“查一次就记住”的设备信息（如 MAC 地址不会变），
 * 下次进入诊断页直接展示缓存，无需重复查询；用户可手动点查询刷新。
 */
class DiagCacheStore(private val context: Context) {

    private val cacheKey = stringPreferencesKey("diag_results")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val outerSerializer = MapSerializer(
        String.serializer(),
        MapSerializer(DeviceCommand.serializer(), DiagResult.serializer()),
    )

    suspend fun load(topic: String): Map<DeviceCommand, DiagResult> {
        val raw = context.diagCacheDataStore.data.first()[cacheKey].orEmpty()
        if (raw.isBlank()) return emptyMap()
        val all = runCatching { json.decodeFromString(outerSerializer, raw) }.getOrDefault(emptyMap())
        return all[topic].orEmpty()
    }

    suspend fun save(topic: String, command: DeviceCommand, result: DiagResult) {
        val all = runCatching {
            json.decodeFromString(outerSerializer, context.diagCacheDataStore.data.first()[cacheKey].orEmpty())
        }.getOrDefault(emptyMap()).toMutableMap()
        val topicMap = (all[topic] ?: emptyMap()).toMutableMap()
        topicMap[command] = result
        all[topic] = topicMap
        context.diagCacheDataStore.edit { preferences ->
            preferences[cacheKey] = json.encodeToString(outerSerializer, all)
        }
    }
}
