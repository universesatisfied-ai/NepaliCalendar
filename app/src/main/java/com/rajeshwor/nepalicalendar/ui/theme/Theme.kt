package com.rajeshwor.nepalicalendar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DeepGreen = Color(0xFF18251D)
val Lime = Color(0xFFB7FF72)
val Surface = Color(0xFFF7F8F5)
val Card = Color(0xFFFFFFFF)

private val Light = lightColorScheme(
    primary = DeepGreen,
    onPrimary = Color.White,
    secondary = Color(0xFF4C6B4F),
    tertiary = Lime,
    background = Surface,
    surface = Card
)

private val Dark = darkColorScheme(
    primary = Lime,
    onPrimary = DeepGreen,
    secondary = Color(0xFFB6D0B5),
    tertiary = Lime,
    background = Color(0xFF101511),
    surface = Color(0xFF18201A)
)

@Composable
fun NepaliCalendarTheme(
    dark: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        typography = Typography(),
        content = content
    )
}
