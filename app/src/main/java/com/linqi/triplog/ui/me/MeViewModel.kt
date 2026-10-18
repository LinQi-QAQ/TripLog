package com.linqi.triplog.ui.me

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linqi.triplog.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class MeUiState(
    val aiBaseUrl: String = "",
    val aiModel: String = "",
    val aiApiKey: String = "",
    val defaultRemindMinutes: Int = 180,
    val tripCount: Int = 0,
    val testing: Boolean = false,
    val testResult: String = "",
    val saved: Boolean = false
)

class MeViewModel(app: Application) : AndroidViewModel(app) {

    private val container = AppContainer.of(app)
    private val settings = container.settings
    private val testClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _state = MutableStateFlow(
        MeUiState(
            aiBaseUrl = settings.aiBaseUrl,
            aiModel = settings.aiModel,
            aiApiKey = settings.aiApiKey,
            defaultRemindMinutes = settings.defaultRemindMinutes
        )
    )
    val state: StateFlow<MeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val count = container.tripRepository.countAll()
            _state.update { it.copy(tripCount = count) }
        }
    }

    fun onBaseUrl(v: String) = _state.update { it.copy(aiBaseUrl = v) }
    fun onModel(v: String) = _state.update { it.copy(aiModel = v) }
    fun onApiKey(v: String) = _state.update { it.copy(aiApiKey = v) }
    fun onDefaultRemind(v: Int) = _state.update { it.copy(defaultRemindMinutes = v) }

    fun save() {
        settings.aiBaseUrl = _state.value.aiBaseUrl.trim()
        settings.aiModel = _state.value.aiModel.trim()
        settings.aiApiKey = _state.value.aiApiKey.trim()
        settings.defaultRemindMinutes = _state.value.defaultRemindMinutes
        _state.update { it.copy(saved = true) }
    }

    /** 用最小请求测连通性，只发一句话，不烧额度也能验证 Key 和地址。 */
    fun testConnection() {
        val base = _state.value.aiBaseUrl.trim().trimEnd('/')
        val key = _state.value.aiApiKey.trim()
        val model = _state.value.aiModel.trim()
        if (base.isBlank() || key.isBlank() || model.isBlank()) {
            _state.update { it.copy(testResult = "接口地址、模型名、API Key 都要填") }
            return
        }
        _state.update { it.copy(testing = true, testResult = "") }
        viewModelScope.launch {
            try {
                val payload = """
                    {"model":"$model","messages":[{"role":"user","content":"回复一个字：好"}],"max_tokens":8}
                """.trimIndent()
                val url = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $key")
                    .post(payload.toRequestBody("application/json".toMediaType()))
                    .build()
                testClient.newCall(request).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    _state.update {
                        it.copy(
                            testing = false,
                            testResult = if (resp.isSuccessful) "连接成功" else "失败 ${resp.code}：${body.take(200)}"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(testing = false, testResult = "请求异常：${e.message ?: e.javaClass.simpleName}") }
            }
        }
    }

    private inline fun <T> MutableStateFlow<T>.update(block: (T) -> T) {
        value = block(value)
    }
}
