package com.example.detoxxing.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF121212)
val InputBackground = Color(0xFF2C2C2C)
val TextWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFFAAAAAA)

@Composable
fun KkTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(background = DarkBackground, surface = DarkBackground, primary = TextWhite, onPrimary = Color.Black), content = content)
}