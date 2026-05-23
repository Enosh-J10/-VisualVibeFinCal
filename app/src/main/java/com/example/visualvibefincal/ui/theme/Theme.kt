package com.example.visualvibefincal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun FinCalcTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF00D1B2),
            background = Color(0xFF0F2027),
            surface = Color(0xFF203A43),
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF00D1B2),
            background = Color.White,
            surface = Color(0xFFF0F4F8),
            onBackground = Color.Black,
            onSurface = Color.Black
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
