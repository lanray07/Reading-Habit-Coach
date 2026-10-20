package com.readinghabitcoach.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Forest = Color(0xFF35574A)
val Ivory = Color(0xFFF8F5EF)
val Parchment = Color(0xFFEDE5D6)
val Bronze = Color(0xFF8A6135)
val Ink = Color(0xFF292C29)
private val light = lightColorScheme(primary = Forest, onPrimary = Ivory, primaryContainer = Color(0xFFE3EBDD), onPrimaryContainer = Forest,
    secondaryContainer = Parchment, onSecondaryContainer = Ink, tertiaryContainer = Parchment, surfaceTint = Forest,
    secondary = Bronze, background = Ivory, surface = Ivory, surfaceContainer = Color(0xFFF0ECE3),
    surfaceContainerLow = Color(0xFFFFFCF7), onSurface = Ink, onBackground = Ink,
    onSurfaceVariant = Color(0xFF62665E), outline = Color(0xFF7A7E73), outlineVariant = Color(0xFFDEDCD2))
private val dark = darkColorScheme(primary = Color(0xFFB7D1BD), onPrimary = Color(0xFF183B2E), primaryContainer = Forest,
    onPrimaryContainer = Ivory, secondary = Color(0xFFDDB582), background = Color(0xFF1C231F), surface = Color(0xFF1C231F),
    surfaceContainer = Color(0xFF29342C), surfaceContainerLow = Color(0xFF222C25), onSurface = Color(0xFFE9E8DF),
    onBackground = Color(0xFFE9E8DF), onSurfaceVariant = Color(0xFFBFC7BC), outlineVariant = Color(0xFF414E43))
@Composable fun CoachTheme(darkMode: Boolean = false, content: @Composable () -> Unit) {
    val typography = Typography(
        headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 36.sp, lineHeight = 43.sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 28.sp, lineHeight = 35.sp),
        headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontSize = 23.sp, lineHeight = 30.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 23.sp),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp), bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
        labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp))
    MaterialTheme(colorScheme = if (darkMode) dark else light, typography = typography, content = content)
}
