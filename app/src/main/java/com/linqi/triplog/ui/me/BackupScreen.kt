package com.linqi.triplog.ui.me

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.linqi.triplog.data.backup.BackupManager
import com.linqi.triplog.ui.components.PrimaryButton
import com.linqi.triplog.ui.components.SecondaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var confirmRestore by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupManager.MIME)
    ) { uri ->
        if (uri != null) viewModel.export(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            confirmRestore = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导出与备份", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
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
                .padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("当前数据", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "本机存了 ${state.tripCount} 条行程，其中 ${state.imageCount} 条带车票图。",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "全部在本地数据库和私有目录，没有任何云端同步。换机或卸载前请先导出。",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("导出", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "导出成一个 zip 文件，包含全部行程数据和车票图。选好保存位置即可，建议放到 Downloads 或云盘。",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                text = "导出备份",
                onClick = {
                    exportLauncher.launch(BackupManager.suggestedFileName())
                },
                enabled = !state.busy && state.tripCount > 0
            )

            Spacer(Modifier.height(24.dp))

            Text("恢复", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "从之前导出的 zip 恢复。注意：恢复会清空当前所有行程，再用备份里的数据替换。",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                text = "选择备份文件恢复",
                onClick = { importLauncher.launch(arrayOf(BackupManager.MIME)) },
                enabled = !state.busy
            )

            if (state.busy) {
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).width(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.height(0.dp))
                    Text(
                        "  处理中…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.message.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (confirmRestore && pendingImportUri != null) {
        val uri = pendingImportUri!!
        AlertDialog(
            onDismissRequest = {
                confirmRestore = false
                pendingImportUri = null
            },
            title = { Text("确认恢复") },
            text = {
                Text(
                    "恢复会先清空当前全部 ${state.tripCount} 条行程，再用备份文件里的数据替换，此操作不可撤销。确定继续吗？"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    viewModel.import(uri)
                    pendingImportUri = null
                }) { Text("确定恢复") }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    pendingImportUri = null
                }) { Text("取消") }
            }
        )
    }
}
