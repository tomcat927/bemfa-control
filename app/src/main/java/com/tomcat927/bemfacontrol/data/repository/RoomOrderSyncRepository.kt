package com.tomcat927.bemfacontrol.data.repository

import com.tomcat927.bemfacontrol.data.model.GitHubGistFileRequest
import com.tomcat927.bemfacontrol.data.model.GitHubGistRequest
import com.tomcat927.bemfacontrol.data.model.RoomOrderPayload
import com.tomcat927.bemfacontrol.data.network.GitHubApi
import com.tomcat927.bemfacontrol.data.settings.AppSettingsStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed interface RoomOrderRemoteRead {
    data object NotConfigured : RoomOrderRemoteRead
    data object Missing : RoomOrderRemoteRead
    data class Available(val payload: RoomOrderPayload) : RoomOrderRemoteRead
}

class RoomOrderSyncRepository(
    private val api: GitHubApi,
    private val settingsStore: AppSettingsStore,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    suspend fun readRemote(): RoomOrderRemoteRead {
        val config = settingsStore.gistConfig() ?: return RoomOrderRemoteRead.NotConfigured
        val gist = api.getGist(authorization(config.second), config.first)
        val file = gist.files[ROOM_ORDER_FILE]
        val content = file?.content.orEmpty()
        if (file?.truncated == true || content.isBlank()) {
            return RoomOrderRemoteRead.Missing
        }

        val payload = json.decodeFromString<RoomOrderPayload>(content)
        return RoomOrderRemoteRead.Available(payload)
    }

    suspend fun save(order: List<String>) {
        val config = requireNotNull(settingsStore.gistConfig()) { "Gist 同步未配置" }
        val payload = RoomOrderPayload(
            updatedAt = System.currentTimeMillis(),
            order = order,
        )
        api.updateGist(
            authorization(config.second),
            config.first,
            GitHubGistRequest(
                files = mapOf(
                    ROOM_ORDER_FILE to GitHubGistFileRequest(json.encodeToString(payload)),
                ),
            ),
        )
    }

    suspend fun configure(inputGistId: String, inputToken: String, localOrder: List<String>): String {
        val token = inputToken.trim().ifBlank { settingsStore.githubToken() }
        if (token.isBlank()) {
            throw IllegalArgumentException("请填写 GitHub Token")
        }

        val inputId = inputGistId.trim().substringAfterLast('/')
        val gistId = if (inputId.isBlank()) {
            val payload = RoomOrderPayload(
                updatedAt = System.currentTimeMillis(),
                order = localOrder,
            )
            val gist = api.createGist(
                authorization(token),
                GitHubGistRequest(
                    description = "BemfaControl room order",
                    public = false,
                    files = mapOf(
                        ROOM_ORDER_FILE to GitHubGistFileRequest(json.encodeToString(payload)),
                    ),
                ),
            )
            gist.id.orEmpty().ifBlank { throw IllegalStateException("创建 Gist 失败") }
        } else {
            api.getGist(authorization(token), inputId)
            inputId
        }

        settingsStore.setGistConfig(gistId, token)
        return gistId
    }

    private fun authorization(token: String): String = "token $token"

    companion object {
        const val ROOM_ORDER_FILE = "room_order.json"
    }
}
