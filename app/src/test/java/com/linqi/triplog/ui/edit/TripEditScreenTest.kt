package com.linqi.triplog.ui.edit

import android.app.Application
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.linqi.triplog.ai.AiClient
import com.linqi.triplog.data.local.Trip
import com.linqi.triplog.data.local.TripDatabase
import com.linqi.triplog.data.local.VehicleType
import com.linqi.triplog.data.repo.TripRepository
import com.linqi.triplog.ui.theme.TripLogTheme
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * 添加/编辑页整页测试。
 * 重点验证表单校验：必填项没填时保存按钮不可点，填齐后可点。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TripEditScreenTest {

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

    /** 未配置 AI，识别按钮应该是禁用的。 */
    private fun setEditScreen(tripId: Long? = null) {
        val vm = TripEditViewModel(
            app = app,
            repo = repo,
            aiClient = AiClient(com.linqi.triplog.di.AppContainer.of(app).settings),
            aiConfigured = false
        )
        vm.load(tripId)
        composeRule.setContent {
            TripLogTheme {
                TripEditScreen(
                    tripId = tripId,
                    onDone = {},
                    onCancel = {},
                    viewModel = vm
                )
            }
        }
    }

    @Test
    fun 新建页标题是添加行程() {
        setEditScreen()
        composeRule.onNodeWithText("添加行程").assertIsDisplayed()
    }

    @Test
    fun 编辑页标题是编辑行程() = runTest {
        val id = repo.insert(
            Trip(fromStation = "上海", toStation = "北京", departTime = 1_700_000_000_000)
        )
        setEditScreen(tripId = id)
        composeRule.onNodeWithText("编辑行程").assertIsDisplayed()
    }

    @Test
    fun `未配置 AI 时识别按钮禁用`() {
        setEditScreen()
        composeRule.onNodeWithText("选择截图识别").assertIsNotEnabled()
        composeRule.onNodeWithText("AI 未配置，去「我的 - AI 设置」填接口信息后才能用").assertIsDisplayed()
    }

    @Test
    fun `保存按钮初始不可点`() {
        setEditScreen()
        // 三项必填都没填
        composeRule.onNodeWithText("保存").assertIsNotEnabled()
        composeRule.onNodeWithText("出发地、目的地、出发时间为必填").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `只填出发地时保存仍不可点`() {
        setEditScreen()
        // 输入框按 placeholder 定位，label 是独立的 Text 节点不能输入
        composeRule.onNodeWithText("上海虹桥").performTextInput("上海虹桥")
        composeRule.onNodeWithText("保存").assertIsNotEnabled()
    }

    @Test
    fun `填了出发地和目的地仍不可点`() {
        setEditScreen()
        composeRule.onNodeWithText("上海虹桥").performTextInput("上海虹桥")
        composeRule.onNodeWithText("北京南").performTextInput("北京南")
        composeRule.onNodeWithText("保存").assertIsNotEnabled()
    }

    @Test
    fun `空白字符不算填了必填项`() {
        setEditScreen()
        composeRule.onNodeWithText("上海虹桥").performTextInput("   ")
        composeRule.onNodeWithText("保存").assertIsNotEnabled()
    }

    @Test
    fun `表单标签齐全`() {
        setEditScreen()
        // 表单很长，靠下的字段要先滚到可视区域再断言
        listOf(
            "出发地 *",
            "目的地 *",
            "出发时间 *",
            "到达时间",
            "车次 / 航班号",
            "座位",
            "票价",
            "乘车人",
            "备注"
        ).forEach { label ->
            composeRule.onNodeWithText(label).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun `交通工具四个选项都在`() {
        setEditScreen()
        composeRule.onNodeWithText("火车").assertIsDisplayed()
        composeRule.onNodeWithText("飞机").assertIsDisplayed()
        composeRule.onNodeWithText("汽车").assertIsDisplayed()
        composeRule.onNodeWithText("其他").assertIsDisplayed()
    }

    @Test
    fun `提醒时长四个选项都在`() {
        setEditScreen()
        // 提醒区在表单底部，要滚过去
        listOf("不提醒", "1小时", "3小时", "1天").forEach { label ->
            composeRule.onNodeWithText(label).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun `出发时间默认显示未设置`() {
        setEditScreen()
        // 出发时间和到达时间都显示「未设置」，共两个节点，用 onAllNodes 数一下
        composeRule.onAllNodesWithTextCount("未设置", 2)
    }

    @Test
    fun `可选字段留空不影响必填校验提示`() {
        setEditScreen()
        // 只动可选字段，按 placeholder 定位
        composeRule.onNodeWithText("G1234").performTextInput("G1234")
        composeRule.onNodeWithText("08车 12A").performTextInput("08车 12A")
        // 必填仍未填，保存还是不可点
        composeRule.onNodeWithText("保存").assertIsNotEnabled()
    }

    @Test
    fun `编辑已有行程时表单带出原值`() = runTest {
        val id = repo.insert(
            Trip(
                vehicleType = VehicleType.FLIGHT,
                fromStation = "PVG",
                toStation = "PEK",
                departTime = LocalDateTime.of(2026, 10, 20, 8, 30)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                number = "MU5101",
                seat = "12A",
                price = "1280"
            )
        )
        setEditScreen(tripId = id)

        // 表单值渲染在输入框里，靠下的字段先滚到可视区域
        composeRule.onNodeWithText("PVG").assertIsDisplayed()
        composeRule.onNodeWithText("PEK").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("MU5101").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("12A").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1280").performScrollTo().assertIsDisplayed()
        // 三项必填齐了，保存应该可点
        composeRule.onNodeWithText("保存").performScrollTo().assertIsEnabled()
    }

    @Test
    fun `删除按钮只在编辑模式出现`() {
        setEditScreen()
        // 新建模式不该有删除
        composeRule.onAllNodesWithTextCount("删除", 0)
    }

    @Test
    fun 编辑模式有删除按钮() = runTest {
        val id = repo.insert(
            Trip(fromStation = "A", toStation = "B", departTime = 1_700_000_000_000)
        )
        setEditScreen(tripId = id)
        composeRule.onNodeWithText("删除").assertIsDisplayed()
    }

    @Test
    fun `删除后回调被触发且数据真的没了`() = runTest {
        val id = repo.insert(
            Trip(fromStation = "A", toStation = "B", departTime = 1_700_000_000_000)
        )
        var doneCalled = false
        val vm = TripEditViewModel(
            app = app,
            repo = repo,
            aiClient = AiClient(com.linqi.triplog.di.AppContainer.of(app).settings),
            aiConfigured = false
        )
        vm.load(id)
        composeRule.setContent {
            TripLogTheme {
                TripEditScreen(
                    tripId = id,
                    onDone = { doneCalled = true },
                    onCancel = {},
                    viewModel = vm
                )
            }
        }

        composeRule.onNodeWithText("删除").performClick()

        // 删除走协程，Robolectric 下要等主线程调度完
        val deadline = System.currentTimeMillis() + 3000
        while (!doneCalled && System.currentTimeMillis() < deadline) {
            composeRule.waitForIdle()
            kotlinx.coroutines.delay(50)
        }

        assertTrue("删除后应该触发 onDone", doneCalled)
        val remaining = repo.getAllSnapshot()
        assertTrue("数据库里应该没有这条了", remaining.none { it.id == id })
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
