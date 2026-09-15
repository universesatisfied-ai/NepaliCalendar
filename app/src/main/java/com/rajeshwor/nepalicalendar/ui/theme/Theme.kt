package com.rajeshwor.nepalicalendar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DeepGreen = Color(0xFF18251D)
val Lime = Color(0xFFB7FF72)
val Surface = Color(0xFFF7F8F5)

private val Light = lightColorScheme(
    primary = DeepGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8DC),
    onPrimaryContainer = DeepGreen,
    secondary = Color(0xFF4C6B4F),
    tertiary = Lime,
    background = Surface,
    surface = Color.White,
    surfaceVariant = Color(0xFFE9EEE8),
    errorContainer = Color(0xFFFFDAD6)
)

private val Dark = darkColorScheme(
    primary = Lime,
    onPrimary = DeepGreen,
    primaryContainer = Color(0xFF304232),
    onPrimaryContainer = Color(0xFFD7E8D5),
    secondary = Color(0xFFB6D0B5),
    tertiary = Lime,
    background = Color(0xFF101511),
    surface = Color(0xFF18201A),
    surfaceVariant = Color(0xFF283229),
    errorContainer = Color(0xFF93000A)
)

@Composable
fun NepaliCalendarTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
