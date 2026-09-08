package com.tomcat927.bemfacontrol.data.repository

import com.tomcat927.bemfacontrol.diagnostics.RuntimeLog
import com.tomcat927.bemfacontrol.data.model.BemfaMessageRequest
import com.tomcat927.bemfacontrol.data.model.BemfaNameRequest
import com.tomcat927.bemfacontrol.data.model.BemfaChangeRoomRequest
import com.tomcat927.bemfacontrol.data.model.BemfaTimer
import com.tomcat927.bemfacontrol.data.model.BemfaTimerRequest
import com.tomcat927.bemfacontrol.data.model.BemfaTimerToggleRequest
import com.tomcat927.bemfacontrol.data.model.BemfaRoom
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

    suspend fun modifyName(uid: String, topic: String, name: String) {
        val startedAt = System.currentTimeMillis()
        try {
            val response = api.modifyName(BemfaNameRequest(uid, topic, BemfaApiFactory.tcpDeviceType(), name))
            assertSuccess(response.code, response.message ?: response.msg)
            RuntimeLog.debug("modifyName success: topic=$topic in ${System.currentTimeMillis() - startedAt}ms")
        } catch (throwable: Throwable) {
            RuntimeLog.error("modifyName failed: topic=$topic", throwable)
            throw throwable
        }
    }

    suspend fun rooms(uid: String): List<BemfaRoom> {
        val startedAt = System.currentTimeMillis()
        try {
            val response = api.allRooms(uid, BemfaApiFactory.tcpDeviceType())
            assertSuccess(response.code, response.message ?: response.msg)
            val result = response.data?.rooms.orEmpty()
            RuntimeLog.debug("allRoom success: ${result.size} rooms in ${System.currentTimeMillis() - startedAt}ms")
            return result
        } catch (throwable: Throwable) {
            RuntimeLog.error("allRoom failed", throwable)
            throw throwable
        }
    }

    suspend fun changeRoom(uid: String, topic: String, room: String) {
        val startedAt = System.currentTimeMillis()
        try {
            val response = api.changeTopicRoom(
                BemfaChangeRoomRequest(uid, listOf(topic), BemfaApiFactory.tcpDeviceType(), room)
            )
            assertSuccess(response.code, response.message ?: response.msg)
            RuntimeLog.debug("changeRoom success: topic=$topic room=$room in ${System.currentTimeMillis() - startedAt}ms")
        } catch (throwable: Throwable) {
            RuntimeLog.error("changeRoom failed: topic=$topic", throwable)
            throw throwable
        }
    }

    suspend fun timers(uid: String, topic: String): List<BemfaTimer> {
        val startedAt = System.currentTimeMillis()
        try {
            val response = api.timerList(uid, topic, BemfaApiFactory.tcpDeviceType())
            assertSuccess(response.code, response.message ?: response.msg)
            val result = response.data?.timers.orEmpty()
            RuntimeLog.debug("timerList success: ${result.size} timers in ${System.currentTimeMillis() - startedAt}ms")
            return result
        } catch (throwable: Throwable) {
            RuntimeLog.error("timerList failed: topic=$topic", throwable)
            throw throwable
        }
    }

    suspend fun addTimer(uid: String, topic: String, time: String, msg: String, week: List<Int>) {
        val startedAt = System.currentTimeMillis()
        try {
            val response = api.addTimer(
                BemfaTimerRequest(uid, topic, BemfaApiFactory.tcpDeviceType(), time, msg, week)
            )
            assertSuccess(response.code, response.message ?: response.msg)
            RuntimeLog.debug("addTimer success: topic=$topic time=$time in ${System.currentTimeMillis() - startedAt}ms")
        } catch (throwable: Throwable) {
            RuntimeLog.error("addTimer failed: topic=$topic", throwable)
            throw throwable
        }
    }

    suspend fun toggleTimer(uid: String, topic: String, timerId: Int, enable: Boolean) {
        val startedAt = System.currentTimeMillis()
        try {
            val request = BemfaTimerToggleRequest(uid, topic, BemfaApiFactory.tcpDeviceType(), timerId)
            val response = if (enable) api.enableTimer(request) else api.disableTimer(request)
            assertSuccess(response.code, response.message ?: response.msg)
            RuntimeLog.debug("toggleTimer success: topic=$topic id=$timerId enable=$enable in ${System.currentTimeMillis() - startedAt}ms")
        } catch (throwable: Throwable) {
            RuntimeLog.error("toggleTimer failed: topic=$topic id=$timerId", throwable)
            throw throwable
        }
    }

    suspend fun deleteTimer(uid: String, topic: String, timerId: Int) {
        val startedAt = System.currentTimeMillis()
        try {
            val response = api.deleteTimer(
                BemfaTimerToggleRequest(uid, topic, BemfaApiFactory.tcpDeviceType(), timerId)
            )
            assertSuccess(response.code, response.message ?: response.msg)
            RuntimeLog.debug("deleteTimer success: topic=$topic id=$timerId in ${System.currentTimeMillis() - startedAt}ms")
        } catch (throwable: Throwable) {
            RuntimeLog.error("deleteTimer failed: topic=$topic id=$timerId", throwable)
            throw throwable
        }
    }
}
