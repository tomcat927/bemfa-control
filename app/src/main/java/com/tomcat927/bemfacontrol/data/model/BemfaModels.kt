package com.tomcat927.bemfacontrol.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BemfaResponse<T>(
    val code: Int = -1,
    val message: String? = null,
    val data: T? = null,
)

@Serializable
data class BemfaTopic(
    val topic: String,
    val name: String = "",
    val room: String = "",
    val group: String = "",
    val msg: String = "",
    val online: Boolean = false,
    val deviceType: String = "",
    val time: String? = null,
    val unix: Long? = null,
)

@Serializable
data class BemfaMessageRequest(
    val uid: String,
    val topic: String,
    val type: Int = DEVICE_TYPE_TCP,
    val msg: String,
) {
    companion object {
        const val DEVICE_TYPE_TCP = 3
    }
}

data class OutletDevice(
    val topic: String,
    val name: String,
    val room: String,
    val isOnline: Boolean,
    val isOn: Boolean,
    val lastMessageTime: String?,
)

data class DeviceGroup(
    val room: String,
    val devices: List<OutletDevice>,
)
