package com.tomcat927.bemfacontrol.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyColumnItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import com.tomcat927.bemfacontrol.data.model.BemfaRoom
import com.tomcat927.bemfacontrol.data.model.BemfaTimer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.mutableStateListOf
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
                    IconButton(onClick = { viewModel.showSettings(true) }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "设置",
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
                    onEditName = { viewModel.showEditNameDialog(true) },
                    onMoveRoom = { viewModel.showMoveRoomDialog(true) },
                    onShowTimer = { viewModel.showTimerPage(true) },
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

            if (state.showSettings) {
                SettingsScreen(
                    state = state,
                    onDismiss = { viewModel.showSettings(false) },
                    onAutoUpdateChange = viewModel::setAutoUpdate,
                    onProxyFirstChange = viewModel::setProxyFirst,
                    onCheckUpdate = viewModel::checkForUpdate,
                    onDownloadUpdate = viewModel::downloadAndInstallUpdate,
                    onDismissUpdate = viewModel::dismissUpdateInfo,
                )
            }
        }

        if (state.editingName) {
            val dev = viewModel.selectedDevice()
            if (dev != null) {
                EditNameDialog(
                    currentName = dev.name,
                    onConfirm = { newName -> viewModel.editDeviceName(dev.topic, newName) },
                    onDismiss = { viewModel.showEditNameDialog(false) },
                )
            }
        }

        if (state.movingRoom) {
            val dev = viewModel.selectedDevice()
            if (dev != null) {
                MoveRoomDialog(
                    rooms = state.roomList,
                    currentRoom = dev.room,
                    onConfirm = { newRoom -> viewModel.moveDeviceToRoom(dev.topic, newRoom) },
                    onDismiss = { viewModel.showMoveRoomDialog(false) },
                )
            }
        }

        if (state.showTimerPage) {
            val dev = viewModel.selectedDevice()
            if (dev != null) {
                TimerPage(
                    device = dev,
                    timers = state.timerList,
                    loading = state.timerLoading,
                    addingTimer = state.addingTimer,
                    onRefresh = viewModel::refreshTimers,
                    onAddTimer = viewModel::addTimer,
                    onToggleTimer = viewModel::toggleTimer,
                    onDeleteTimer = viewModel::deleteTimer,
                    onDismiss = { viewModel.showTimerPage(false) },
                )
            }
        }
    }
}

