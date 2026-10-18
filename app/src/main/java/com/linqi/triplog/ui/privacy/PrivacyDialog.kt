package com.linqi.triplog.ui.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 首次启动的隐私政策弹窗。
 *
 * 设计成不可取消（返回键和点击外部都不关闭），必须点「同意并继续」，
 * 避免用户误触跳过导致后续出了问题说不清。不同意就退出应用。
 */
@Composable
fun PrivacyDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* 禁止点击外部关闭 */ },
        title = {
            Text(
                text = "隐私政策",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                PrivacySection(
                    heading = "数据存在哪",
                    body = "你的行程、车票原图、提醒设置全部保存在这台手机的应用私有目录里，" +
                        "不上传到任何服务器，我们也拿不到。卸载应用会一并删除。"
                )
                PrivacySection(
                    heading = "AI 识别会上传图片",
                    body = "只有使用「识别车票」功能时，你选中的那张图片会经过 base64 编码后，" +
                        "发送到你自己在设置里填写的 AI 接口提供方（例如 DeepSeek、OpenAI）。" +
                        "这是识别所必需的，不发送就无法提取车票信息。\n\n" +
                        "请勿在识别时使用包含个人敏感信息的图片。提供方如何处理这些数据，" +
                        "请查阅对应服务商的隐私政策。"
                )
                PrivacySection(
                    heading = "权限用途",
                    body = "联网：仅用于调用你配置的 AI 接口。\n" +
                        "通知与闹钟：用于出发前提醒你。\n" +
                        "存储：用于保存车票原图，仅在本应用内可见。"
                )
                PrivacySection(
                    heading = "第三方服务",
                    body = "本应用不含广告、统计、崩溃上报等任何第三方 SDK。"
                )
                PrivacySection(
                    heading = "你的选择",
                    body = "你可以随时在「我的」里清除 AI 配置或导出删除全部数据。" +
                        "不同意上述条款将无法使用本应用。"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text("同意并继续")
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("不同意并退出")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PrivacySection(heading: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(
            text = heading,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
