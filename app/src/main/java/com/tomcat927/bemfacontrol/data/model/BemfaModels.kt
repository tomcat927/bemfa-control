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
