package com.tomcat927.bemfacontrol.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomcat927.bemfacontrol.data.model.OutletDevice
import com.tomcat927.bemfacontrol.diagnostics.RuntimeLog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DevicesScreen(viewModel: DevicesViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("巴法智控") },
                actions = {
                    if (state.lastSyncTime != null) {
                        Text(
                            text = state.lastSyncTime!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                    TextButton(onClick = { viewModel.showDebugDialog(true) }) {
                        Text(
                            text = "日志",
                            color = if (state.debugEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    TextButton(onClick = viewModel::refresh) {
                        Text("刷新")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.needsSetup -> SetupContent(
                    isLoading = state.isLoading,
                    onConfigure = viewModel::configureUid,
                )

                state.selectedDeviceTopic != null -> DeviceDetailContent(
                    device = viewModel.selectedDevice(),
                    detailLoading = state.detailLoading,
                    onDismiss = { viewModel.selectDevice(null) },
                    onToggle = viewModel::setPower,
                    onCopyTopic = { topic ->
                        clipboard.setPrimaryClip(ClipData.newPlainText("topic", topic))
                        Toast.makeText(context, "topic 已复制", Toast.LENGTH_SHORT).show()
                    },
                    onRefresh = viewModel::refreshDeviceDetail,
                )

                else -> DeviceListContent(
                    state = state,
                    onToggle = viewModel::setPower,
                    onRefresh = viewModel::refresh,
                    onSelectRoom = viewModel::selectRoom,
                    onViewModeChange = viewModel::setViewMode,
                    onDeviceClick = viewModel::selectDevice,
                    onCopyTopic = { topic ->
                        clipboard.setPrimaryClip(ClipData.newPlainText("topic", topic))
                        Toast.makeText(context, "topic 已复制", Toast.LENGTH_SHORT).show()
                    },
                )
            }

            if (state.showDebugDialog) {
                DebugDialog(
                    enabled = state.debugEnabled,
                    onEnabledChange = viewModel::setDebugLogging,
                    onCopy = {
                        val text = RuntimeLog.snapshot().joinToString(separator = "\n")
                        clipboard.setPrimaryClip(ClipData.newPlainText("Bemfa Runtime Logs", text))
                    },
                    onClear = viewModel::clearLogs,
                    onDismiss = { viewModel.showDebugDialog(false) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DeviceListContent(
    state: DevicesUiState,
    onToggle: (String, Boolean) -> Unit,
    onRefresh: () -> Unit,
    onSelectRoom: (String?) -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onDeviceClick: (String) -> Unit,
    onCopyTopic: (String) -> Unit,
) {
    val displayDevices = when {
        state.viewMode == ViewMode.DEVICE -> state.allDevices
        state.selectedRoom != null -> state.allDevices.filter { it.room == state.selectedRoom }
        else -> state.allDevices
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Stats bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "共 ${state.totalCount} 个 · 在线 ${state.onlineCount}",
                style = MaterialTheme.typography.titleSmall,
            )
            TextButton(onClick = onRefresh, enabled = !state.isLoading) {
                Text("同步")
            }
        }

        // View mode segmented buttons
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            SegmentedButton(
                selected = state.viewMode == ViewMode.ROOM,
                onClick = { onViewModeChange(ViewMode.ROOM) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) {
                Text("房间")
            }
            SegmentedButton(
                selected = state.viewMode == ViewMode.DEVICE,
                onClick = { onViewModeChange(ViewMode.DEVICE) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) {
                Text("设备")
            }
        }

        // Room filter row (only in ROOM mode)
        if (state.viewMode == ViewMode.ROOM && state.rooms.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.selectedRoom == null,
                        onClick = { onSelectRoom(null) },
                        label = { Text("全部") },
                    )
                }
                items(state.rooms, key = { it }) { room ->
                    FilterChip(
                        selected = state.selectedRoom == room,
                        onClick = { onSelectRoom(room) },
                        label = { Text(room) },
                    )
                }
            }
        }

        if (displayDevices.isEmpty()) {
            EmptyContent(isLoading = state.isLoading)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(displayDevices, key = { it.topic }) { device ->
                    OutletGridCard(
                        device = device,
                        isPending = device.topic in state.pendingTopics,
                        onToggle = onToggle,
                        onClick = { onDeviceClick(device.topic) },
                        onLongPress = { onCopyTopic(device.topic) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OutletGridCard(
    device: OutletDevice,
    isPending: Boolean,
    onToggle: (String, Boolean) -> Unit,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Name - up to 2 lines, no truncation
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            // Topic - full display, small monospace
            Text(
                text = device.topic,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            )

            // Status + time row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val statusText = when {
                    isPending -> "处理中"
                    !device.isOnline -> "离线"
                    device.isOn -> "开启"
                    else -> "关闭"
                }
                val statusColor = when {
                    !device.isOnline -> MaterialTheme.colorScheme.error
                    device.isOn -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                )
                device.lastMessageTime?.let { time ->
                    val timeOnly = time.substringAfter(' ').take(5)
                    Text(
                        text = "  $timeOnly",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Circular power button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (device.isOn && device.isOnline && !isPending) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        }
                    )
                    .border(
                        width = 2.dp,
                        color = when {
                            !device.isOnline -> MaterialTheme.colorScheme.outlineVariant
                            device.isOn -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape,
                    )
                    .clickable(enabled = !isPending && device.isOnline) {
                        onToggle(device.topic, !device.isOn)
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (isPending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = "电源",
                        tint = when {
                            !device.isOnline -> MaterialTheme.colorScheme.outlineVariant
                            device.isOn -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceDetailContent(
    device: OutletDevice?,
    onDismiss: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onCopyTopic: (String) -> Unit,
) {
    if (device == null) {
        onDismiss()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(device.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !detailLoading) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "刷新",
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (detailLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = device.topic,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onCopyTopic(device.topic) }) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "复制",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            HorizontalDivider()
            DetailText("房间", device.room)
            DetailText("在线状态", if (device.isOnline) "在线" else "离线")
            DetailText("当前状态", if (device.isOn) "开启" else "关闭")
            DetailText("最近消息时间", device.lastMessageTime ?: "无")
            HorizontalDivider()
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            if (device.isOn && device.isOnline) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            }
                        )
                        .border(
                            width = 2.dp,
                            color = when {
                                !device.isOnline -> MaterialTheme.colorScheme.outlineVariant
                                device.isOn -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            },
                            shape = CircleShape,
                        )
                        .clickable(enabled = device.isOnline) {
                            onToggle(device.topic, !device.isOn)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = "电源",
                        tint = when {
                            !device.isOnline -> MaterialTheme.colorScheme.outlineVariant
                            device.isOn -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            Text(
                text = if (!device.isOnline) "设备离线" else if (device.isOn) "已开启" else "已关闭",
                style = MaterialTheme.typography.bodyLarge,
                color = when {
                    !device.isOnline -> MaterialTheme.colorScheme.error
                    device.isOn -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    onCopy: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )
        }
        TextButton(onClick = { onCopy(value) }) {
            Text("复制")
        }
    }
}

@Composable
private fun DetailText(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun DebugDialog(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val logs by RuntimeLog.entries.collectAsStateWithLifecycle()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("调试日志") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (enabled) "已开启" else "已关闭")
                    Switch(checked = enabled, onCheckedChange = onEnabledChange)
                }
                Text(
                    text = "开启后记录接口调用、错误和异常摘要。日志不会包含 UID。",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (logs.isEmpty()) {
                    Text("暂无日志")
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        logs.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCopy()
                    onDismiss()
                },
            ) {
                Text("复制日志")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onClear) {
                    Text("清空")
                }
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        },
    )
}

@Composable
private fun SetupContent(
    isLoading: Boolean,
    onConfigure: (String) -> Unit,
) {
    var uid by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "配置巴法云",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "粘贴控制台中的用户私钥 UID，仅保存在本机。",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = uid,
            onValueChange = { uid = it },
            label = { Text("用户私钥 UID") },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onConfigure(uid) },
            enabled = !isLoading && uid.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("开始使用")
        }
        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun EmptyContent(isLoading: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isLoading -> CircularProgressIndicator()
            else -> Text(
                text = "没有找到 TCP 插座。\n请确认巴法云主题类型为 TCP 且命名以 001 结尾。",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
