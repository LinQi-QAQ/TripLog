package com.linqi.triplog.data.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 票价解析。用户输入格式五花八门，这里覆盖常见的几种。
 */
class FareParserTest {

    @Test
    fun `纯数字`() {
        assertEquals(553.0, FareParser.parseOrNull("553")!!, 0.001)
        assertEquals(0.0, FareParser.parseOrNull("0")!!, 0.001)
    }

    @Test
    fun `带货币符号`() {
        assertEquals(553.0, FareParser.parseOrNull("¥553")!!, 0.001)
        assertEquals(553.0, FareParser.parseOrNull("￥553")!!, 0.001)
        assertEquals(88.5, FareParser.parseOrNull("$88.5")!!, 0.001)
    }

    @Test
    fun `带单位`() {
        assertEquals(553.0, FareParser.parseOrNull("553元")!!, 0.001)
        assertEquals(123.45, FareParser.parseOrNull("123.45 CNY")!!, 0.001)
    }

    @Test
    fun `千分位逗号`() {
        assertEquals(1234.5, FareParser.parseOrNull("1,234.5")!!, 0.001)
        assertEquals(1234.0, FareParser.parseOrNull("1,234")!!, 0.001)
    }

    @Test
    fun `只有小数点开头或结尾`() {
        assertEquals(0.5, FareParser.parseOrNull(".5")!!, 0.001)
        assertEquals(5.0, FareParser.parseOrNull("5.")!!, 0.001)
    }

    @Test
    fun `多个小数点视为脏数据`() {
        // 取第一个小数点，后面的当误触删掉
        assertEquals(12.53, FareParser.parseOrNull("12.5.3")!!, 0.001)
    }

    @Test
    fun `空和无法解析返回 null`() {
        assertNull(FareParser.parseOrNull(""))
        assertNull(FareParser.parseOrNull("   "))
        assertNull(FareParser.parseOrNull("免费"))
        assertNull(FareParser.parseOrNull("公司报销"))
        assertNull(FareParser.parseOrNull("."))
    }

    @Test
    fun `负数视为无效`() {
        // 票价不该是负数，解析出负数说明输入有误
        assertNull(FareParser.parseOrNull("-50"))
    }

    @Test
    fun `parseOrZero 兼容旧调用`() {
        assertEquals(553.0, FareParser.parseOrZero("553"), 0.001)
        assertEquals(0.0, FareParser.parseOrZero("免费"), 0.001)
        assertEquals(0.0, FareParser.parseOrZero(""), 0.001)
    }
}
