package com.tomcat927.bemfacontrol.data.repository

import com.tomcat927.bemfacontrol.data.model.BemfaTopic
import com.tomcat927.bemfacontrol.data.model.DeviceGroup
import com.tomcat927.bemfacontrol.data.model.OutletDevice

object OutletMapper {

    private const val OUTLET_SUFFIX = "001"

    fun filterOutlets(topics: List<BemfaTopic>): List<BemfaTopic> =
        topics.filter { it.topic.isNotBlank() && isOutlet(it) }

    fun toOutletDevices(topics: List<BemfaTopic>): List<OutletDevice> =
        filterOutlets(topics)
            .map { topic ->
                OutletDevice(
                    topic = topic.topic,
                    name = topic.name.ifBlank { topic.topic },
                    room = topic.room.ifBlank { "未分配房间" },
                    isOnline = topic.online,
                    isOn = topic.msg.equals("on", ignoreCase = true),
                    lastMessageTime = topic.time,
                )
            }
            .sortedWith(compareBy({ it.room }, { it.name }, { it.topic }))

    fun groupByRoom(devices: List<OutletDevice>, roomOrder: List<String> = emptyList()): List<DeviceGroup> {
        val grouped = devices
            .groupBy { it.room }
            .map { (room, roomDevices) -> DeviceGroup(room, roomDevices) }
        if (roomOrder.isEmpty()) return grouped.sortedBy { it.room }
        val orderMap = roomOrder.withIndex().associate { it.value to it.index }
        return grouped.sortedWith(
            compareBy(
                { orderMap[it.room] ?: Int.MAX_VALUE },
                { it.room },
            ),
        )
    }

    fun isOutlet(topic: BemfaTopic): Boolean =
        topic.deviceType.equals("outlet", ignoreCase = true) ||
            topic.deviceType.isBlank() && topic.topic.endsWith(OUTLET_SUFFIX, ignoreCase = true)
}
