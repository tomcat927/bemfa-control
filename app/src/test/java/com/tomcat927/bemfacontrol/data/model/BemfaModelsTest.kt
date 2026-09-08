package com.tomcat927.bemfacontrol.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class BemfaModelsTest {

    @Test
    fun `BemfaTopicList parses wrapped data response`() {
        val json = Json { ignoreUnknownKeys = true }
        val payload = """
            {"code":0,"msg":"success","data":{"data":[{"topic":"chazuo01001","name":"客厅插座","room":"客厅","msg":"on"}]}}
        """.trimIndent()

        val result = json.decodeFromString(BemfaTopicList.serializer(), payload)

        assertEquals(1, result.topics.size)
        assertEquals("chazuo01001", result.topics.first().topic)
        assertEquals("客厅插座", result.topics.first().name)
        assertEquals("客厅", result.topics.first().room)
        assertEquals("on", result.topics.first().msg)
    }

    @Test
    fun `BemfaTopicList parses direct array response`() {
        val json = Json { ignoreUnknownKeys = true }
        val payload = """[{"topic":"chazuo01001","name":"客厅插座"}]"""

        val result = json.decodeFromString(BemfaTopicList.serializer(), payload)

        assertEquals(1, result.topics.size)
        assertEquals("chazuo01001", result.topics.first().topic)
    }
}
