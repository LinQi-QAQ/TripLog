package com.linqi.triplog.ui.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 表单校验与转换测试。
 */
class EditFormTest {

    @Test
    fun `三项必填齐了才算合法`() {
        assertTrue(EditForm(fromStation = "上海", toStation = "北京", departTime = 1L).valid)
    }

    @Test
    fun `缺出发地不合法`() {
        assertFalse(EditForm(fromStation = "", toStation = "北京", departTime = 1L).valid)
    }

    @Test
    fun `缺目的地不合法`() {
        assertFalse(EditForm(fromStation = "上海", toStation = "", departTime = 1L).valid)
    }

    @Test
    fun `缺出发时间不合法`() {
        assertFalse(EditForm(fromStation = "上海", toStation = "北京", departTime = null).valid)
    }

    @Test
    fun `空白字符不算填了`() {
        assertFalse(EditForm(fromStation = "   ", toStation = "北京", departTime = 1L).valid)
    }

    @Test
    fun `可选字段全部可空`() {
        val form = EditForm(fromStation = "上海", toStation = "北京", departTime = 1L)
        assertTrue(form.valid)
        assertEquals("", form.number)
        assertEquals("", form.seat)
        assertEquals("", form.price)
        assertEquals("", form.ticketImagePath)
    }

    @Test
    fun `转 Trip 时去掉首尾空格`() {
        val form = EditForm(
            fromStation = "  上海虹桥  ",
            toStation = " 北京南 ",
            departTime = 1L,
            number = " G1234 ",
            seat = " 08车 12A "
        )
        val trip = form.toTrip()
        assertEquals("上海虹桥", trip.fromStation)
        assertEquals("北京南", trip.toStation)
        assertEquals("G1234", trip.number)
        assertEquals("08车 12A", trip.seat)
    }

    @Test
    fun `从已有 Trip 还原表单`() {
        val trip = com.linqi.triplog.data.local.Trip(
            id = 7L,
            fromStation = "上海",
            toStation = "北京",
            departTime = 123L,
            number = "G1",
            ticketImagePath = "/x/y.jpg",
            remindBeforeMinutes = 180
        )
        val form = EditForm.from(trip)
        assertEquals(7L, form.id)
        assertEquals(123L, form.departTime)
        assertEquals("G1", form.number)
        assertEquals("/x/y.jpg", form.ticketImagePath)
        assertEquals(180, form.remindBeforeMinutes)
    }
}
