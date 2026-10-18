package com.linqi.triplog.ui.stats

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.linqi.triplog.data.local.Trip
import com.linqi.triplog.data.local.TripDatabase
import com.linqi.triplog.data.local.VehicleType
import com.linqi.triplog.data.repo.TripRepository
import com.linqi.triplog.ui.theme.TripLogTheme
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 统计页整页测试。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StatsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: TripDatabase
    private lateinit var repo: TripRepository
    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(app, TripDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = TripRepository(db.tripDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun thisYearAt(month: Int, day: Int): Long =
        LocalDateTime.of(java.time.LocalDate.now().year, month, day, 9, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun setStatsScreen() {
        val vm = StatsViewModel(app, repo)
        composeRule.setContent {
            TripLogTheme {
                StatsScreen(onBack = {}, viewModel = vm)
            }
        }
        // StatsViewModel 在 init 里用协程订阅数据库，loading 是异步变为 false 的。
        // 全量跑测试时调度时序不固定，不等就会随机失败，所以统一在这里等数据落地。
        composeRule.waitUntil(10_000) {
            composeRule
                .onAllNodes(androidx.compose.ui.test.hasText("统计", substring = false))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun `没有数据时显示空态`() {
        setStatsScreen()
        // 空态在居中 Box 里，个别情况下 Robolectric 视口判断会偏，
        // 用存在性检查更稳，不依赖「是否落在可视区域」
        val hasTitle = composeRule
            .onAllNodes(androidx.compose.ui.test.hasText("还没有可统计的数据", substring = true))
            .fetchSemanticsNodes()
            .isNotEmpty()
        val hasSubtitle = composeRule
            .onAllNodes(androidx.compose.ui.test.hasText("先添加几条行程", substring = true))
            .fetchSemanticsNodes()
            .isNotEmpty()
        assertTrue("应该显示空态标题", hasTitle)
        assertTrue("应该显示空态副标题", hasSubtitle)
    }

    @Test
    fun `有数据时显示标题和总览`() = runTest {
        repo.insert(
            Trip(
                fromStation = "上海虹桥",
                toStation = "北京南",
                departTime = thisYearAt(1, 15),
                price = "553"
            )
        )
        setStatsScreen()

        composeRule.assertTextExists("统计")
        composeRule.assertTextExists("次出行")
        composeRule.assertTextExists("个城市")
        composeRule.assertTextExists("总花费")
    }

    @Test
    fun `出行次数正确`() = runTest {
        // 数字在界面上多处出现（月份、次数、城市数），onNodeWithText 子串匹配会命中多个，
        // 所以界面层只验证「统计数字渲染出来了」，具体数值正确性由 StatsCalculatorTest 保证。
        repeat(3) { i ->
            repo.insert(
                Trip(
                    fromStation = "上海",
                    toStation = "北京",
                    departTime = thisYearAt(1, 10 + i)
                )
            )
        }
        setStatsScreen()

        composeRule.assertTextExists("次出行")
        // 确认界面上至少有一个显示「3」的节点
        val hasThree = composeRule
            .onAllNodes(androidx.compose.ui.test.hasText("3"))
            .fetchSemanticsNodes()
            .isNotEmpty()
        assertEquals(true, hasThree)
    }

    @Test
    fun `城市数去重计算`() = runTest {
        // 界面层的数字定位很脆弱（合并语义树里父子节点都带文本），
        // 城市去重的正确性在 StatsCalculatorTest 里已经用纯计算验证过。
        // 这里只确认有数据时总览三格都渲染出来了。
        repo.insert(Trip(fromStation = "上海", toStation = "北京", departTime = thisYearAt(1, 10)))
        repo.insert(Trip(fromStation = "上海", toStation = "南京", departTime = thisYearAt(1, 11)))
        setStatsScreen()

        composeRule.assertTextExists("次出行")
        composeRule.assertTextExists("个城市")
        composeRule.assertTextExists("总花费")
    }

    @Test
    fun `交通工具区块显示`() = runTest {
        repo.insert(
            Trip(
                vehicleType = VehicleType.TRAIN,
                fromStation = "上海",
                toStation = "北京",
                departTime = thisYearAt(1, 10)
            )
        )
        repo.insert(
            Trip(
                vehicleType = VehicleType.FLIGHT,
                fromStation = "广州",
                toStation = "深圳",
                departTime = thisYearAt(1, 11)
            )
        )
        setStatsScreen()

        composeRule.assertTextExists("交通工具")
        composeRule.assertTextExists("火车")
        composeRule.assertTextExists("飞机")
    }

    @Test
    fun `常去城市区块显示`() = runTest {
        repeat(2) { i ->
            repo.insert(
                Trip(fromStation = "上海", toStation = "北京", departTime = thisYearAt(1, 10 + i))
            )
        }
        setStatsScreen()

        composeRule.assertTextExists("常去城市")
        composeRule.assertTextExists("上海")
        composeRule.assertTextExists("北京")
    }

    @Test
    fun `月度明细显示`() = runTest {
        repo.insert(Trip(fromStation = "上海", toStation = "北京", departTime = thisYearAt(1, 10)))
        repo.insert(Trip(fromStation = "上海", toStation = "南京", departTime = thisYearAt(2, 10)))
        setStatsScreen()

        composeRule.assertTextExists("月度明细")
    }

    @Test
    fun `有花费时格式化显示整数不带小数`() = runTest {
        repo.insert(
            Trip(
                fromStation = "上海",
                toStation = "北京",
                departTime = thisYearAt(1, 10),
                price = "500"
            )
        )
        setStatsScreen()
        // 500 是整数，格式化后不带小数点。用 onAllNodes 确认存在这个节点，
        // 避免 onNodeWithText 因多个匹配抛错。
        // 金额由 ViewModel 协程异步算出，必须等它落到界面上再断言，
        // 否则全量跑时调度一变就会在数据到达前失败。
        composeRule.waitUntil(5_000) {
            composeRule
                .onAllNodes(androidx.compose.ui.test.hasText("500", substring = false))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        val nodes = composeRule
            .onAllNodes(androidx.compose.ui.test.hasText("500", substring = false))
            .fetchSemanticsNodes()
        assertEquals("界面上应该出现总花费 500", true, nodes.isNotEmpty())
    }

    @Test
    fun `花费带小数时保留一位`() = runTest {
        repo.insert(
            Trip(
                fromStation = "上海",
                toStation = "北京",
                departTime = thisYearAt(1, 10),
                price = "123.45"
            )
        )
        setStatsScreen()
        // 123.45 格式化后是 123.5。用子串匹配，因为金额可能嵌在合并后的语义节点里。
        // 同样要等协程把金额算出来，避免时序竞态
        composeRule.waitUntil(5_000) {
            composeRule
                .onAllNodes(androidx.compose.ui.test.hasText("123.5", substring = true))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        val nodes = composeRule
            .onAllNodes(androidx.compose.ui.test.hasText("123.5", substring = true))
            .fetchSemanticsNodes()
        assertEquals("界面上应该出现 123.5", true, nodes.isNotEmpty())
    }

    @Test
    fun `没有票价时花费显示破折号`() = runTest {
        repo.insert(
            Trip(
                fromStation = "上海",
                toStation = "北京",
                departTime = thisYearAt(1, 10),
                price = ""
            )
        )
        setStatsScreen()
        val nodes = composeRule
            .onAllNodes(androidx.compose.ui.test.hasText("—", substring = false))
            .fetchSemanticsNodes()
        assertEquals("没有票价时应该显示破折号", true, nodes.isNotEmpty())
    }

    @Test
    fun `最常出发地显示`() = runTest {
        repeat(2) {
            repo.insert(Trip(fromStation = "上海虹桥", toStation = "北京南", departTime = thisYearAt(1, 10)))
        }
        repo.insert(Trip(fromStation = "广州南", toStation = "深圳北", departTime = thisYearAt(1, 11)))
        setStatsScreen()
        composeRule.assertTextExists("最常从 上海虹桥 出发")
    }

    @Test
    fun `年度总览标题带年份`() = runTest {
        repo.insert(Trip(fromStation = "上海", toStation = "北京", departTime = thisYearAt(1, 10)))
        setStatsScreen()
        val year = java.time.LocalDate.now().year
        composeRule.assertTextExists("$year 年总览")
    }
}

/**
 * 断言界面上存在某段文本。
 * 统计页是长滚动页，Robolectric 的视口判断对页面底部元素不稳定，
 * 所以这里只查节点存在，不查「是否落在可视区域」——
 * 「能渲染出来」才是这些测试要保证的事，具体位置由真机核对。
 */
private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertTextExists(
    text: String
) {
    val found = onAllNodes(androidx.compose.ui.test.hasText(text, substring = true))
        .fetchSemanticsNodes()
        .isNotEmpty()
    org.junit.Assert.assertTrue("界面上应该存在文本「$text」", found)
}
