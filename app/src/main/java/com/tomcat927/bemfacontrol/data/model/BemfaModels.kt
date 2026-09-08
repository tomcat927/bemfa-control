package com.tomcat927.bemfacontrol.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject

@Serializable
data class BemfaResponse<T>(
    val code: Int = -1,
    val message: String? = null,
    val msg: String? = null,
    val data: T? = null,
)

@Serializable
data class BemfaActionResponse(
    val code: Int = -1,
    val message: String? = null,
    val msg: String? = null,
    val data: BemfaActionCode? = null,
)

@Serializable
data class BemfaActionCode(
    val code: Int = 0,
)

@Serializable(with = BemfaTopicListSerializer::class)
data class BemfaTopicList(
    val topics: List<BemfaTopic> = emptyList(),
)

object BemfaTopicListSerializer : KSerializer<BemfaTopicList> {

    private val delegate = ListSerializer(BemfaTopic.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): BemfaTopicList {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        val topicsElement = when (element) {
            is JsonArray -> element
            is JsonObject -> element["data"] as? JsonArray ?: JsonArray(emptyList())
            else -> JsonArray(emptyList())
        }
        return BemfaTopicList(decoder.json.decodeFromJsonElement(delegate, topicsElement))
    }

    override fun serialize(encoder: Encoder, value: BemfaTopicList) {
        delegate.serialize(encoder, value.topics)
    }
}

@Serializable
data class BemfaTopic(
    val topic: String,
    val name: String = "",
    @SerialName("type")
    val protocolType: Int = 3,
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

@Serializable
data class BemfaNameRequest(
    val uid: String,
    val topic: String,
    val type: Int = 3,
    val name: String,
)

@Serializable
data class BemfaChangeRoomRequest(
    val openID: String,
    val topicIDs: List<String>,
    val type: Int = 3,
    val room: String = "",
)

@Serializable(with = BemfaRoomListSerializer::class)
data class BemfaRoomList(
    val rooms: List<BemfaRoom> = emptyList(),
)

object BemfaRoomListSerializer : KSerializer<BemfaRoomList> {
    private val delegate = ListSerializer(BemfaRoom.serializer())
    override val descriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): BemfaRoomList {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        val arr = when (element) {
            is JsonArray -> element
            is JsonObject -> element["data"] as? JsonArray ?: JsonArray(emptyList())
            else -> JsonArray(emptyList())
        }
        return BemfaRoomList(decoder.json.decodeFromJsonElement(delegate, arr))
    }

    override fun serialize(encoder: Encoder, value: BemfaRoomList) {
        delegate.serialize(encoder, value.rooms)
    }
}

@Serializable
data class BemfaRoom(
    val name: String = "",
    val num: Int = 0,
)

@Serializable(with = BemfaTimerListSerializer::class)
data class BemfaTimerList(
    val timers: List<BemfaTimer> = emptyList(),
)

object BemfaTimerListSerializer : KSerializer<BemfaTimerList> {
    private val delegate = ListSerializer(BemfaTimer.serializer())
    override val descriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): BemfaTimerList {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        val arr = when (element) {
            is JsonArray -> element
            is JsonObject -> element["data"] as? JsonArray ?: JsonArray(emptyList())
            else -> JsonArray(emptyList())
        }
        return BemfaTimerList(decoder.json.decodeFromJsonElement(delegate, arr))
    }

    override fun serialize(encoder: Encoder, value: BemfaTimerList) {
        delegate.serialize(encoder, value.timers)
    }
}

@Serializable
data class BemfaTimer(
    val id: Int = 0,
    val status: Int = 0,
    val time: String = "",
    val msg: String = "",
    val week: List<Int> = emptyList(),
) {
    val isEnabled: Boolean get() = status == 1
}

@Serializable
data class BemfaTimerRequest(
    val openID: String,
    val topicID: String,
    val type: Int = 3,
    val time: String,
    val msg: String,
    val week: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6),
)

@Serializable
data class BemfaTimerToggleRequest(
    val openID: String,
    val topicID: String,
    val type: Int = 3,
    val id: Int,
)
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonInt
