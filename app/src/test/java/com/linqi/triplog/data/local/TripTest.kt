package com.linqi.triplog.data.local

import com.linqi.triplog.remind.departLabel
import com.linqi.triplog.remind.title
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Trip 的时间标签与车辆类型测试。
 */
class TripTest {

    private fun trip(
        from: String = "上海虹桥",
        to: String = "北京南",
        depart: Long? = null,
        number: String = "",
        type: String = VehicleType.TRAIN
    ) = Trip(
        vehicleType = type,
        fromStation = from,
        toStation = to,
        departTime = depart,
        number = number
    )

    @Test
    fun `departLabel 无时间时显示未定时间`() {
        assertEquals("未定时间", trip(depart = null).departLabel())
    }

    @Test
    fun `departLabel 格式化到分钟`() {
        // 2026-10-20 08:30 本地时间
        val millis = java.time.LocalDateTime.of(2026, 10, 20, 8, 30)
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        assertEquals("2026-10-20 08:30", trip(depart = millis).departLabel())
    }

    @Test
    fun `title 包含起终点`() {
        val t = trip(from = "上海", to = "杭州")
        assertEquals("上海 到 杭州", t.title())
    }

    @Test
    fun `title 有车次时带上车次`() {
        val t = trip(from = "上海", to = "杭州", number = "G1234")
        assertEquals("上海 到 杭州 G1234", t.title())
    }

    @Test
    fun `车辆类型枚举完整`() {
        assertEquals(
            listOf("train", "flight", "bus", "other"),
            VehicleType.all
        )
    }

    @Test
    fun `表单校验要求三项`() {
        // 通过 EditForm 的 valid 规则间接验证必填项
        val full = com.linqi.triplog.ui.edit.EditForm(
            fromStation = "上海",
            toStation = "北京",
            departTime = 1L
        )
        assertTrue(full.valid)

        val noDepart = com.linqi.triplog.ui.edit.EditForm(
            fromStation = "上海",
            toStation = "北京",
            departTime = null
        )
        assertTrue(!noDepart.valid)
    }
}
