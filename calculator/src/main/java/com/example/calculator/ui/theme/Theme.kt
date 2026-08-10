package com.example.calculator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppBackground = Color(0xFF141619)
val DisplayBackground = Color(0xFF0C0E10)
val KeypadBackground = Color(0xFF17191C)
val SheetBackground = Color(0xFF1E2126)
val SecondaryText = Color(0xFF9AA0A6)
val ExpressionColor = Color(0xFFB9BFC7)
val DigitBackground = Color(0xFFE8EAED)
val DigitText = Color(0xFF1B1D20)
val ErrorRed = Color(0xFFFF6B6B)

data class AccentPalette(
    val function: Color,
    val operator: Color,
    val equal: Color,
)

val AccentNames = listOf(
    "薄荷绿",
    "水晶蓝",
    "香槟金",
    "玫瑰红",
    "宝石蓝",
    "柠檬绿",
    "苹果绿",
    "钻石灰",
)

val AccentPalettes = listOf(
    AccentPalette(Color(0xFF4CAF50), Color(0xFF43A047), Color(0xFF66BB6A)),
    AccentPalette(Color(0xFF42A5F5), Color(0xFF1E88E5), Color(0xFF64B5F6)),
    AccentPalette(Color(0xFFD4AF37), Color(0xFFC9A227), Color(0xFFE0C15A)),
    AccentPalette(Color(0xFFE57373), Color(0xFFEF5350), Color(0xFFF08A8A)),
    AccentPalette(Color(0xFF5C6BC0), Color(0xFF3F51B5), Color(0xFF7986CB)),
    AccentPalette(Color(0xFF9CCC65), Color(0xFF8BC34A), Color(0xFFAED581)),
    AccentPalette(Color(0xFF66BB6A), Color(0xFF4DB6AC), Color(0xFF81C784)),
    AccentPalette(Color(0xFF78909C), Color(0xFF607D8B), Color(0xFF90A4AE)),
)

@Composable
fun CalculatorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AccentPalettes[0].function,
            background = AppBackground,
            surface = KeypadBackground,
            onBackground = Color.White,
            onSurface = Color.White,
            surfaceVariant = SheetBackground,
        ),
        content = content,
    )
}
