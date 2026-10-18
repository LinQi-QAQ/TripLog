package com.linqi.triplog.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * AI 识别车票后返回的结构化结果。字段可空，空表示图上没有或看不清。
 *
 * 注意：模型不守规矩是常态，常见花样包括
 * - 该给字符串的地方给对象或数组
 * - 该给数字的地方给 "¥123" 这种字符串
 * - 多给一堆用不上的字段
 * 所以每个字段都单独做了宽容转换，一个字段抽风不影响其他字段。
 */
@Serializable
data class TicketInfo(
    val vehicle_type: String? = null,
    val from_station: String? = null,
    val to_station: String? = null,
    val depart_time: String? = null,
    val arrive_time: String? = null,
    val number: String? = null,
    val seat: String? = null,
    val price: String? = null,
    val passenger: String? = null,
    val note: String? = null
) {
    companion object {

        private val lenient = Json { ignoreUnknownKeys = true; isLenient = true }

        /**
         * 从原始 JSON 元素宽容地取字符串。
         * 字符串原样返回；数字或布尔转成文本；对象和数组转成紧凑 JSON 文本；
         * null 返回 null。这样 note 被写成对象也不会让整次识别失败。
         */
        fun stringOf(element: JsonElement?): String? = when (element) {
            null -> null
            is JsonPrimitive -> {
                if (element.isString) element.content
                else element.content.takeIf { it.isNotBlank() && it != "null" }
            }
            else -> runCatching { lenient.encodeToString(JsonElement.serializer(), element) }.getOrNull()
        }

        /** 宽松解析：先按 TicketInfo 解，解不了的字段逐个兜底。 */
        fun parseLenient(rawJson: String): TicketInfo? {
            return runCatching {
                val obj = lenient.parseToJsonElement(rawJson) as? kotlinx.serialization.json.JsonObject
                    ?: return null
                TicketInfo(
                    vehicle_type = stringOf(obj["vehicle_type"]),
                    from_station = stringOf(obj["from_station"]),
                    to_station = stringOf(obj["to_station"]),
                    depart_time = stringOf(obj["depart_time"]),
                    arrive_time = stringOf(obj["arrive_time"]),
                    number = stringOf(obj["number"]),
                    seat = stringOf(obj["seat"]),
                    price = stringOf(obj["price"]),
                    passenger = stringOf(obj["passenger"]),
                    note = stringOf(obj["note"])
                )
            }.getOrNull()
        }
    }
}
