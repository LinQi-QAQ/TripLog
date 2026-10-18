package com.linqi.triplog.ui.list

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
 * 列表页整页测试。ViewModel 可以直接注入测试仓库，
 * 所以能验证「有数据时界面显示什么、点搜索框输入后列表怎么变」。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TripListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: TripDatabase
    private lateinit var repo: TripRepository
    private lateinit var app: Application

    /** 即将出发的时间（三天后）。 */
    private val futureMillis: Long
        get() = LocalDateTime.now().plusDays(3)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    /** 已经过去的时间（三天前）。 */
    private val pastMillis: Long
        get() = LocalDateTime.now().minusDays(3)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

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

    private fun setListScreen(onOpenDetail: (Long) -> Unit = {}, onAdd: () -> Unit = {}) {
        val vm = TripListViewModel(app, repo)
        composeRule.setContent {
            TripLogTheme {
                TripListScreen(
                    onOpenDetail = onOpenDetail,
                    onAdd = onAdd,
                    viewModel = vm
                )
            }
        }
    }

    @Test
    fun 没有数据时显示空态引导() {
        setListScreen()
        composeRule.onNodeWithText("还没有出行记录").assertIsDisplayed()
        composeRule.onNodeWithText("点右下角按钮添加第一条").assertIsDisplayed()
    }

    @Test
    fun 有未来行程时显示在即将出发分区() = runTest {
        repo.insert(
            Trip(
                vehicleType = VehicleType.TRAIN,
                fromStation = "上海虹桥",
                toStation = "北京南",
                departTime = futureMillis,
                number = "G1234"
            )
        )

        setListScreen()
        composeRule.onNodeWithText("即将出发").assertIsDisplayed()
        composeRule.onNodeWithText("上海虹桥").assertIsDisplayed()
        composeRule.onNodeWithText("北京南").assertIsDisplayed()
        composeRule.onNodeWithText("G1234").assertIsDisplayed()
    }

    @Test
    fun 过去行程归到历史分区() = runTest {
        repo.insert(
            Trip(
                vehicleType = VehicleType.FLIGHT,
                fromStation = "PVG",
                toStation = "PEK",
                departTime = pastMillis,
                number = "MU5101"
            )
        )

        setListScreen()
        composeRule.onNodeWithText("历史").assertIsDisplayed()
        composeRule.onNodeWithText("PVG").assertIsDisplayed()
        composeRule.onNodeWithText("MU5101").assertIsDisplayed()
        // 历史行程不该出现在「即将出发」分区
        composeRule.onAllNodesWithTextCount("即将出发", 0)
    }

    @Test
    fun 两个分区同时存在时都有标题() = runTest {
        repo.insert(
            Trip(fromStation = "未来站", toStation = "未来站B", departTime = futureMillis)
        )
        repo.insert(
            Trip(fromStation = "过去站", toStation = "过去站B", departTime = pastMillis)
        )

        setListScreen()
        composeRule.onNodeWithText("即将出发").assertIsDisplayed()
        composeRule.onNodeWithText("历史").assertIsDisplayed()
        composeRule.onNodeWithText("未来站").assertIsDisplayed()
        composeRule.onNodeWithText("过去站").assertIsDisplayed()
    }

    @Test
    fun 总条数显示真实数量() = runTest {
        repo.insert(Trip(fromStation = "A", toStation = "B", departTime = futureMillis))
        repo.insert(Trip(fromStation = "C", toStation = "D", departTime = pastMillis))

        setListScreen()
        composeRule.onNodeWithText("共 2 条").assertIsDisplayed()
    }

    @Test
    fun 点击卡片跳转到对应详情() = runTest {
        val id = repo.insert(
            Trip(
                fromStation = "上海虹桥",
                toStation = "北京南",
                departTime = futureMillis
            )
        )

        var openedId: Long? = null
        setListScreen(onOpenDetail = { openedId = it })

        composeRule.onNodeWithText("上海虹桥").performClick()
        assertEquals(id, openedId)
    }

    @Test
    fun 搜索无结果时显示空态提示() = runTest {
        repo.insert(
            Trip(fromStation = "上海虹桥", toStation = "北京南", departTime = futureMillis)
        )

        setListScreen()
        // 输入一个肯定搜不到的关键词
        composeRule.onNodeWithText("搜索出发地、目的地、车次").performTextInput("新疆乌鲁木齐")

        composeRule.onNodeWithText("没有匹配的行程").assertIsDisplayed()
        composeRule.onNodeWithText("换个关键词或清除筛选试试").assertIsDisplayed()
    }

    @Test
    fun 设置提醒的行程显示提醒标记() = runTest {
        repo.insert(
            Trip(
                fromStation = "上海",
                toStation = "北京",
                departTime = futureMillis,
                remindBeforeMinutes = 180
            )
        )
        setListScreen()
        composeRule.onNodeWithText("提醒").assertIsDisplayed()
    }

    @Test
    fun 没有设置提醒时不显示提醒标记() = runTest {
        repo.insert(
            Trip(
                fromStation = "上海",
                toStation = "北京",
                departTime = futureMillis,
                remindBeforeMinutes = 0
            )
        )
        setListScreen()
        composeRule.onAllNodesWithTextCount("提醒", 0)
    }

    @Test
    fun 多条行程按出发时间正序() = runTest {
        // 近的排前面，和「即将出发」分区的语义一致：最紧急的在最上面
        repo.insert(Trip(fromStation = "三天后", toStation = "X", departTime = futureMillis))
        repo.insert(
            Trip(
                fromStation = "十天后",
                toStation = "Y",
                departTime = LocalDateTime.now().plusDays(10)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
        )

        val list = repo.getAllSnapshot()
        assertEquals("三天后", list[0].fromStation)
        assertEquals("十天后", list[1].fromStation)
        assertTrue(list.isNotEmpty())
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
