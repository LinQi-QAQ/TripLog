package com.linqi.triplog.data.stats

/**
 * 票价解析。
 *
 * 用户可能输入「553」「¥553」「553.5 元」「1,234.50 CNY」「¥ 88」这些写法，
 * 也可能写「免费」「0」「公司报销」。这里只负责从字符串里抠出数字。
 *
 * 与旧实现的关键差别：解析失败返回 null 而不是 0。
 * 因为「免费」和「写错了」在求和时应该都不计入，但「0 元」是真实金额。
 * 用 null 表达「没有可用的数值」，语义清楚。
 */
object FareParser {

    /**
     * 从票价原文提取数值。
     *
     * @param raw 原始输入。
     * @return 解析出的非负金额；无法解析返回 null。
     */
    fun parseOrNull(raw: String): Double? {
        if (raw.isBlank()) return null

        val trimmed = raw.trim()

        // 负数直接判无效。票价不该是负数，出现负号说明用户输错了，
        // 不能把负号过滤掉当成正数收下。
        if (trimmed.startsWith("-")) return null

        // 先去掉千分位逗号和空白，再只保留数字与小数点
        val cleaned = trimmed.replace(",", "")
            .replace("，", "")
            .filter { it.isDigit() || it == '.' }

        if (cleaned.isBlank()) return null
        // 只有小数点、没有数字，说明输入里根本没有金额
        if (cleaned.all { it == '.' }) return null

        // 多个小数点视为脏数据，取第一个作为小数点重新拼，避免 toDoubleOrNull 直接失败
        val dots = cleaned.count { it == '.' }
        val normalized = when {
            dots == 0 -> cleaned
            dots == 1 -> {
                when {
                    cleaned.startsWith(".") -> "0$cleaned"
                    cleaned.endsWith(".") -> cleaned.dropLast(1).ifBlank { return null }
                    else -> cleaned
                }
            }
            else -> {
                val first = cleaned.indexOf('.')
                cleaned.substring(0, first + 1) +
                    cleaned.substring(first + 1).replace(".", "")
            }
        }

        val value = normalized.toDoubleOrNull() ?: return null
        if (value < 0) return null
        return value
    }

    /**
     * 兼容旧调用：解析失败按 0 处理。统计求和之外的地方用这个。
     */
    fun parseOrZero(raw: String): Double = parseOrNull(raw) ?: 0.0
}
