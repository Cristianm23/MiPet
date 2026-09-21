package com.example.mipet.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PetroleumBlue80,
    secondary = Turquoise80,
    tertiary = SoftCoral80,
    background = DarkBlueText,
    surface = DarkBlueText,
    onPrimary = DarkBlueText,
    onSecondary = DarkBlueText,
    onTertiary = DarkBlueText,
    onBackground = BluishWhite,
    onSurface = BluishWhite
)

private val LightColorScheme = lightColorScheme(
    primary = PetroleumBlue,
    secondary = Turquoise,
    tertiary = SoftCoralHighlight,
    background = BluishWhite,
    surface = SurfaceWhite,
    onPrimary = SurfaceWhite,
    onSecondary = SurfaceWhite,
    onTertiary = SurfaceWhite,
    onBackground = DarkBlueText,
    onSurface = DarkBlueText
)

@Composable
fun MiPetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set dynamicColor to false to respect our custom palette by default
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
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
