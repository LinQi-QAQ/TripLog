package com.linqi.triplog.ui.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linqi.triplog.data.local.VehicleType
import com.linqi.triplog.remind.departLabel
import com.linqi.triplog.ui.components.FormField
import com.linqi.triplog.ui.components.LoadingBox
import com.linqi.triplog.ui.components.PrimaryButton
import com.linqi.triplog.ui.components.VehicleIcon
import com.linqi.triplog.ui.components.vehicleName
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEditScreen(
    tripId: Long?,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    viewModel: TripEditViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val form = state.form

    LaunchedEffect(tripId) { viewModel.load(tripId) }
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    var pendingTimeField by remember { mutableStateOf<String?>(null) }
    var pendingTimeTargetDepart by remember { mutableStateOf(true) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) viewModel.recognize(uri)
    }

    fun pickImage() {
        pickMedia.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (form.id == 0L) "添加行程" else "编辑行程", style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    TextButton(onClick = onCancel) {
                        Text("取消")
                    }
                },
                actions = {
                    if (form.id != 0L) {
                        TextButton(onClick = { viewModel.deleteCurrent() }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // AI 识别区
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            com.linqi.triplog.ui.nav.Icons.sparkle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (form.id == 0L) "识别车票截图，自动填表" else "重新识别车票截图",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (state.aiConfigured) "选一张车票截图，AI 会提取车次、时间、站点" else "AI 未配置，去「我的 - AI 设置」填接口信息后才能用",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    if (state.recognizing) {
                        LoadingBox("正在识别，请稍候…")
                    } else {
                        PrimaryButton(
                            text = if (form.ticketImagePath.isBlank()) "选择截图识别" else "换一张截图",
                            onClick = { pickImage() },
                            enabled = state.aiConfigured
                        )
                    }
                    if (state.recognizeError.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            state.recognizeError,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (state.recognizeSuccess) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "识别完成，请核对下面的信息后再保存",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (form.ticketImagePath.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "已附带车票图",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 交通工具
            Text("交通工具", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VehicleType.all.forEach { type ->
                    FilterChip(
                        selected = form.vehicleType == type,
                        onClick = { viewModel.update { it.copy(vehicleType = type) } },
                        label = { Text(vehicleName(type), style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = { VehicleIcon(type, size = 16.dp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            FormField(
                label = "出发地",
                value = form.fromStation,
                onValueChange = { v -> viewModel.update { it.copy(fromStation = v) } },
                placeholder = "上海虹桥",
                required = true
            )
            Spacer(Modifier.height(12.dp))
            FormField(
                label = "目的地",
                value = form.toStation,
                onValueChange = { v -> viewModel.update { it.copy(toStation = v) } },
                placeholder = "北京南",
                required = true
            )
            Spacer(Modifier.height(12.dp))

            // 出发时间
            TimeRow(
                label = "出发时间",
                value = form.departTime?.let { formatMillis(it) } ?: "未设置",
                required = true,
                onClick = { pendingTimeField = "depart" }
            )
            Spacer(Modifier.height(12.dp))
            TimeRow(
                label = "到达时间",
                value = form.arriveTime?.let { formatMillis(it) } ?: "未设置",
                onClick = { pendingTimeField = "arrive" }
            )

            Spacer(Modifier.height(16.dp))
            Text("其他信息（可不填）", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FormField(
                label = "车次 / 航班号",
                value = form.number,
                onValueChange = { v -> viewModel.update { it.copy(number = v) } },
                placeholder = "G1234"
            )
            Spacer(Modifier.height(12.dp))
            FormField(
                label = "座位",
                value = form.seat,
                onValueChange = { v -> viewModel.update { it.copy(seat = v) } },
                placeholder = "08车 12A"
            )
            Spacer(Modifier.height(12.dp))
            FormField(
                label = "票价",
                value = form.price,
                onValueChange = { v -> viewModel.update { it.copy(price = v) } },
                placeholder = "553"
            )
            Spacer(Modifier.height(12.dp))
            FormField(
                label = "乘车人",
                value = form.passenger,
                onValueChange = { v -> viewModel.update { it.copy(passenger = v) } },
                placeholder = "林祺"
            )
            Spacer(Modifier.height(12.dp))
            FormField(
                label = "备注",
                value = form.note,
                onValueChange = { v -> viewModel.update { it.copy(note = v) } },
                placeholder = "",
                singleLine = false
            )

            Spacer(Modifier.height(16.dp))

            // 提醒
            Text("出发提醒", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "不提醒", 60 to "1小时", 180 to "3小时", 1440 to "1天").forEach { (min, label) ->
                    FilterChip(
                        selected = form.remindBeforeMinutes == min,
                        onClick = { viewModel.update { it.copy(remindBeforeMinutes = min) } },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = "保存",
                onClick = { viewModel.save() },
                enabled = form.valid
            )
            if (!form.valid) {
                Text(
                    "出发地、目的地、出发时间为必填",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    // 时间选择：先选日期，再选时间
    if (pendingTimeField != null) {
        val isDepart = pendingTimeField == "depart"
        val dateState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { pendingTimeField = null },
            confirmButton = {
                TextButton(onClick = {
                    pendingDateMillis = dateState.selectedDateMillis
                    pendingTimeTargetDepart = isDepart
                    pendingTimeField = "time"
                }) { Text("下一步") }
            },
            dismissButton = { TextButton(onClick = { pendingTimeField = null }) { Text("取消") } }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (pendingTimeField == "time" && pendingDateMillis != null) {
        val base = pendingDateMillis!!
        val targetDepart = pendingTimeTargetDepart
        val timeState = rememberTimePickerState(is24Hour = true)
        AlertDialog(
            onDismissRequest = { pendingTimeField = null; pendingDateMillis = null },
            confirmButton = {
                TextButton(onClick = {
                    val hour = timeState.hour
                    val minute = timeState.minute
                    val localDate = Instant.ofEpochMilli(base).atZone(ZoneId.of("UTC")).toLocalDate()
                    val result = localDate.atTime(hour, minute)
                        .atZone(ZoneId.systemDefault())
                        .toInstant().toEpochMilli()
                    if (targetDepart) {
                        viewModel.update { it.copy(departTime = result) }
                    } else {
                        viewModel.update { it.copy(arriveTime = result) }
                    }
                    pendingTimeField = null
                    pendingDateMillis = null
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { pendingTimeField = null; pendingDateMillis = null }) { Text("取消") } },
            title = { Text("选择时间") },
            text = { TimePicker(state = timeState) }
        )
    }
}

@Composable
private fun TimeRow(label: String, value: String, required: Boolean = false, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            if (required) "$label *" else label,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onClick() }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                color = if (value == "未设置") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatMillis(millis: Long): String {
    val dt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}
