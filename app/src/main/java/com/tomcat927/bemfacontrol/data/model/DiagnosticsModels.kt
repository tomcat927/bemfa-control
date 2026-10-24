package com.tomcat927.bemfacontrol.data.model

import kotlinx.serialization.Serializable

/**
 * 插座固件（chazuo main.ino dispatchTcpMessage）支持的诊断指令。
 * 查询类设备回复到 <topic>/up，巴法云把 /up 上报同步进主 topic 的 msg，
 * app 用 getmsg 拉主 topic 最新消息即可拿到回复。
 * 动作类只触发动作，部分有状态回复。
 */
@Serializable
enum class DeviceCommand(
    val msg: String,
    val label: String,
    val isAction: Boolean,
) {
    GET_VERSION("get_version", "固件版本", false),
    GET_WIFI("get_wifi", "WiFi 信息", false),
    GET_MAC("get_mac", "MAC 地址", false),
    GET_CURRENT_TIME("get_current_time", "当前时间", false),
    GET_TIMERS_LOADED("get_timers_loaded", "定时加载状态", false),
    REBOOT("reboot", "重启设备", true),
    UPDATE("update", "OTA 升级", true),
    TIMER_GET("timer_get", "拉取云端定时", true),
    NTP_SYNC("ntp_sync", "同步 NTP 时间", true);

    /** 设备回复 msg 的前缀，用于轮询时区分查询回复与开关状态/下发命令本身。 */
    val replyPrefix: String
        get() = when (this) {
            GET_VERSION -> "version:"
            GET_MAC -> "MAC:"
            GET_WIFI -> "ssid:"
            GET_CURRENT_TIME -> "time:"
            GET_TIMERS_LOADED -> "timers_loaded:"
            UPDATE -> "update_started"
            NTP_SYNC -> "ntp_sync_"
            REBOOT, TIMER_GET -> ""
        }

    /** 是否有可识别的回复（决定要不要轮询等回复）。 */
    val hasReply: Boolean
        get() = replyPrefix.isNotEmpty()
}

/** 单条诊断指令的执行结果。 */
@Serializable
data class DiagResult(
    val command: DeviceCommand,
    val raw: String,
    val time: String?,
    val unix: Long?,
    val fields: Map<String, String>,
    val success: Boolean,
) {
    val isTimeout: Boolean
        get() = !success && raw.isEmpty()
}

/**
 * 解析固件回复文本。固件回复是自定义管道分隔格式，例如：
 * version:20260906
 * MAC:XX:XX:XX:XX:XX:XX
 * ssid:MyWiFi|rssi:-52|ip:192.168.1.10
 * time:14:30:45|timestamp:1738823445|synced:1
 * timers_loaded:true|count:3
 */
object DiagParser {

    fun parse(raw: String): Map<String, String> {
        if (raw.isEmpty()) return emptyMap()
        val fields = LinkedHashMap<String, String>()
        // MAC:XX:XX:.. 这种值本身含冒号，按第一个冒号切 key/value
        for (part in raw.split("|")) {
            val idx = part.indexOf(':')
            if (idx > 0) {
                fields[part.substring(0, idx).trim()] = part.substring(idx + 1).trim()
            }
        }
        return fields
    }

    fun isReplyTo(command: DeviceCommand, msg: String): Boolean {
        val prefix = command.replyPrefix
        return prefix.isNotEmpty() && msg.startsWith(prefix)
    }

    /** UI 按固定顺序展示的字段名。 */
    fun displayKeys(command: DeviceCommand): List<String> = when (command) {
        DeviceCommand.GET_VERSION -> listOf("version")
        DeviceCommand.GET_MAC -> listOf("MAC")
        DeviceCommand.GET_WIFI -> listOf("ssid", "rssi", "ip")
        DeviceCommand.GET_CURRENT_TIME -> listOf("time", "timestamp", "synced")
        DeviceCommand.GET_TIMERS_LOADED -> listOf("timers_loaded", "count")
        else -> emptyList()
    }
}
