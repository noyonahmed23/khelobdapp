package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// Strict Sharp Design - Avoid Border Radius across all components
private val SharpShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
)

private val DarkColorScheme = darkColorScheme(
    primary = KheloGreen,
    onPrimary = DarkBg,
    primaryContainer = KheloGreenContainer,
    onPrimaryContainer = OnKheloGreenContainer,
    secondary = EsportsGold,
    onSecondary = DarkBg,
    secondaryContainer = Color(0xFF3E2E04),
    onSecondaryContainer = Color(0xFFFFE082),
    tertiary = EsportsCyan,
    onTertiary = DarkBg,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = EsportsRed,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Esports platform defaults to immersive dark mode
    dynamicColor: Boolean = false, // Keep consistent Khelo BD branding
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DarkBg.toArgb()
                window.navigationBarColor = DarkBg.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SharpShapes,
        content = content
    )
}
