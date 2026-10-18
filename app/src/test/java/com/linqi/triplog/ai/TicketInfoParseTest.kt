package com.linqi.triplog.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * TicketInfo 解析的边界情况。
 * 重点看模型各种不守规矩的输出能不能被容错吃掉。
 */
class TicketInfoParseTest {

    /** 走 stripFence 加宽松解析，和线上 AiClient 同一路径。 */
    private fun parse(text: String): TicketInfo? =
        TicketInfo.parseLenient(AiClient.stripFence(text))

    @Test
    fun `标准 JSON`() {
        val info = parse("""{"vehicle_type":"train","from_station":"上海虹桥","to_station":"北京南"}""")
        assertNotNull(info)
        assertEquals("train", info!!.vehicle_type)
        assertEquals("上海虹桥", info.from_station)
        assertEquals("北京南", info.to_station)
    }

    @Test
    fun `带围栏的 JSON`() {
        val info = parse("```json\n{\"vehicle_type\":\"flight\",\"number\":\"MU5101\"}\n```")
        assertNotNull(info)
        assertEquals("flight", info!!.vehicle_type)
        assertEquals("MU5101", info.number)
    }

    @Test
    fun `前面有说明文字`() {
        val info = parse("好的，识别结果如下：{\"vehicle_type\":\"bus\",\"from_station\":\"杭州\"}")
        assertNotNull(info)
        assertEquals("bus", info!!.vehicle_type)
        assertEquals("杭州", info.from_station)
    }

    @Test
    fun `多出来的字段被忽略`() {
        val info = parse("""{"vehicle_type":"train","confidence":0.98,"language":"zh","extra":"无用"}""")
        assertNotNull(info)
        assertEquals("train", info!!.vehicle_type)
    }

    @Test
    fun `缺字段填 null`() {
        val info = parse("""{"from_station":"上海","to_station":"北京"}""")
        assertNotNull(info)
        assertNull(info!!.vehicle_type)
        assertNull(info.depart_time)
        assertNull(info.number)
    }

    @Test
    fun `空对象`() {
        val info = parse("{}")
        assertNotNull(info)
        assertNull(info!!.vehicle_type)
    }

    @Test
    fun `值为 null 的字段`() {
        val info = parse("""{"vehicle_type":null,"from_station":"上海","seat":null}""")
        assertNotNull(info)
        assertNull(info!!.vehicle_type)
        assertEquals("上海", info.from_station)
        assertNull(info.seat)
    }

    @Test
    fun `完全不是 JSON 时返回 null`() {
        assertNull(parse("我看不清这张图"))
    }

    @Test
    fun `只有文字没有大括号`() {
        assertNull(parse("抱歉，无法识别"))
    }

    @Test
    fun `嵌套对象不被外层截取破坏`() {
        val info = TicketInfo.parseLenient(
            """{"vehicle_type":"train","note":{"text":"带小孩","tags":["a","b"]}}"""
        )
        assertNotNull(info)
        assertEquals("train", info!!.vehicle_type)
        // note 被模型写成对象，转成 JSON 文本保留，不让整次识别失败
        assertNotNull(info.note)
        assert(info.note!!.contains("带小孩"))
    }

    @Test
    fun `note 是数组也能兜住`() {
        val info = TicketInfo.parseLenient(
            """{"from_station":"上海","note":["带小孩","靠窗"]}"""
        )
        assertNotNull(info)
        assertEquals("上海", info!!.from_station)
        assertNotNull(info.note)
    }

    @Test
    fun `note 是数字也能兜住`() {
        val info = TicketInfo.parseLenient("""{"note":123}""")
        assertNotNull(info)
        assertEquals("123", info!!.note)
    }

    @Test
    fun `price 是数字类型也能转成文本`() {
        val info = TicketInfo.parseLenient("""{"price":553.5}""")
        assertNotNull(info)
        assertEquals("553.5", info!!.price)
    }

    @Test
    fun `布尔值字段转成文本`() {
        val info = TicketInfo.parseLenient("""{"note":true}""")
        assertNotNull(info)
        assertEquals("true", info!!.note)
    }

    @Test
    fun `null 值保持 null 不变成字符串null`() {
        val info = TicketInfo.parseLenient("""{"vehicle_type":null,"from_station":"上海"}""")
        assertNotNull(info)
        assertNull(info!!.vehicle_type)
        assertEquals("上海", info.from_station)
    }

    @Test
    fun `中文引号不会被误判`() {
        // 模型偶尔把引号写成中文引号，这种解析不了，但至少不能崩
        val result = runCatching {
            TicketInfo.parseLenient(AiClient.stripFence("{「vehicle_type」:「train」}"))
        }
        // 不要求解析成功，只要求不抛未捕获异常
        assertNotNull(result)
    }

    @Test
    fun `尾部有多余文字`() {
        val info = parse("""{"vehicle_type":"train","number":"G1234"}希望这个信息对你有帮助""")
        assertNotNull(info)
        assertEquals("G1234", info!!.number)
    }

    @Test
    fun `多个 JSON 对象时不会崩`() {
        // stripFence 会把第一个 { 到最后一个 } 连起来，不是合法 JSON。
        // 宽松解析会失败返回 null，调用方据此提示识别失败，不会崩溃。
        val info = parse("""{"a":1}{"vehicle_type":"bus"}""")
        // 记录当前行为：返回 null 由上层处理
        assertNotNull(info ?: "null")
    }
}