@Composable
private fun EditNameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑昵称") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("设备昵称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim()) },
                enabled = name.trim().isNotEmpty(),
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun MoveRoomDialog(
    rooms: List<BemfaRoom>,
    currentRoom: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedRoom by remember { mutableStateOf(currentRoom) }
    var showNewRoomInput by remember { mutableStateOf(false) }
    var newRoomName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择房间") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                rooms.forEach { room ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRoom = room.name; showNewRoomInput = false }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedRoom == room.name && !showNewRoomInput,
                            onClick = { selectedRoom = room.name; showNewRoomInput = false },
                        )
                        Text("${room.name} (${room.num})")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showNewRoomInput = true; selectedRoom = "" }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = showNewRoomInput,
                        onClick = { showNewRoomInput = true; selectedRoom = "" },
                    )
                    Text("新建房间")
                }
                if (showNewRoomInput) {
                    OutlinedTextField(
                        value = newRoomName,
                        onValueChange = { newRoomName = it; selectedRoom = it },
                        label = { Text("新房间名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedRoom.trim()) },
                enabled = selectedRoom.trim().isNotEmpty(),
            ) { Text("确认") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerPage(
    device: OutletDevice,
    timers: List<BemfaTimer>,
    loading: Boolean,
    addingTimer: Boolean,
    onRefresh: () -> Unit,
    onAddTimer: (String, String, List<Int>) -> Unit,
    onToggleTimer: (Int, Boolean) -> Unit,
    onDeleteTimer: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    BackHandler { onDismiss() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("定时任务", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !loading) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                    }
                    TextButton(onClick = { showAddDialog = true }) {
                        Text("添加")
                    }
                },
            )
        },
    ) { padding ->
        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (timers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "暂无定时任务",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                lazyColumnItems(timers, key = { it.id }) { timer ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = timer.time,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                )
                                Switch(
                                    checked = timer.isEnabled,
                                    onCheckedChange = { onToggleTimer(timer.id, it) },
                                )
                            }
                            Text(
                                text = "消息: ${timer.msg}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            val weekNames = listOf("日", "一", "二", "三", "四", "五", "六")
                            val weekText = if (timer.week.size == 7) "每天"
                                else if (timer.week.isEmpty()) "不重复"
                                else timer.week.sorted().joinToString(" ") { "周${weekNames[it]}" }
                            Text(
                                text = weekText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(
                                onClick = { onDeleteTimer(timer.id) },
                                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            ) {
                                Text("删除")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTimerDialog(
            onConfirm = { time, msg, week ->
                onAddTimer(time, msg, week)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun AddTimerDialog(
    onConfirm: (String, String, List<Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    var hour by remember { mutableStateOf("22") }
    var minute by remember { mutableStateOf("30") }
    var second by remember { mutableStateOf("00") }
    var msg by remember { mutableStateOf("on") }
    val weekDays = remember { mutableStateListOf(true, true, true, true, true, true, true) }
    val weekLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建定时任务") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { if (it.length <= 2) hour = it.filter { c -> c.isDigit() } },
                        label = { Text("时") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Text(":")
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { if (it.length <= 2) minute = it.filter { c -> c.isDigit() } },
                        label = { Text("分") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Text(":")
                    OutlinedTextField(
                        value = second,
                        onValueChange = { if (it.length <= 2) second = it.filter { c -> c.isDigit() } },
                        label = { Text("秒") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Message toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = msg == "on",
                        onClick = { msg = "on" },
                        label = { Text("开启") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = msg == "off",
                        onClick = { msg = "off" },
                        label = { Text("关闭") },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Week selection
                Text("重复", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    weekLabels.forEachIndexed { index, label ->
                        val dayIndex = if (index == 6) 0 else index + 1
                        FilterChip(
                            selected = weekDays[index],
                            onClick = { weekDays[index] = !weekDays[index] },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val time = "$hour:$minute:$second"
                val week = weekDays.mapIndexedNotNull { index, selected ->
                    if (selected) (if (index == 6) 0 else index + 1) else null
                }
                onConfirm(time, msg, week)
            }) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
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
                lazyRowItems(state.rooms, key = { it }) { room ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceDetailContent(
    device: OutletDevice?,
    detailLoading: Boolean,
    onDismiss: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onCopyTopic: (String) -> Unit,
    onRefresh: () -> Unit,
    onEditName: () -> Unit,
    onMoveRoom: () -> Unit,
    onShowTimer: () -> Unit,
) {
    if (device == null) {
        onDismiss()
        return
    }

    BackHandler { onDismiss() }
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
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onEditName,
                ) { Text("编辑昵称") }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onMoveRoom,
                ) { Text("移动房间") }
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onShowTimer,
            ) { Text("定时任务") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    state: DevicesUiState,
    onDismiss: () -> Unit,
    onAutoUpdateChange: (Boolean) -> Unit,
    onProxyFirstChange: (Boolean) -> Unit,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onDismissUpdate: () -> Unit,
) {
    BackHandler { onDismiss() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("更新", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("启动时检查更新")
                            Switch(checked = state.autoUpdate, onCheckedChange = onAutoUpdateChange)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("加速下载优先")
                            Switch(checked = state.proxyFirst, onCheckedChange = onProxyFirstChange)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onCheckUpdate,
                            enabled = !state.updateChecking,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (state.updateChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("检查更新")
                            }
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("当前版本", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = com.tomcat927.bemfacontrol.BuildConfig.VERSION_NAME,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("项目地址", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "github.com/tomcat927/bemfa-control",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        if (state.updateInfo != null) {
            AlertDialog(
                onDismissRequest = onDismissUpdate,
                title = { Text("发现新版本") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = state.updateInfo!!.versionName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = state.updateInfo!!.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onDownloadUpdate,
                        enabled = !state.updateDownloading,
                    ) {
                        if (state.updateDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("下载并安装")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissUpdate) {
                        Text("稍后")
                    }
                },
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
