package com.tomcat927.bemfacontrol.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RoomOrderPayload(
    val version: Int = 1,
    val updatedAt: Long,
    val order: List<String> = emptyList(),
)
