package com.linqi.triplog.ui.components

/**
 * 提醒时长的输入解析与显示格式化。
 *
 * 编辑页和「我的」页都要用，抽出来放这里，避免两处实现不一致。
 */
object RemindFormat {

    /** 提前时间的上限，30 天。超过这个值基本没有实际意义，也容易是误输入。 */
    const val MAX_MINUTES = 60 * 24 * 30

    /**
     * 解析用户输入，返回分钟数。
     *
     * 支持的写法：
     * 45 / 45分钟 / 45分 / 2小时 / 2时 / 2h / 1天 / 1d
     * 纯数字按分钟算。不做「1小时30分」这种混合解析，宁可拒绝也不要算错。
     *
     * @return 分钟数；看不懂返回 null。
     */
    fun parse(raw: String): Int? {
        val text = raw.trim()
            .lowercase()
            .replace(" ", "")
            .replace("分钟", "分")
            .replace("个小时", "时")
            .replace("小时", "时")

        if (text.isEmpty()) return null

        return when {
            text.all { it.isDigit() } -> text.toIntOrNull()

            text.endsWith("分") -> text.removeSuffix("分").toIntOrNull()

            text.endsWith("时") || text.endsWith("h") ->
                text.dropLast(1).toIntOrNull()?.let { it * 60 }

            text.endsWith("天") || text.endsWith("d") ->
                text.dropLast(1).toIntOrNull()?.let { it * 60 * 24 }

            else -> null
        }
    }

    /**
     * 分钟数转好读的文案。优先用能整除的最大单位。
     * 90 分钟显示成「1小时30分」，1440 显示成「1天」。
     */
    fun label(minutes: Int): String = when {
        minutes <= 0 -> "不提醒"
        minutes % (60 * 24) == 0 -> "${minutes / (60 * 24)}天"
        minutes % 60 == 0 -> "${minutes / 60}小时"
        minutes >= 60 -> {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0) "${h}小时" else "${h}小时${m}分"
        }
        else -> "${minutes}分钟"
    }
}
