package com.tomcat927.bemfacontrol.ui

import com.tomcat927.bemfacontrol.data.model.OutletDevice
import com.tomcat927.bemfacontrol.diagnostics.RuntimeLog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tomcat927.bemfacontrol.data.model.DeviceGroup
import com.tomcat927.bemfacontrol.data.repository.BemfaOutletRepository
import com.tomcat927.bemfacontrol.data.repository.UpdateRepository
import com.tomcat927.bemfacontrol.data.model.ReleaseInfo
import com.tomcat927.bemfacontrol.BuildConfig
import com.tomcat927.bemfacontrol.data.settings.AppSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DevicesUiState(
    val isLoading: Boolean = false,
    val needsSetup: Boolean = false,
    val uid: String = "",
    val groups: List<DeviceGroup> = emptyList(),
    val allDevices: List<OutletDevice> = emptyList(),
    val rooms: List<String> = emptyList(),
    val selectedRoom: String? = null,
    val viewMode: ViewMode = ViewMode.ROOM,
    val selectedDeviceTopic: String? = null,
    val lastSyncTime: String? = null,
    val onlineCount: Int = 0,
    val totalCount: Int = 0,
    val pendingTopics: Set<String> = emptySet(),
    val message: String? = null,
    val debugEnabled: Boolean = false,
    val showDebugDialog: Boolean = false,
    val detailLoading: Boolean = false,
    val showSettings: Boolean = false,
    val autoUpdate: Boolean = true,
    val proxyFirst: Boolean = true,
    val updateChecking: Boolean = false,
    val updateInfo: ReleaseInfo? = null,
    val updateDownloading: Boolean = false,
    val updateDownloadProgress: Float = 0f,
)

enum class ViewMode { ROOM, DEVICE }

