package com.linqi.triplog.data.stats

/**
 * 车站名到城市名的归一化。
 *
 * 统计「去过几个城市」时，如果直接拿站名去重，「上海虹桥」「上海南」「上海」会被算成
 * 三个不同的地方，数字虚高。这里把常见车站名收敛到城市。
 *
 * 做法分三层，按优先级：
 * 1. 精确表：明确知道属于哪个城市但名字里看不出来的（如「虹桥」「浦东」→ 上海）。
 *    航空场景特别需要这一层，机场名往往不带城市。
 * 2. 后缀剥离：国内车站绝大多数是「城市名」+「方位/次名」，如「北京南」「广州东」
 *    「杭州东」，剥掉后缀就得到城市。只剥一次，避免把「南京」的「京」当后缀。
 * 3. 兜底：原样返回，不做猜测。
 *
 * 这是个纯函数的启发式，不联网、不引数据库。宁可少归并，也不要把不同城市错误合并。
 */
object CityNormalizer {

    /**
     * 名字里看不出城市、但确实属于某城市的特殊站名与机场名。
     * 键是归一化后的比较形式（去掉「站」「市」等），值是城市名。
     */
    private val specialCases: Map<String, String> = mapOf(
        // 上海的机场与卫星站
        "虹桥" to "上海",
        "虹桥火车站" to "上海",
        "浦东" to "上海",
        "浦东机场" to "上海",
        "上海虹桥" to "上海",
        "上海浦东" to "上海",
        // 北京
        "首都" to "北京",
        "首都机场" to "北京",
        "大兴" to "北京",
        "大兴机场" to "北京",
        "北京首都" to "北京",
        // 广州
        "白云" to "广州",
        "白云机场" to "广州",
        "广州白云" to "广州",
        // 深圳
        "宝安" to "深圳",
        "宝安机场" to "深圳",
        // 成都
        "双流" to "成都",
        "双流机场" to "成都",
        "天府机场" to "成都",
        // 杭州
        "萧山" to "杭州",
        "萧山机场" to "杭州",
        // 南京
        "禄口" to "南京",
        "禄口机场" to "南京",
        // 武汉
        "天河机场" to "武汉",
        // 西安
        "咸阳" to "西安",
        "咸阳机场" to "西安",
        // 长沙
        "黄花" to "长沙",
        "黄花机场" to "长沙",
        // 重庆
        "江北" to "重庆",
        "江北机场" to "重庆",
        // 香港
        "赤鱲角" to "香港",
        // 乌鲁木齐
        "地窝堡" to "乌鲁木齐"
    )

    /**
     * 会被当成「方位或次名」剥掉的后缀。
     * 顺序有讲究：长的放前面，否则「火车东站」会被「东站」先匹配。
     * 注意不要包含「州」「京」「汉」这类可能本身属于城市名结尾的字。
     */
    private val suffixes: List<String> = listOf(
        "火车站", "高铁站", "国际机场", "机场",
        "东站", "南站", "西站", "北站", "新站", "中心站", "长途站", "客运站", "汽车站",
        "东", "南", "西", "北"
    )

    /** 比较前统一去掉的行政单位后缀。 */
    private val adminSuffixes: List<String> = listOf("市", "站")

    /**
     * 把站名归一化成城市名。看不懂就原样返回，不猜。
     *
     * @param station 用户输入或识别出的站名，可能是「北京南」「首都机场」「上海」。
     * @return 城市名，如「北京」「上海」。空输入返回空串。
     */
    fun toCity(station: String): String {
        val trimmed = station.trim()
        if (trimmed.isEmpty()) return ""

        // 第一层：先查特殊表。这里要试三种形式，因为「虹桥机场」要先去后缀
        // 才能匹配到「虹桥」这条规则。
        specialCases[trimmed]?.let { return it }

        val withoutAdmin = trimAdmin(trimmed)
        specialCases[withoutAdmin]?.let { return it }

        val stripped = stripDirection(withoutAdmin)
        specialCases[stripped]?.let { return it }

        // 第二层：剥方位后缀后如果变了，就用剥过的结果
        if (stripped != withoutAdmin && stripped.isNotEmpty()) {
            return stripped
        }

        // 第三层：兜底
        return withoutAdmin
    }

    private fun trimAdmin(raw: String): String {
        var s = raw
        for (suffix in adminSuffixes) {
            if (s.endsWith(suffix) && s.length > suffix.length) {
                s = s.removeSuffix(suffix)
            }
        }
        return s
    }

    private fun stripDirection(raw: String): String {
        for (suffix in suffixes) {
            if (raw.endsWith(suffix) && raw.length > suffix.length) {
                return raw.removeSuffix(suffix)
            }
        }
        return raw
    }
}
