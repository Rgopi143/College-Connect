package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PolishedDarkPrimary,
    onPrimary = PolishedDarkOnPrimary,
    primaryContainer = PolishedDarkPrimaryContainer,
    onPrimaryContainer = PolishedDarkOnPrimaryContainer,
    secondary = PolishedDarkSecondary,
    onSecondary = PolishedDarkOnSecondary,
    secondaryContainer = PolishedDarkSecondaryContainer,
    background = PolishedDarkBackground,
    surface = PolishedDarkSurface,
    onBackground = PolishedDarkOnBackground,
    onSurface = PolishedDarkOnSurface,
    surfaceVariant = PolishedDarkSurfaceVariant,
    onSurfaceVariant = PolishedDarkOnSurfaceVariant,
    outline = PolishedDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = PolishedPrimary,
    onPrimary = PolishedOnPrimary,
    primaryContainer = PolishedPrimaryContainer,
    onPrimaryContainer = PolishedOnPrimaryContainer,
    secondary = PolishedSecondary,
    onSecondary = PolishedOnSecondary,
    secondaryContainer = PolishedSecondaryContainer,
    tertiary = PolishedTertiary,
    onTertiary = PolishedOnTertiary,
    background = PolishedBackground,
    onBackground = PolishedOnBackground,
    surface = PolishedSurface,
    onSurface = PolishedOnSurface,
    surfaceVariant = PolishedSurfaceVariant,
    onSurfaceVariant = PolishedOnSurfaceVariant,
    outline = PolishedOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Force the crisp premium White Theme by default
    dynamicColor: Boolean = false, // We prioritize our custom scholastic branding!
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