class DevicesViewModel(
    private val settingsStore: AppSettingsStore,
    private val outletRepository: BemfaOutletRepository,
    private val updateRepository: UpdateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevicesUiState(isLoading = true))
    val uiState: StateFlow<DevicesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val enabled = settingsStore.debugLogging()
            RuntimeLog.enabled = enabled
            _uiState.update { it.copy(debugEnabled = enabled, autoUpdate = settingsStore.autoUpdate(), proxyFirst = settingsStore.proxyFirst()) }
            if (settingsStore.autoUpdate()) {
                checkForUpdate()
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            val uid = settingsStore.uid()
            if (uid.isBlank()) {
                _uiState.update {
                    it.copy(isLoading = false, needsSetup = true, groups = emptyList())
                }
                return@launch
            }

            _uiState.update { it.copy(needsSetup = false, uid = uid) }
            runCatching { outletRepository.groups(uid) }
                .onSuccess { groups ->
                    RuntimeLog.debug("sync success: ${groups.sumOf { it.devices.size }} devices")
                    _uiState.update { state ->
                        val allDevices = groups.flatMap { it.devices }
                        val rooms = groups.map { it.room }.distinct()
                        state.copy(
                            isLoading = false,
                            groups = groups,
                            allDevices = allDevices,
                            rooms = rooms,
                            lastSyncTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.CHINA).format(java.util.Date()),
                            totalCount = groups.sumOf { group -> group.devices.size },
                            onlineCount = groups.sumOf { group ->
                                group.devices.count { device -> device.isOnline }
                            },
                            message = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    RuntimeLog.error("sync failed", throwable)
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            message = throwable.message ?: "设备同步失败",
                        )
                    }
                }
        }
    }

    fun configureUid(value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(message = "请输入巴法云用户私钥 UID") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            runCatching { settingsStore.setUid(trimmed) }
                .onSuccess {
                    RuntimeLog.info("configureUid: saved UID")
                    _uiState.update { state -> state.copy(uid = trimmed, needsSetup = false) }
                    refresh()
                }
                .onFailure { throwable ->
                    RuntimeLog.error("configureUid: save failed", throwable)
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            message = throwable.message ?: "保存配置失败",
                        )
                    }
                }
        }
    }

    fun setPower(topic: String, on: Boolean) {
        val uid = _uiState.value.uid
        if (uid.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(pendingTopics = it.pendingTopics + topic, message = null)
            }
            runCatching { outletRepository.setPower(uid, topic, on) }
                .onSuccess {
                    RuntimeLog.debug("setPower sent: topic=$topic")
                    updateLocalPower(topic, on)
                    refresh()
                }
                .onFailure { throwable ->
                    RuntimeLog.error("setPower request failed: topic=$topic", throwable)
                    _uiState.update { state ->
                        state.copy(
                            pendingTopics = state.pendingTopics - topic,
                            message = throwable.message ?: "控制指令发送失败",
                        )
                    }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun selectRoom(room: String?) {
        _uiState.update { it.copy(selectedRoom = room) }
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode, selectedRoom = null) }
    }

    fun selectDevice(topic: String?) {
        _uiState.update { it.copy(selectedDeviceTopic = topic) }
        if (topic != null) refreshDeviceDetail()
    }

    fun refreshDeviceDetail() {
        val uid = _uiState.value.uid
        if (uid.isBlank()) return
        val topic = _uiState.value.selectedDeviceTopic ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(detailLoading = true) }
            runCatching { outletRepository.checkOnline(uid, topic) }
                .onSuccess { online ->
                    _uiState.update { state ->
                        state.copy(
                            detailLoading = false,
                            allDevices = state.allDevices.map { device ->
                                if (device.topic == topic) device.copy(isOnline = online) else device
                            },
                        )
                    }
                }
                .onFailure { throwable ->
                    RuntimeLog.error("detail online check failed: topic=$topic", throwable)
                    _uiState.update { it.copy(detailLoading = false) }
                }
        }
    }

    fun selectedDevice(): OutletDevice? =
        _uiState.value.allDevices.find { it.topic == _uiState.value.selectedDeviceTopic }

    fun showDebugDialog(visible: Boolean) {
        _uiState.update { it.copy(showDebugDialog = visible) }
    }

    fun setDebugLogging(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { settingsStore.setDebugLogging(enabled) }
                .onSuccess {
                    RuntimeLog.enabled = enabled
                    if (enabled) RuntimeLog.info("debug logging enabled")
                    _uiState.update { it.copy(debugEnabled = enabled) }
                }
                .onFailure { throwable ->
                    RuntimeLog.error("debug logging save failed", throwable)
                    _uiState.update { it.copy(message = "保存日志设置失败") }
                }
        }
    }

    fun clearLogs() {
        RuntimeLog.clear()
        _uiState.update { it.copy(showDebugDialog = true) }
    }

    fun showSettings(visible: Boolean) {
        _uiState.update { it.copy(showSettings = visible) }
    }

    fun setAutoUpdate(value: Boolean) {
        viewModelScope.launch {
            settingsStore.setAutoUpdate(value)
            _uiState.update { it.copy(autoUpdate = value) }
        }
    }

    fun setProxyFirst(value: Boolean) {
        viewModelScope.launch {
            settingsStore.setProxyFirst(value)
            _uiState.update { it.copy(proxyFirst = value) }
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.update { it.copy(updateChecking = true) }
            val info = updateRepository.checkForUpdate(BuildConfig.VERSION_CODE)
            _uiState.update { it.copy(updateChecking = false, updateInfo = info) }
        }
    }

    fun downloadAndInstallUpdate() {
        val info = _uiState.value.updateInfo ?: return
        val useProxy = _uiState.value.proxyFirst
        viewModelScope.launch {
            _uiState.update { it.copy(updateDownloading = true, updateDownloadProgress = 0f) }
            val success = updateRepository.downloadAndInstall(info, useProxy) { progress ->
                _uiState.update { it.copy(updateDownloadProgress = progress) }
            }
            _uiState.update { it.copy(updateDownloading = false) }
            if (!success) {
                _uiState.update { it.copy(message = "下载更新失败，请尝试切换下载线路") }
            }
        }
    }

    fun dismissUpdateInfo() {
        _uiState.update { it.copy(updateInfo = null) }
    }

    private fun updateLocalPower(topic: String, on: Boolean) {
        _uiState.update { state ->
            state.copy(
                pendingTopics = state.pendingTopics - topic,
                groups = state.groups.map { group ->
                    group.copy(
                        devices = group.devices.map { device ->
                            if (device.topic == topic) device.copy(isOn = on) else device
                        },
                    )
                },
            )
        }
    }

    class Factory(
        private val settingsStore: AppSettingsStore,
        private val outletRepository: BemfaOutletRepository,
        private val updateRepository: UpdateRepository,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DevicesViewModel(settingsStore, outletRepository, updateRepository) as T
    }
}
