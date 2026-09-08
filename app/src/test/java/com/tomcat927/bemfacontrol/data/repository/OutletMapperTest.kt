package com.tomcat927.bemfacontrol.data.repository

import com.tomcat927.bemfacontrol.data.model.BemfaTopic
import org.junit.Assert.assertEquals
import org.junit.Test

class OutletMapperTest {

    @Test
    fun `filterOutlets keeps outlet suffix and outlet device type`() {
        val topics = listOf(
            BemfaTopic(topic = "living-room-001", name = "Living Room Plug"),
            BemfaTopic(topic = "sensor004", deviceType = "sensor"),
            BemfaTopic(topic = "kitchen", deviceType = "outlet"),
        )

        val outlets = OutletMapper.filterOutlets(topics)

        assertEquals(listOf("living-room-001", "kitchen"), outlets.map { it.topic })
    }

    @Test
    fun `toOutletDevices uses fallback name and room`() {
        val devices = OutletMapper.toOutletDevices(
            listOf(
                BemfaTopic(
                    topic = "desk001",
                    msg = "on",
                    online = true,
                    time = "2026-09-08 12:00:00",
                ),
            ),
        )

        assertEquals(1, devices.size)
        assertEquals("desk001", devices.first().name)
        assertEquals("未分配房间", devices.first().room)
        assertEquals(true, devices.first().isOn)
        assertEquals(true, devices.first().isOnline)
    }

    @Test
    fun `groupByRoom sorts rooms and keeps complete names`() {
        val devices = OutletMapper.toOutletDevices(
            listOf(
                BemfaTopic(
                    topic = "desk001",
                    name = "Desk Plug with a very long complete display name",
                    room = "书房",
                ),
                BemfaTopic(
                    topic = "tv001",
                    name = "TV Cabinet Plug",
                    room = "客厅",
                ),
            ),
        )

        val groups = OutletMapper.groupByRoom(devices)

        assertEquals(listOf("书房", "客厅"), groups.map { it.room })
        assertEquals(
            "Desk Plug with a very long complete display name",
            groups.first { it.room == "书房" }.devices.first().name,
        )
    }
}
