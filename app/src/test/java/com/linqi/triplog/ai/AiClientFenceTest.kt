package com.linqi.triplog.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * AiClient.stripFence 的容错测试。
 * 模型返回的 JSON 常常带 markdown 围栏或者前后多余文字，
 * 这三层容错是识别功能能不能用的关键。
 */
class AiClientFenceTest {

    @Test
    fun `纯 JSON 原样返回`() {
        val input = """{"a":1}"""
        assertEquals("""{"a":1}""", AiClient.stripFence(input))
    }

    @Test
    fun `剥掉 json 围栏`() {
        val input = "```json\n{\"a\":1}\n```"
        assertEquals("""{"a":1}""", AiClient.stripFence(input))
    }

    @Test
    fun `剥掉无语言标注的围栏`() {
        val input = "```\n{\"a\":1}\n```"
        assertEquals("""{"a":1}""", AiClient.stripFence(input))
    }

    @Test
    fun `截取第一个大括号到最后一个大括号`() {
        val input = "好的，这是结果：{\"a\":1} 希望有帮助"
        assertEquals("""{"a":1}""", AiClient.stripFence(input))
    }

    @Test
    fun `围栏外加说明文字`() {
        val input = "识别结果如下：\n```json\n{\"a\":1,\"b\":2}\n```\n以上"
        assertEquals("""{"a":1,"b":2}""", AiClient.stripFence(input))
    }

    @Test
    fun `没有大括号时原样返回`() {
        val input = "  完全不是 json  "
        assertEquals("完全不是 json", AiClient.stripFence(input))
    }

    @Test
    fun `嵌套大括号只取最外层`() {
        val input = "{\"a\":{\"b\":1}}"
        assertEquals("{\"a\":{\"b\":1}}", AiClient.stripFence(input))
    }
}
