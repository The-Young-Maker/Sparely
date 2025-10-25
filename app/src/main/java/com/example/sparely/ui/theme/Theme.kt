package com.example.sparely.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = Color(0xFF00382F),
    primaryContainer = TealPrimaryDarkContainer,
    onPrimaryContainer = MistyWhite,
    secondary = MintSecondaryDark,
    onSecondary = Color(0xFF003918),
    secondaryContainer = MintSecondaryDarkContainer,
    onSecondaryContainer = MistyWhite,
    tertiary = AzureTertiaryDark,
    onTertiary = Color(0xFF00344C),
    tertiaryContainer = AzureTertiaryDarkContainer,
    onTertiaryContainer = MistyWhite,
    background = AbyssBackground,
    onBackground = MistyWhite,
    surface = MidnightSurface,
    onSurface = MistyWhite,
    surfaceVariant = DeepCurrentSurfaceVariant,
    onSurfaceVariant = PearlOnVariant,
    outline = ReefOutline,
    outlineVariant = AbyssOutlineVariant,
    scrim = Color(0xFF000000),
    inverseSurface = MistyWhite,
    inverseOnSurface = DeepNavy,
    inversePrimary = TealPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealPrimaryContainer,
    onPrimaryContainer = Color(0xFF00382F),
    secondary = MintSecondary,
    onSecondary = Color(0xFF003916),
    secondaryContainer = MintSecondaryContainer,
    onSecondaryContainer = Color(0xFF00210B),
    tertiary = AzureTertiary,
    onTertiary = Color.White,
    tertiaryContainer = AzureTertiaryContainer,
    onTertiaryContainer = Color(0xFF00233A),
    background = MistBackground,
    onBackground = DeepNavy,
    surface = WhisperSurface,
    onSurface = DeepNavy,
    surfaceVariant = SeafoamSurfaceVariant,
    onSurfaceVariant = SlateOnVariant,
    outline = TideOutline,
    outlineVariant = DuneOutlineVariant,
    scrim = Color(0xFF000000),
    inverseSurface = DeepNavy,
    inverseOnSurface = MistyWhite,
    inversePrimary = TealPrimaryDark
)

@Composable
fun SparelyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalSpacing provides SparelySpacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = ExpressiveShapes,
            content = content
        )
    }
}