package com.sukashawarma.superapp.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SukaColorScheme = lightColorScheme(
    primary = SukaOrange,
    onPrimary = Color.White,
    secondary = SukaBrown,
    background = SukaCream,
    surface = Color.White,
    error = StatusRed,
)

@Composable
fun SukaSuperappTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SukaColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
