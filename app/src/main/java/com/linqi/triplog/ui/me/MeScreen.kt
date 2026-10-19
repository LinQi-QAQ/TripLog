package com.linqi.triplog.ui.me

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linqi.triplog.ui.components.PrimaryButton
import com.linqi.triplog.ui.components.RemindFormat
import com.linqi.triplog.ui.components.SecondaryButton
import com.linqi.triplog.ui.nav.Icons

@Composable
fun MeScreen(
    onOpenAiSettings: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenStats: () -> Unit,
    viewModel: MeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    // 自定义默认提醒时间的输入弹窗
    var showCustomRemind by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) snackbar.showSnackbar("AI 设置已保存")
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("我的", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("AI 识别设置", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "配置一个 OpenAI 兼容接口，用来识别车票截图。Key 只存在本机。",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton(text = "打开 AI 设置", onClick = onOpenAiSettings)
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("默认提醒时间", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "添加行程时默认选中的提前提醒时长。",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    // 预设选项，自定义的分钟数单独判断
                    val remindPresets = listOf(0 to "不提醒", 60 to "1小时", 180 to "3小时", 1440 to "1天")
                    val remindIsCustom = remindPresets.none { it.first == state.defaultRemindMinutes } &&
                        state.defaultRemindMinutes > 0

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        remindPresets.forEach { (min, label) ->
                            FilterChip(
                                selected = state.defaultRemindMinutes == min,
                                onClick = { viewModel.onDefaultRemind(min) },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    FilterChip(
                        selected = remindIsCustom,
                        onClick = { showCustomRemind = true },
                        label = {
                            Text(
                                if (remindIsCustom) {
                                    RemindFormat.label(state.defaultRemindMinutes)
                                } else {
                                    "自定义"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("统计", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "看每月出行次数、交通工具占比、常去城市和总花费。",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton(text = "打开统计", onClick = onOpenStats)
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("数据", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "本机已保存 ${state.tripCount} 条行程，全部存在本地数据库，不上传任何服务器。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton(text = "导出与备份", onClick = onOpenBackup)
                }
            }

            Spacer(Modifier.height(12.dp))

            // 版本号从 BuildConfig 读，避免和 build.gradle 里的 versionName 不同步
            Text(
                "出行记 ${com.linqi.triplog.BuildConfig.VERSION_NAME} · 本地自用版",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    // 自定义默认提醒时间
    if (showCustomRemind) {
        var input by remember { mutableStateOf("") }
        val parsed = RemindFormat.parse(input)

        AlertDialog(
            onDismissRequest = { showCustomRemind = false },
            title = { Text("自定义提醒时间") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "添加行程时默认选中的提前时长，可以用 45分、2小时、1天 这种写法。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("例如 45分钟 或 8小时") },
                        singleLine = true,
                        isError = input.isNotBlank() && parsed == null,
                        supportingText = {
                            when {
                                input.isBlank() -> Text("留空表示取消自定义")
                                parsed == null -> Text("看不懂这个写法")
                                parsed == 0 -> Text("提前时间必须大于 0")
                                parsed > RemindFormat.MAX_MINUTES -> Text("最多提前 30 天")
                                else -> Text("默认提前 ${RemindFormat.label(parsed)} 提醒")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = parsed != null && parsed > 0 && parsed <= RemindFormat.MAX_MINUTES,
                    onClick = {
                        val minutes = parsed ?: return@TextButton
                        viewModel.onDefaultRemind(minutes)
                        showCustomRemind = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRemind = false }) { Text("取消") }
            }
        )
    }
}
