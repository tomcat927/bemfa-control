package com.tomcat927.bemfacontrol.data.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomOrderSyncModelsTest {

    @Test
    fun `room order payload round trips`() {
        val json = Json { ignoreUnknownKeys = true }
        val payload = RoomOrderPayload(
            updatedAt = 1760000000000L,
            order = listOf("客厅", "卧室", "书房"),
        )
        val encoded = json.encodeToString(payload)

        assertEquals(payload, json.decodeFromString<RoomOrderPayload>(encoded))
    }
}
