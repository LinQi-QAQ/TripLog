package com.linqi.triplog.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 自定义提醒时间的解析与格式化。
 * 这部分逻辑用户直接输入，边界最容易出问题，单独测。
 */
class RemindFormatTest {

    @Test
    fun `纯数字按分钟解析`() {
        assertEquals(45, RemindFormat.parse("45"))
        assertEquals(1, RemindFormat.parse("1"))
        assertEquals(999, RemindFormat.parse("999"))
    }

    @Test
    fun `带分字解析`() {
        assertEquals(45, RemindFormat.parse("45分"))
        assertEquals(90, RemindFormat.parse("90分钟"))
    }

    @Test
    fun `带时字按小时解析`() {
        assertEquals(60, RemindFormat.parse("1时"))
        assertEquals(120, RemindFormat.parse("2小时"))
        assertEquals(120, RemindFormat.parse("2个小时"))
        assertEquals(120, RemindFormat.parse("2h"))
    }

    @Test
    fun `带天字按天解析`() {
        assertEquals(1440, RemindFormat.parse("1天"))
        assertEquals(2880, RemindFormat.parse("2天"))
        assertEquals(1440, RemindFormat.parse("1d"))
    }

    @Test
    fun `空格和大小写都容错`() {
        assertEquals(120, RemindFormat.parse(" 2 H "))
        assertEquals(120, RemindFormat.parse("2 小时"))
    }

    @Test
    fun `看不懂的输入返回 null`() {
        assertNull(RemindFormat.parse(""))
        assertNull(RemindFormat.parse("   "))
        assertNull(RemindFormat.parse("abc"))
        assertNull(RemindFormat.parse("一小时"))
        assertNull(RemindFormat.parse("1小时30分"))
        assertNull(RemindFormat.parse("半小时"))
    }

    @Test
    fun `零和负数的处理`() {
        // 0 是合法输入，表示不提醒，由调用方判断
        assertEquals(0, RemindFormat.parse("0"))
    }

    @Test
    fun `格式化保留最大单位`() {
        assertEquals("不提醒", RemindFormat.label(0))
        assertEquals("1天", RemindFormat.label(1440))
        assertEquals("2天", RemindFormat.label(2880))
        assertEquals("1小时", RemindFormat.label(60))
        assertEquals("3小时", RemindFormat.label(180))
        assertEquals("45分钟", RemindFormat.label(45))
    }

    @Test
    fun `非整小时显示成时分组合`() {
        assertEquals("1小时30分", RemindFormat.label(90))
        assertEquals("2小时15分", RemindFormat.label(135))
    }

    @Test
    fun `负数和零都显示不提醒`() {
        assertEquals("不提醒", RemindFormat.label(-1))
        assertEquals("不提醒", RemindFormat.label(0))
    }
}
