package com.tomcat927.bemfacontrol.ui

import com.tomcat927.bemfacontrol.diagnostics.RuntimeLog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tomcat927.bemfacontrol.data.model.DeviceGroup
import com.tomcat927.bemfacontrol.data.repository.BemfaOutletRepository
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
    val onlineCount: Int = 0,
    val totalCount: Int = 0,
    val pendingTopics: Set<String> = emptySet(),
    val message: String? = null,
    val debugEnabled: Boolean = false,
    val showDebugDialog: Boolean = false,
)

class DevicesViewModel(
    private val settingsStore: AppSettingsStore,
    private val outletRepository: BemfaOutletRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevicesUiState(isLoading = true))
    val uiState: StateFlow<DevicesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val enabled = settingsStore.debugLogging()
            RuntimeLog.enabled = enabled
            _uiState.update { it.copy(debugEnabled = enabled) }
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
                        state.copy(
                            isLoading = false,
                            groups = groups,
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
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DevicesViewModel(settingsStore, outletRepository) as T
    }
}
