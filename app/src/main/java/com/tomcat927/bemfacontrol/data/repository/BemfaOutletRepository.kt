package com.tomcat927.bemfacontrol.data.repository

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
        val response = api.allTopics(uid, BemfaApiFactory.tcpDeviceType())
        assertSuccess(response.code, response.message)
        return OutletMapper.toOutletDevices(response.data.orEmpty())
    }

    suspend fun groups(uid: String): List<DeviceGroup> =
        OutletMapper.groupByRoom(outlets(uid))

    suspend fun setPower(uid: String, topic: String, on: Boolean) {
        val response = api.postMessage(
            BemfaMessageRequest(
                uid = uid,
                topic = topic,
                type = BemfaApiFactory.tcpDeviceType(),
                msg = if (on) "on" else "off",
            ),
        )
        assertSuccess(response.code, response.message)
    }

    private fun assertSuccess(code: Int, message: String?) {
        if (code != 0) {
            throw BemfaApiException(message ?: "巴法云请求失败", code)
        }
    }
}
