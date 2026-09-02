package com.zenx.yugen.play.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val YugenDarkColorScheme = darkColorScheme(
    primary = YugenPurple,
    onPrimary = TextPrimary,
    primaryContainer = YugenPurpleDark,
    onPrimaryContainer = TextPrimary,
    secondary = YugenPurpleDark,
    onSecondary = TextPrimary,
    background = YugenBackground,
    onBackground = TextPrimary,
    surface = YugenSurface,
    onSurface = TextPrimary,
    surfaceVariant = YugenSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = YugenCardBorder
)

@Composable
fun YugenPlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep false to maintain our OLED anime aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = YugenDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = YugenBackground.toArgb()
                window.navigationBarColor = YugenBackground.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}