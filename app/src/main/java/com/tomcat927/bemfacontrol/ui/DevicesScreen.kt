package com.tomcat927.bemfacontrol.data.repository

import com.tomcat927.bemfacontrol.diagnostics.RuntimeLog
import com.tomcat927.bemfacontrol.data.model.BemfaMessageRequest
import com.tomcat927.bemfacontrol.data.model.DeviceGroup
import com.tomcat927.bemfacontrol.data.model.OutletDevice
import com.tomcat927.bemfacontrol.data.network.BemfaApi
import com.tomcat927.bemfacontrol.data.network.BemfaApiFactory

class BemfaApiException(
    message: String,
    val code: Int,
) : IllegalStateException(message)

class BemfaOutletRepository(
    private val api: BemfaApi,
) {

    suspend fun outlets(uid: String): List<OutletDevice> {
        val startedAt = System.currentTimeMillis()
        try {
            val response = api.allTopics(uid, BemfaApiFactory.tcpDeviceType())
            assertSuccess(response.code, response.message ?: response.msg)
            val devices = OutletMapper.toOutletDevices(response.data?.topics.orEmpty())
            RuntimeLog.debug(
                "allTopic success: ${devices.size} outlets in ${System.currentTimeMillis() - startedAt}ms",
            )
            return devices
        } catch (throwable: Throwable) {
            RuntimeLog.error(
                "allTopic failed after ${System.currentTimeMillis() - startedAt}ms",
                throwable,
            )
            throw throwable
        }
    }

    suspend fun groups(uid: String): List<DeviceGroup> =
        OutletMapper.groupByRoom(outlets(uid))

    suspend fun checkOnline(uid: String, topic: String): Boolean {
        val startedAt = System.currentTimeMillis()
        try {
            val response = api.checkOnline(uid, topic, BemfaApiFactory.tcpDeviceType())
            assertSuccess(response.code, response.message ?: response.msg)
            val online = response.data ?: false
            RuntimeLog.debug("checkOnline: topic=$topic online=$online in ${System.currentTimeMillis() - startedAt}ms")
            return online
        } catch (throwable: Throwable) {
            RuntimeLog.error("checkOnline failed: topic=$topic", throwable)
            throw throwable
        }
    }

    suspend fun setPower(uid: String, topic: String, on: Boolean) {
        RuntimeLog.debug("setPower start: topic=$topic target=${if (on) "on" else "off"}")
        val startedAt = System.currentTimeMillis()
        try {
            val response = api.postMessage(
                BemfaMessageRequest(
                    uid = uid,
                    topic = topic,
                    type = BemfaApiFactory.tcpDeviceType(),
                    msg = if (on) "on" else "off",
                ),
            )
            assertSuccess(response.code, response.message ?: response.msg)
            RuntimeLog.debug("setPower success in ${System.currentTimeMillis() - startedAt}ms")
        } catch (throwable: Throwable) {
            RuntimeLog.error(
                "setPower failed after ${System.currentTimeMillis() - startedAt}ms",
                throwable,
            )
            throw throwable
        }
    }

    private fun assertSuccess(code: Int, message: String?) {
        if (code != 0) {
            throw BemfaApiException(message ?: "巴法云请求失败", code)
        }
    }
}
