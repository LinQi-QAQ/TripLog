package com.linqi.triplog.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.linqi.triplog.data.local.Trip
import com.linqi.triplog.data.local.VehicleType
import com.linqi.triplog.remind.departLabel
import com.linqi.triplog.remind.title
import com.linqi.triplog.ui.components.EmptyState
import com.linqi.triplog.ui.components.FormField
import com.linqi.triplog.ui.components.LoadingBox
import com.linqi.triplog.ui.components.PrimaryButton
import com.linqi.triplog.ui.components.RouteArrowRow
import com.linqi.triplog.ui.components.SecondaryButton
import com.linqi.triplog.ui.components.VehicleIcon
import com.linqi.triplog.ui.components.vehicleName
import com.linqi.triplog.ui.theme.TripLogTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose 界面测试，用 Robolectric 在 JVM 上跑，不需要真机。
 * 这解决了一个实际问题：本机没有可用设备（模拟器缺 AEHD 驱动，手机没插）。
 *
 * 覆盖的是「界面真的渲染得出来、点得动」，不是纯逻辑。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ComposeComponentsRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 空态文案能显示() {
        composeRule.setContent {
            TripLogTheme {
                EmptyState(
                    title = "还没有出行记录",
                    subtitle = "点右下角按钮添加第一条"
                )
            }
        }
        composeRule.onNodeWithText("还没有出行记录").assertIsDisplayed()
        composeRule.onNodeWithText("点右下角按钮添加第一条").assertIsDisplayed()
    }

    @Test
    fun 起终点行显示出发地和目的地() {
        composeRule.setContent {
            TripLogTheme { RouteArrowRow(from = "上海虹桥", to = "北京南") }
        }
        composeRule.onNodeWithText("上海虹桥").assertIsDisplayed()
        composeRule.onNodeWithText("北京南").assertIsDisplayed()
    }

    @Test
    fun 出发地为空时显示占位文字() {
        composeRule.setContent {
            TripLogTheme { RouteArrowRow(from = "", to = "北京南") }
        }
        composeRule.onNodeWithText("出发地").assertIsDisplayed()
        composeRule.onNodeWithText("北京南").assertIsDisplayed()
    }

    @Test
    fun 表单输入能触发回调() {
        var captured = ""
        composeRule.setContent {
            TripLogTheme {
                FormField(
                    label = "出发地",
                    value = captured,
                    onValueChange = { captured = it },
                    placeholder = "上海虹桥"
                )
            }
        }
        composeRule.onNodeWithText("上海虹桥").performTextInput("北京南站")
        assertEquals("北京南站", captured)
    }

    @Test
    fun 必填标签带星号() {
        composeRule.setContent {
            TripLogTheme {
                FormField(
                    label = "出发时间",
                    value = "",
                    onValueChange = {},
                    required = true
                )
            }
        }
        composeRule.onNodeWithText("出发时间 *").assertIsDisplayed()
    }

    @Test
    fun 非必填标签不带星号() {
        composeRule.setContent {
            TripLogTheme {
                FormField(label = "备注", value = "", onValueChange = {})
            }
        }
        composeRule.onNodeWithText("备注").assertIsDisplayed()
        composeRule.onAllNodesWithTextCount("备注 *", 0)
    }

    @Test
    fun 主按钮点击能触发() {
        var clicked = false
        composeRule.setContent {
            TripLogTheme { PrimaryButton(text = "保存", onClick = { clicked = true }) }
        }
        composeRule.onNodeWithText("保存").performClick()
        assertTrue(clicked)
    }

    @Test
    fun 次按钮点击能触发() {
        var clicked = false
        composeRule.setContent {
            TripLogTheme { SecondaryButton(text = "取消", onClick = { clicked = true }) }
        }
        composeRule.onNodeWithText("取消").performClick()
        assertTrue(clicked)
    }

    @Test
    fun 禁用状态的按钮点了不触发() {
        var clicked = false
        composeRule.setContent {
            TripLogTheme {
                PrimaryButton(text = "保存", onClick = { clicked = true }, enabled = false)
            }
        }
        composeRule.onNodeWithText("保存").performClick()
        assertFalse(clicked)
    }

    @Test
    fun 加载态文字能显示() {
        composeRule.setContent {
            TripLogTheme { LoadingBox("正在识别，请稍候…") }
        }
        composeRule.onNodeWithText("正在识别，请稍候…").assertIsDisplayed()
    }

    @Test
    fun 四种交通工具名称都能渲染() {
        // 一次渲染四种，避免同一个 setContent 调多次（会报 already set content）
        composeRule.setContent {
            TripLogTheme {
                androidx.compose.foundation.layout.Column {
                    for (type in VehicleType.all) {
                        androidx.compose.foundation.layout.Row {
                            VehicleIcon(type, size = androidx.compose.ui.unit.Dp(22f))
                            androidx.compose.material3.Text(vehicleName(type))
                        }
                    }
                }
            }
        }
        for (type in VehicleType.all) {
            composeRule.onNodeWithText(vehicleName(type)).assertIsDisplayed()
        }
    }

    @Test
    fun 深色模式下内容照样显示() {
        composeRule.setContent {
            TripLogTheme(darkTheme = true) { EmptyState(title = "深色模式测试") }
        }
        composeRule.onNodeWithText("深色模式测试").assertIsDisplayed()
    }

    @Test
    fun 亮色模式下内容照样显示() {
        composeRule.setContent {
            TripLogTheme(darkTheme = false) { EmptyState(title = "亮色模式测试") }
        }
        composeRule.onNodeWithText("亮色模式测试").assertIsDisplayed()
    }

    @Test
    fun 时间标签格式化到分钟() {
        val millis = java.time.LocalDateTime.of(2026, 10, 20, 8, 30)
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val trip = Trip(
            vehicleType = VehicleType.TRAIN,
            fromStation = "上海",
            toStation = "北京",
            departTime = millis
        )
        assertEquals("2026-10-20 08:30", trip.departLabel())
    }

    @Test
    fun 无时间时标签为未定时间() {
        val trip = Trip(fromStation = "上海", toStation = "北京", departTime = null)
        assertEquals("未定时间", trip.departLabel())
    }

    @Test
    fun 行程标题含起终点和车次() {
        val trip = Trip(
            fromStation = "上海",
            toStation = "北京",
            number = "G1234"
        )
        assertEquals("上海 到 北京 G1234", trip.title())
    }

    @Test
    fun 无车次时标题只有起终点() {
        val trip = Trip(fromStation = "上海", toStation = "北京", number = "")
        assertEquals("上海 到 北京", trip.title())
    }
}

/** 断言某个文本在界面上出现指定次数。 */
private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextCount(
    text: String,
    expected: Int
) {
    val actual = onAllNodes(androidx.compose.ui.test.hasText(text)).fetchSemanticsNodes().size
    assertEquals("文本「$text」出现次数不对", expected, actual)
}
