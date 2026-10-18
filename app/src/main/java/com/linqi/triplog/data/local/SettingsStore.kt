package com.linqi.triplog.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 设置存储。API Key 用 EncryptedSharedPreferences（AES256_GCM）加密保存，不明文落盘。
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "triplog_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // 极端情况下加密不可用，退回普通 prefs，功能不中断
        context.getSharedPreferences("triplog_prefs", Context.MODE_PRIVATE)
    }

    var aiBaseUrl: String
        get() = prefs.getString(KEY_AI_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) = prefs.edit { putString(KEY_AI_BASE_URL, value) }

    var aiModel: String
        get() = prefs.getString(KEY_AI_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = prefs.edit { putString(KEY_AI_MODEL, value) }

    var aiApiKey: String
        get() = prefs.getString(KEY_AI_API_KEY, "") ?: ""
        set(value) = prefs.edit { putString(KEY_AI_API_KEY, value) }

    /** 系统提示词，可改。 */
    var aiSystemPrompt: String
        get() = prefs.getString(KEY_AI_SYSTEM_PROMPT, DEFAULT_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT
        set(value) = prefs.edit { putString(KEY_AI_SYSTEM_PROMPT, value) }

    var defaultRemindMinutes: Int
        get() = prefs.getInt(KEY_DEFAULT_REMIND, 180)
        set(value) = prefs.edit { putInt(KEY_DEFAULT_REMIND, value) }

    /** 隐私政策是否已确认。首次启动弹窗，点同意后才为 true。 */
    var privacyAccepted: Boolean
        get() = prefs.getBoolean(KEY_PRIVACY_ACCEPTED, false)
        set(value) = prefs.edit { putBoolean(KEY_PRIVACY_ACCEPTED, value) }

    fun aiConfigured(): Boolean = aiApiKey.isNotBlank() && aiBaseUrl.isNotBlank() && aiModel.isNotBlank()

    companion object {
        const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        const val DEFAULT_MODEL = "gpt-4o-mini"
        const val KEY_AI_BASE_URL = "ai_base_url"
        const val KEY_AI_MODEL = "ai_model"
        const val KEY_AI_API_KEY = "ai_api_key"
        const val KEY_AI_SYSTEM_PROMPT = "ai_system_prompt"
        const val KEY_DEFAULT_REMIND = "default_remind_minutes"
        const val KEY_PRIVACY_ACCEPTED = "privacy_accepted"

        const val DEFAULT_SYSTEM_PROMPT =
            "你是车票信息提取助手。只输出 JSON 对象，不要输出 markdown 代码块围栏，不要输出任何解释文字。" +
                "字段：vehicle_type（train/flight/bus/other）、from_station、to_station、" +
                "depart_time（本地时间，格式 YYYY-MM-DDTHH:mm）、arrive_time（同格式，没有则 null）、" +
                "number（车次号或航班号，保持原文不要转写）、seat、price（只保留数字和小数点）、" +
                "passenger、note。缺失或看不清的字段填 null，绝对不要猜测或编造。" +
                "站名保留完整官方名称，不要用简称。"
    }
}
