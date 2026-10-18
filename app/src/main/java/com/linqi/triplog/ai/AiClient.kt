package com.linqi.triplog.ai

import android.util.Base64
import com.linqi.triplog.data.local.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * OpenAI 兼容 chat completions 客户端，用于识别车票截图。
 * baseUrl / model / apiKey 全部来自本机配置，可指向任何兼容端点
 * （OpenAI、DeepSeek、通义、智谱、OpenRouter、自建中转等）。
 */
class AiClient(private val settings: SettingsStore) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    sealed interface Result {
        data class Success(val info: TicketInfo, val raw: String) : Result
        data class Failure(val message: String) : Result
    }

    suspend fun recognizeTicket(imagePath: String): Result = withContext(Dispatchers.IO) {
        if (!settings.aiConfigured()) {
            return@withContext Result.Failure("AI 未配置，请先在「我的 - AI 设置」里填接口地址、模型名和 API Key")
        }
        val file = File(imagePath)
        if (!file.exists()) {
            return@withContext Result.Failure("图片不存在：$imagePath")
        }

        try {
            val bytes = file.readBytes()
            if (bytes.size > 8 * 1024 * 1024) {
                return@withContext Result.Failure("图片太大（${bytes.size / 1024}KB），请换一张更小的截图")
            }
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val dataUrl = "data:image/jpeg;base64,$base64"

            val payload = buildJsonObject {
                put("model", settings.aiModel)
                put("temperature", 0)
                put("max_tokens", 800)
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "system")
                        put("content", settings.aiSystemPrompt)
                    })
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", buildJsonArray {
                            add(buildJsonObject {
                                put("type", "text")
                                put("text", USER_PROMPT)
                            })
                            add(buildJsonObject {
                                put("type", "image_url")
                                put("image_url", buildJsonObject { put("url", dataUrl) })
                            })
                        })
                    })
                })
            }

            val baseUrl = settings.aiBaseUrl.trimEnd('/')
            val url = if (baseUrl.endsWith("/chat/completions")) baseUrl else "$baseUrl/chat/completions"

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${settings.aiApiKey}")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.Failure("接口返回 ${response.code}：${body.take(300)}")
                }
                val content = extractContent(body)
                    ?: return@withContext Result.Failure("接口返回格式无法解析：${body.take(300)}")
                parseTicket(content)
            }
        } catch (e: Exception) {
            Result.Failure("请求失败：${e.javaClass.simpleName} ${e.message ?: ""}".trim())
        }
    }

    private fun extractContent(body: String): String? {
        return try {
            val obj = json.parseToJsonElement(body) as? JsonObject ?: return null
            val choices = obj["choices"] as? kotlinx.serialization.json.JsonArray ?: return null
            val first = choices.firstOrNull() as? JsonObject ?: return null
            val message = first["message"] as? JsonObject ?: return null
            val content = message["content"]
            when (content) {
                is kotlinx.serialization.json.JsonPrimitive -> content.content
                is kotlinx.serialization.json.JsonArray -> {
                    content.mapNotNull { part ->
                        (part as? JsonObject)?.get("text")
                            ?.let { it as? kotlinx.serialization.json.JsonPrimitive }?.content
                    }.joinToString("\n")
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTicket(content: String): Result {
        val cleaned = stripFence(content)
        // 先用宽松解析，字段类型抽风也能尽量捞回来
        val info = TicketInfo.parseLenient(cleaned)
        return if (info != null) {
            Result.Success(info, content)
        } else {
            Result.Failure("模型返回的不是合法 JSON，已保留原文：\n${content.take(500)}")
        }
    }

    companion object {
        const val USER_PROMPT =
            "请识别这张车票截图，提取下面字段并以 JSON 返回：" +
                "vehicle_type（train/flight/bus/other）、from_station、to_station、" +
                "depart_time（YYYY-MM-DDTHH:mm）、arrive_time（同格式）、" +
                "number（车次号或航班号）、seat、price、passenger、note。" +
                "不要输出 JSON 以外的任何内容。"

        /** 剥掉 ```json 围栏，并截取第一个 { 到最后一个 }。 */
        fun stripFence(text: String): String {
            var t = text.trim()
            if (t.startsWith("```")) {
                t = t.removePrefix("```")
                val nl = t.indexOf('\n')
                if (nl in 0 until t.length) t = t.substring(nl + 1)
                val end = t.lastIndexOf("```")
                if (end >= 0) t = t.substring(0, end)
            }
            t = t.trim()
            val start = t.indexOf('{')
            val end = t.lastIndexOf('}')
            return if (start >= 0 && end > start) t.substring(start, end + 1) else t
        }
    }
}
