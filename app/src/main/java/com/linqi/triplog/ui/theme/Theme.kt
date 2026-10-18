package com.linqi.triplog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 延续个人主页的极简路子：中性灰阶 + 一个墨蓝强调色。
val InkBlue = Color(0xFF3B5B92)
val InkBlueLight = Color(0xFF7FA0D8)
val SkyBlue = Color(0xFF5B7F9E)
val Olive = Color(0xFF7A8B5A)
val NeutralGray = Color(0xFF8A8A85)

val BgLight = Color(0xFFFFFFFF)
val CardLight = Color(0xFFF7F7F5)
val DividerLight = Color(0xFFE6E6E1)
val TextPrimary = Color(0xFF1A1A1A)
val TextSecondary = Color(0xFF8A8A85)

val BgDark = Color(0xFF121212)
val CardDark = Color(0xFF1E1E1E)
val DividerDark = Color(0xFF2C2C2C)
val TextPrimaryDark = Color(0xFFECECEC)
val TextSecondaryDark = Color(0xFF9A9A95)

private val LightColors = lightColorScheme(
    primary = InkBlue,
    onPrimary = Color.White,
    background = BgLight,
    onBackground = TextPrimary,
    surface = BgLight,
    onSurface = TextPrimary,
    surfaceVariant = CardLight,
    onSurfaceVariant = TextSecondary,
    outline = DividerLight,
    outlineVariant = DividerLight,
    secondary = SkyBlue,
    tertiary = Olive
)

private val DarkColors = darkColorScheme(
    primary = InkBlueLight,
    onPrimary = Color.Black,
    background = BgDark,
    onBackground = TextPrimaryDark,
    surface = BgDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = DividerDark,
    outlineVariant = DividerDark,
    secondary = SkyBlue,
    tertiary = Olive
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Medium, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)
)

@Composable
fun TripLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}

/** 按交通工具类型取颜色。 */
fun vehicleColor(type: String, dark: Boolean): Color = when (type) {
    com.linqi.triplog.data.local.VehicleType.TRAIN -> if (dark) InkBlueLight else InkBlue
    com.linqi.triplog.data.local.VehicleType.FLIGHT -> SkyBlue
    com.linqi.triplog.data.local.VehicleType.BUS -> Olive
    else -> NeutralGray
}
