package com.linqi.triplog.ui.me

import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linqi.triplog.ui.components.FormField
import com.linqi.triplog.ui.components.PrimaryButton
import com.linqi.triplog.ui.components.SecondaryButton
import androidx.compose.material3.OutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onBack: () -> Unit,
    viewModel: MeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 设置", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "任何 OpenAI 兼容端点都能用：OpenAI、DeepSeek、通义、智谱、OpenRouter、自建中转。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "地址填到 /v1 这一层即可，例如 https://api.deepseek.com/v1。模型名必须支持图片输入（如 gpt-4o、gpt-4o-mini、deepseek-vl2 等）。",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            FormField(
                label = "接口地址",
                value = state.aiBaseUrl,
                onValueChange = viewModel::onBaseUrl,
                placeholder = "https://api.openai.com/v1"
            )
            Spacer(Modifier.height(12.dp))
            FormField(
                label = "模型名",
                value = state.aiModel,
                onValueChange = viewModel::onModel,
                placeholder = "gpt-4o-mini"
            )
            Spacer(Modifier.height(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("API Key", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = state.aiApiKey,
                    onValueChange = viewModel::onApiKey,
                    placeholder = { Text("sk-...") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "加密存储在本机，不明文落盘。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(
                    text = if (state.testing) "测试中…" else "测试连接",
                    onClick = { viewModel.testConnection() },
                    enabled = !state.testing,
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = "保存",
                    onClick = { viewModel.save() },
                    modifier = Modifier.weight(1f)
                )
            }

            if (state.testing) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("正在请求…", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (state.testResult.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    state.testResult,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.testResult.startsWith("连接成功")) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
