package com.outfuseplayer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val Obsidian = Color(0xFF0B0D10)
val Surface = Color(0xFF141820)
val Surface2 = Color(0xFF1C222D)
val SurfaceGlass = Color(0xCC151A22)
val PrimaryOrange = Color(0xFFFF7A00)
val PrimaryAmber = Color(0xFFFFB441)
val ElectricBlue = Color(0xFF7AA7FF)
val SoftTeal = Color(0xFF5FD3C5)
val TextPrimary = Color(0xFFF4F6FA)
val TextMuted = Color(0xFF9AA4B2)
val Danger = Color(0xFFFF6B6B)

private val OutfuseDarkScheme = darkColorScheme(
    primary = PrimaryOrange,
    onPrimary = Color.White,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    tertiary = SoftTeal,
    background = Obsidian,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextMuted,
    error = Danger
)

private val OutfuseLightScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = Color.White,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    tertiary = SoftTeal,
    background = Color(0xFFF5F6F8),
    onBackground = Color(0xFF12161D),
    surface = Color.White,
    onSurface = Color(0xFF12161D),
    surfaceVariant = Color(0xFFE8EBF0),
    onSurfaceVariant = Color(0xFF556070),
    error = Danger
)

@Composable
fun OutfuseTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = if (darkTheme) Obsidian.toArgb() else Color.White.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.navigationBarDividerColor = Color.Transparent.toArgb()
            }
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) OutfuseDarkScheme else OutfuseLightScheme,
        typography = OutfuseTypography,
        shapes = OutfuseShapes,
        content = content
    )
}

@Composable
fun PreviewOutfuseTheme(content: @Composable () -> Unit) {
    OutfuseTheme(content = content)
}


