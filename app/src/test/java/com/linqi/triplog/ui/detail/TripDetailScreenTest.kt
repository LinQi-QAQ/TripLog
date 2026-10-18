package com.linqi.triplog.ui.detail

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
 * 详情页整页测试。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TripDetailScreenTest {

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

    private fun setDetailScreen(tripId: Long, onBack: () -> Unit = {}, onEdit: (Long) -> Unit = {}) {
        val vm = TripDetailViewModel(app, repo)
        composeRule.setContent {
            TripLogTheme {
                TripDetailScreen(
                    tripId = tripId,
                    onBack = onBack,
                    onEdit = onEdit,
                    viewModel = vm
                )
            }
        }
    }

    @Test
    fun `标题和操作按钮都在`() = runTest {
        val id = repo.insert(
            Trip(
                fromStation = "上海虹桥",
                toStation = "北京南",
                departTime = 1_700_000_000_000
            )
        )
        setDetailScreen(id)

        composeRule.onNodeWithText("行程详情").assertIsDisplayed()
        composeRule.onNodeWithText("返回").assertIsDisplayed()
        composeRule.onNodeWithText("编辑").assertIsDisplayed()
    }

    @Test
    fun `起终点和交通工具类型显示`() = runTest {
        val id = repo.insert(
            Trip(
                vehicleType = VehicleType.FLIGHT,
                fromStation = "PVG",
                toStation = "PEK",
                departTime = 1_700_000_000_000
            )
        )
        setDetailScreen(id)

        composeRule.onNodeWithText("PVG").assertIsDisplayed()
        composeRule.onNodeWithText("PEK").assertIsDisplayed()
        composeRule.onNodeWithText("飞机").assertIsDisplayed()
    }

    @Test
    fun `时间字段格式化显示`() = runTest {
        val depart = LocalDateTime.of(2026, 10, 20, 8, 30)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val arrive = LocalDateTime.of(2026, 10, 20, 12, 45)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val id = repo.insert(
            Trip(
                fromStation = "上海",
                toStation = "北京",
                departTime = depart,
                arriveTime = arrive
            )
        )
        setDetailScreen(id)

        composeRule.onNodeWithText("出发时间").assertIsDisplayed()
        composeRule.onNodeWithText("2026-10-20 08:30").assertIsDisplayed()
        composeRule.onNodeWithText("到达时间").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2026-10-20 12:45").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `其他字段有值时显示`() = runTest {
        val id = repo.insert(
            Trip(
                fromStation = "上海虹桥",
                toStation = "北京南",
                departTime = 1_700_000_000_000,
                number = "G1234",
                seat = "08车 12A",
                price = "553",
                passenger = "林祺"
            )
        )
        setDetailScreen(id)

        composeRule.onNodeWithText("车次 / 航班号").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("G1234").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("08车 12A").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("553").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("林祺").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `其他字段没值时显示未填写`() = runTest {
        val id = repo.insert(
            Trip(
                fromStation = "上海",
                toStation = "北京",
                departTime = 1_700_000_000_000
            )
        )
        setDetailScreen(id)

        // 车次、座位、票价、乘车人都没填，应显示「未填写」
        composeRule.onNodeWithText("车次 / 航班号").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithTextCount("未填写", 4)
    }

    @Test
    fun `有备注时显示备注内容`() = runTest {
        val id = repo.insert(
            Trip(
                fromStation = "上海",
                toStation = "北京",
                departTime = 1_700_000_000_000,
                note = "靠窗，带小孩"
            )
        )
        setDetailScreen(id)

        composeRule.onNodeWithText("备注").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("靠窗，带小孩").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `无备注时不显示备注区`() = runTest {
        val id = repo.insert(
            Trip(fromStation = "上海", toStation = "北京", departTime = 1_700_000_000_000)
        )
        setDetailScreen(id)

        composeRule.onAllNodesWithTextCount("备注", 0)
    }

    @Test
    fun `设了提醒时显示提前时长`() = runTest {
        val id = repo.insert(
            Trip(
                fromStation = "上海",
                toStation = "北京",
                departTime = 1_700_000_000_000,
                remindBeforeMinutes = 180
            )
        )
        setDetailScreen(id)

        composeRule.onNodeWithText("提前 180 分钟提醒").assertIsDisplayed()
    }

    @Test
    fun `没设提醒时不显示提醒文案`() = runTest {
        val id = repo.insert(
            Trip(
                fromStation = "上海",
                toStation = "北京",
                departTime = 1_700_000_000_000,
                remindBeforeMinutes = 0
            )
        )
        setDetailScreen(id)

        composeRule.onAllNodesWithTextCount("提前 180 分钟提醒", 0)
    }

    @Test
    fun `点返回触发回调`() = runTest {
        val id = repo.insert(
            Trip(fromStation = "上海", toStation = "北京", departTime = 1_700_000_000_000)
        )
        var backCalled = false
        setDetailScreen(id, onBack = { backCalled = true })

        composeRule.onNodeWithText("返回").performClick()
        assertTrue(backCalled)
    }

    @Test
    fun `点编辑带着正确 id 跳转`() = runTest {
        val id = repo.insert(
            Trip(fromStation = "上海", toStation = "北京", departTime = 1_700_000_000_000)
        )
        var editId: Long? = null
        setDetailScreen(id, onEdit = { editId = it })

        composeRule.onNodeWithText("编辑").performClick()
        assertEquals(id, editId)
    }

    @Test
    fun `没有车票图时不渲染图片区`() = runTest {
        val id = repo.insert(
            Trip(
                fromStation = "上海",
                toStation = "北京",
                departTime = 1_700_000_000_000,
                ticketImagePath = ""
            )
        )
        setDetailScreen(id)

        // 没有图时不该出现车票图的 contentDescription
        composeRule.onAllNodesWithContentDescriptionCount("车票图", 0)
    }

    @Test
    fun `图片路径指向不存在的文件时不崩溃`() = runTest {
        val id = repo.insert(
            Trip(
                fromStation = "上海",
                toStation = "北京",
                departTime = 1_700_000_000_000,
                ticketImagePath = "/nonexistent/path/ticket_999.jpg"
            )
        )
        setDetailScreen(id)

        // 文件不存在，AsyncImage 不会加载，但页面其他内容要正常
        composeRule.onNodeWithText("上海").assertIsDisplayed()
        composeRule.onNodeWithText("北京").assertIsDisplayed()
    }

    @Test
    fun 四种交通工具类型名称都正确() = runTest {
        // 一个规则只能 setContent 一次，所以四个类型合并到一次渲染里验证映射关系
        val cases = listOf(
            VehicleType.TRAIN to "火车",
            VehicleType.FLIGHT to "飞机",
            VehicleType.BUS to "汽车",
            VehicleType.OTHER to "其他"
        )
        for ((type, name) in cases) {
            repo.insert(
                Trip(
                    vehicleType = type,
                    fromStation = name,
                    toStation = "B",
                    departTime = 1_700_000_000_000
                )
            )
        }
        // 直接用映射函数验证，避免多次 setContent
        for ((type, name) in cases) {
            assertEquals(name, com.linqi.triplog.ui.components.vehicleName(type))
        }
    }
}

/** 断言某个文本在界面上出现指定次数，0 表示不应该出现。 */
private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextCount(
    text: String,
    expected: Int
) {
    val actual = onAllNodes(androidx.compose.ui.test.hasText(text)).fetchSemanticsNodes().size
    assertEquals("文本「$text」出现次数不对", expected, actual)
}

/** 断言某个 contentDescription 在界面上出现指定次数。 */
private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithContentDescriptionCount(
    description: String,
    expected: Int
) {
    val actual = onAllNodes(
        androidx.compose.ui.test.hasContentDescription(description)
    ).fetchSemanticsNodes().size
    assertEquals("contentDescription「$description」出现次数不对", expected, actual)
}
