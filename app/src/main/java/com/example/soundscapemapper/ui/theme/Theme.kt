package com.example.soundscapemapper.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = SaludPrimary,
    onPrimary = SaludOnPrimary,
    primaryContainer = SaludPrimaryContainer,
    onPrimaryContainer = SaludOnPrimaryContainer,
    secondary = SaludSecondary,
    onSecondary = SaludOnSecondary,
    secondaryContainer = SaludSecondaryContainer,
    onSecondaryContainer = SaludOnSecondaryContainer,
    tertiary = SaludTertiary,
    onTertiary = SaludOnTertiary,
    tertiaryContainer = SaludTertiaryContainer,
    onTertiaryContainer = SaludOnTertiaryContainer,
    error = SaludError,
    onError = SaludOnError,
    errorContainer = SaludErrorContainer,
    onErrorContainer = SaludOnErrorContainer,
    background = SaludBackground,
    onBackground = SaludOnBackground,
    surface = SaludSurface,
    onSurface = SaludOnSurface,
    surfaceVariant = SaludSurfaceVariant,
    onSurfaceVariant = SaludOnSurfaceVariant,
    outline = SaludOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = SaludPrimaryDark,
    onPrimary = SaludOnPrimaryDark,
    primaryContainer = SaludPrimaryContainerDark,
    onPrimaryContainer = SaludOnPrimaryContainerDark,
    secondary = SaludSecondaryDark,
    onSecondary = SaludOnSecondaryDark,
    secondaryContainer = SaludSecondaryContainerDark,
    onSecondaryContainer = SaludOnSecondaryContainerDark,
    tertiary = SaludTertiaryDark,
    onTertiary = SaludOnTertiaryDark,
    tertiaryContainer = SaludTertiaryContainerDark,
    onTertiaryContainer = SaludOnTertiaryContainerDark,
    error = SaludErrorDark,
    onError = SaludOnErrorDark,
    errorContainer = SaludErrorContainerDark,
    onErrorContainer = SaludOnErrorContainerDark,
    background = SaludBackgroundDark,
    onBackground = SaludOnBackgroundDark,
    surface = SaludSurfaceDark,
    onSurface = SaludOnSurfaceDark,
    surfaceVariant = SaludSurfaceVariantDark,
    onSurfaceVariant = SaludOnSurfaceVariantDark,
    outline = SaludOutlineDark
)

@Composable
fun SoundscapeMapperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Identidad propia de la marca: se usa la paleta de salud en lugar de color dinámico del sistema.
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
