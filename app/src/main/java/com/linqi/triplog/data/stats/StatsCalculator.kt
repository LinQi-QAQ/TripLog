package com.linqi.triplog.data.stats

import com.linqi.triplog.data.local.Trip
import com.linqi.triplog.data.local.VehicleType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * 出行统计。纯计算，不依赖 Android，方便单测。
 * 时间统一用本地时区归属月份，跟用户在日历上的直觉一致。
 */
data class MonthlyStat(
    val yearMonth: YearMonth,
    val total: Int,
    val byType: Map<String, Int>,
    /** 该月走过的不重复城市数（出发地加目的地去重）。 */
    val cityCount: Int,
    /** 该月的票价总和，只统计能解析成数字的。 */
    val totalFare: Double,
    val trips: List<Trip>
)

data class OverallStat(
    val total: Int,
    val byType: Map<String, Int>,
    val cityCount: Int,
    val totalFare: Double,
    val months: List<MonthlyStat>,
    /** 出行最多的月份。 */
    val busiestMonth: YearMonth?,
    /** 最常去的城市 top 列表。 */
    val topCities: List<Pair<String, Int>>,
    /** 最常用的出发地。 */
    val topFromStation: String?
)

object StatsCalculator {

    private val allTypes = listOf(
        VehicleType.TRAIN,
        VehicleType.FLIGHT,
        VehicleType.BUS,
        VehicleType.OTHER
    )

    fun overall(trips: List<Trip>, now: Long = System.currentTimeMillis()): OverallStat {
        if (trips.isEmpty()) {
            return OverallStat(
                total = 0,
                byType = emptyMap(),
                cityCount = 0,
                totalFare = 0.0,
                months = emptyList(),
                busiestMonth = null,
                topCities = emptyList(),
                topFromStation = null
            )
        }

        val byType = allTypes.associateWith { type -> trips.count { it.vehicleType == type } }
            .filterValues { it > 0 }

        val cities = trips.flatMap { listOf(it.fromStation, it.toStation) }
            .filter { it.isNotBlank() }
            .toSet()

        val fare = trips.sumOf { parseFare(it.price) }

        val grouped = trips.groupBy { monthOf(it, now) }
        val months = grouped.map { (ym, list) ->
            MonthlyStat(
                yearMonth = ym,
                total = list.size,
                byType = allTypes.associateWith { t -> list.count { it.vehicleType == t } }
                    .filterValues { it > 0 },
                cityCount = list.flatMap { listOf(it.fromStation, it.toStation) }
                    .filter { it.isNotBlank() }
                    .toSet()
                    .size,
                totalFare = list.sumOf { parseFare(it.price) },
                trips = list.sortedByDescending { it.departTime ?: 0 }
            )
        }.sortedByDescending { it.yearMonth }

        val cityCounts = trips.flatMap { listOf(it.fromStation, it.toStation) }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()

        val fromCounts = trips.map { it.fromStation }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()

        return OverallStat(
            total = trips.size,
            byType = byType,
            cityCount = cities.size,
            totalFare = fare,
            months = months,
            busiestMonth = months.maxByOrNull { it.total }?.yearMonth,
            topCities = cityCounts.entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .take(5)
                .map { it.key to it.value },
            topFromStation = fromCounts.entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .firstOrNull()?.key
        )
    }

    /**
     * 行程归属的月份。出发时间为空的归到「创建时间」所在月，
     * 都为空就归到当前月，保证不会漏统计。
     */
    fun monthOf(trip: Trip, now: Long = System.currentTimeMillis()): YearMonth {
        val millis = trip.departTime ?: trip.createdAt.takeIf { it > 0 } ?: now
        return YearMonth.from(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
    }

    /**
     * 票价解析。只取数字部分，兼容「¥123.5」「123.50 CNY」「￥123」这些写法。
     * 解析不了返回 0，不参与求和。
     */
    fun parseFare(raw: String): Double {
        if (raw.isBlank()) return 0.0
        val cleaned = raw.replace(",", "")
            .filter { it.isDigit() || it == '.' }
        if (cleaned.isBlank() || cleaned == ".") return 0.0
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    /** 年度筛选用：某年的行程。 */
    fun tripsOfYear(trips: List<Trip>, year: Int, now: Long = System.currentTimeMillis()): List<Trip> =
        trips.filter { monthOf(it, now).year == year }

    /** 数据里出现过的年份，降序，供切换器用。 */
    fun years(trips: List<Trip>, now: Long = System.currentTimeMillis()): List<Int> =
        trips.map { monthOf(it, now).year }.distinct().sortedDescending()
}
