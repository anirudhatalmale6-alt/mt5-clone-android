package com.mt5clone.ui.common.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// MT5 Color Palette
object MT5Colors {
    val BackgroundPrimary = Color(0xFF131722)
    val BackgroundSecondary = Color(0xFF1C1C2E)
    val BackgroundTertiary = Color(0xFF252540)
    val Surface = Color(0xFF1E222D)
    val SurfaceElevated = Color(0xFF2A2E39)

    val TextPrimary = Color(0xFFD1D4DC)
    val TextSecondary = Color(0xFF787B86)
    val TextTertiary = Color(0xFF4C525E)

    val BuyGreen = Color(0xFF26A69A)
    val SellRed = Color(0xFFEF5350)
    val CandleGreen = Color(0xFF26A69A)
    val CandleRed = Color(0xFFEF5350)

    val AccentBlue = Color(0xFF2196F3)
    val AccentOrange = Color(0xFFFFA726)
    val AccentYellow = Color(0xFFFFEB3B)

    val ChartGrid = Color(0xFF2A2E39)
    val ChartCrosshair = Color(0xFF787B86)
    val ChartVolume = Color(0x33787B86)

    val TabSelected = Color(0xFF2196F3)
    val TabUnselected = Color(0xFF787B86)
    val Divider = Color(0xFF2A2E39)
}

data class MT5ColorScheme(
    val backgroundPrimary: Color = MT5Colors.BackgroundPrimary,
    val backgroundSecondary: Color = MT5Colors.BackgroundSecondary,
    val backgroundTertiary: Color = MT5Colors.BackgroundTertiary,
    val surface: Color = MT5Colors.Surface,
    val surfaceElevated: Color = MT5Colors.SurfaceElevated,
    val textPrimary: Color = MT5Colors.TextPrimary,
    val textSecondary: Color = MT5Colors.TextSecondary,
    val textTertiary: Color = MT5Colors.TextTertiary,
    val buyGreen: Color = MT5Colors.BuyGreen,
    val sellRed: Color = MT5Colors.SellRed,
    val candleGreen: Color = MT5Colors.CandleGreen,
    val candleRed: Color = MT5Colors.CandleRed,
    val accentBlue: Color = MT5Colors.AccentBlue,
    val chartGrid: Color = MT5Colors.ChartGrid,
    val chartCrosshair: Color = MT5Colors.ChartCrosshair,
    val divider: Color = MT5Colors.Divider,
    val tabSelected: Color = MT5Colors.TabSelected,
    val tabUnselected: Color = MT5Colors.TabUnselected
)

val LocalMT5Colors = staticCompositionLocalOf { MT5ColorScheme() }

private val DarkColorScheme = darkColorScheme(
    primary = MT5Colors.AccentBlue,
    secondary = MT5Colors.TextSecondary,
    tertiary = MT5Colors.AccentOrange,
    background = MT5Colors.BackgroundPrimary,
    surface = MT5Colors.Surface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = MT5Colors.TextPrimary,
    onSurface = MT5Colors.TextPrimary,
)

private val MT5Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = MT5Colors.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = MT5Colors.TextPrimary
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        color = MT5Colors.TextSecondary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = MT5Colors.TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = MT5Colors.TextPrimary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        color = MT5Colors.TextSecondary
    )
)

@Composable
fun MT5Theme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMT5Colors provides MT5ColorScheme()) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = MT5Typography,
            content = content
        )
    }
}

object MT5ThemeProvider {
    val colors: MT5ColorScheme
        @Composable
        get() = LocalMT5Colors.current
}
