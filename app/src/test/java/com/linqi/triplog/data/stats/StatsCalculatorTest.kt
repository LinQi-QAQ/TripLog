package com.linqi.triplog.data.stats

import com.linqi.triplog.data.local.Trip
import com.linqi.triplog.data.local.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.YearMonth

class StatsCalculatorTest {

    private fun at(
        year: Int, month: Int, day: Int, hour: Int = 9, minute: Int = 0
    ): Long = LocalDateTime.of(year, month, day, hour, minute)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    private fun trip(
        from: String = "上海",
        to: String = "北京",
        depart: Long? = null,
        type: String = VehicleType.TRAIN,
        price: String = ""
    ) = Trip(
        vehicleType = type,
        fromStation = from,
        toStation = to,
        departTime = depart,
        price = price
    )

    @Test
    fun `空数据返回全零`() {
        val s = StatsCalculator.overall(emptyList())
        assertEquals(0, s.total)
        assertEquals(0, s.cityCount)
        assertEquals(0.0, s.totalFare, 0.001)
        assertTrue(s.months.isEmpty())
        assertNull(s.busiestMonth)
        assertTrue(s.topCities.isEmpty())
    }

    @Test
    fun `总数正确`() {
        val trips = listOf(
            trip(depart = at(2026, 10, 1)),
            trip(depart = at(2026, 10, 2)),
            trip(depart = at(2026, 11, 1))
        )
        assertEquals(3, StatsCalculator.overall(trips).total)
    }

    @Test
    fun `按类型统计只列非零项`() {
        val trips = listOf(
            trip(depart = at(2026, 10, 1), type = VehicleType.TRAIN),
            trip(depart = at(2026, 10, 2), type = VehicleType.TRAIN),
            trip(depart = at(2026, 10, 3), type = VehicleType.FLIGHT)
        )
        val byType = StatsCalculator.overall(trips).byType
        assertEquals(2, byType[VehicleType.TRAIN])
        assertEquals(1, byType[VehicleType.FLIGHT])
        assertTrue(!byType.containsKey(VehicleType.BUS))
    }

    @Test
    fun `城市数去重`() {
        val trips = listOf(
            trip(from = "上海", to = "北京", depart = at(2026, 10, 1)),
            trip(from = "上海", to = "北京", depart = at(2026, 10, 2)),
            trip(from = "上海", to = "南京", depart = at(2026, 10, 3))
        )
        // 上海、北京、南京 共三个
        assertEquals(3, StatsCalculator.overall(trips).cityCount)
    }

    @Test
    fun `票价求和只算数字部分`() {
        val trips = listOf(
            trip(depart = at(2026, 10, 1), price = "¥123.5"),
            trip(depart = at(2026, 10, 2), price = "553"),
            trip(depart = at(2026, 10, 3), price = "76.50 CNY")
        )
        assertEquals(753.0, StatsCalculator.overall(trips).totalFare, 0.001)
    }

    @Test
    fun `票价含千分位逗号`() {
        assertEquals(1234.5, StatsCalculator.parseFare("1,234.5"), 0.001)
    }

    @Test
    fun `票价解析不了算零`() {
        assertEquals(0.0, StatsCalculator.parseFare("免费"), 0.001)
        assertEquals(0.0, StatsCalculator.parseFare(""), 0.001)
        assertEquals(0.0, StatsCalculator.parseFare("元"), 0.001)
    }

    @Test
    fun `按月分组且降序`() {
        val trips = listOf(
            trip(depart = at(2026, 9, 1)),
            trip(depart = at(2026, 11, 1)),
            trip(depart = at(2026, 10, 1)),
            trip(depart = at(2026, 10, 5))
        )
        val months = StatsCalculator.overall(trips).months
        assertEquals(3, months.size)
        assertEquals(YearMonth.of(2026, 11), months[0].yearMonth)
        assertEquals(YearMonth.of(2026, 10), months[1].yearMonth)
        assertEquals(2, months[1].total)
    }

    @Test
    fun `最忙月份取条数最多的`() {
        val trips = listOf(
            trip(depart = at(2026, 9, 1)),
            trip(depart = at(2026, 9, 2)),
            trip(depart = at(2026, 10, 1))
        )
        assertEquals(YearMonth.of(2026, 9), StatsCalculator.overall(trips).busiestMonth)
    }

    @Test
    fun `无出发时间时归到创建时间所在月`() {
        val now = at(2026, 10, 15, 12)
        val t = Trip(
            fromStation = "A",
            toStation = "B",
            departTime = null,
            createdAt = at(2026, 8, 20)
        )
        assertEquals(YearMonth.of(2026, 8), StatsCalculator.monthOf(t, now))
    }

    @Test
    fun `出发时间为空创建时间为零时归到当前月`() {
        val now = at(2026, 10, 15, 12)
        val t = Trip(fromStation = "A", toStation = "B", departTime = null, createdAt = 0)
        assertEquals(YearMonth.of(2026, 10), StatsCalculator.monthOf(t, now))
    }

    @Test
    fun `常去城市按出现次数排序`() {
        val trips = listOf(
            trip(from = "上海", to = "北京", depart = at(2026, 10, 1)),
            trip(from = "上海", to = "北京", depart = at(2026, 10, 2)),
            trip(from = "上海", to = "南京", depart = at(2026, 10, 3))
        )
        val top = StatsCalculator.overall(trips).topCities
        // 上海 3 次，北京 2 次，南京 1 次
        assertEquals("上海", top[0].first)
        assertEquals(3, top[0].second)
        assertEquals("北京", top[1].first)
        assertEquals(2, top[1].second)
    }

    @Test
    fun `最常出发地`() {
        val trips = listOf(
            trip(from = "上海虹桥", to = "北京南", depart = at(2026, 10, 1)),
            trip(from = "上海虹桥", to = "南京南", depart = at(2026, 10, 2)),
            trip(from = "广州南", to = "深圳北", depart = at(2026, 10, 3))
        )
        assertEquals("上海虹桥", StatsCalculator.overall(trips).topFromStation)
    }

    @Test
    fun `年度筛选`() {
        val trips = listOf(
            trip(depart = at(2025, 12, 31)),
            trip(depart = at(2026, 1, 1)),
            trip(depart = at(2026, 6, 1))
        )
        val now = at(2026, 10, 1)
        assertEquals(2, StatsCalculator.tripsOfYear(trips, 2026, now).size)
        assertEquals(1, StatsCalculator.tripsOfYear(trips, 2025, now).size)
    }

    @Test
    fun `年份列表降序去重`() {
        val trips = listOf(
            trip(depart = at(2025, 3, 1)),
            trip(depart = at(2026, 1, 1)),
            trip(depart = at(2026, 8, 1))
        )
        assertEquals(listOf(2026, 2025), StatsCalculator.years(trips, at(2026, 10, 1)))
    }

    @Test
    fun `月度统计内部字段齐全`() {
        val trips = listOf(
            trip(from = "上海", to = "北京", depart = at(2026, 10, 1), price = "100"),
            trip(from = "上海", to = "南京", depart = at(2026, 10, 2), type = VehicleType.FLIGHT, price = "200")
        )
        val m = StatsCalculator.overall(trips).months.first()
        assertEquals(2, m.total)
        assertEquals(3, m.cityCount)
        assertEquals(300.0, m.totalFare, 0.001)
        assertEquals(1, m.byType[VehicleType.TRAIN])
        assertEquals(1, m.byType[VehicleType.FLIGHT])
    }
}
