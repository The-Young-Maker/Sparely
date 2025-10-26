package com.example.sparely.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Defines the spacing scale used across the Sparely design system.
 */
data class SparelySpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 40.dp
)

val LocalSpacing = staticCompositionLocalOf { SparelySpacing() }

val MaterialTheme.spacing: SparelySpacing
    @Composable
    get() = LocalSpacing.current
