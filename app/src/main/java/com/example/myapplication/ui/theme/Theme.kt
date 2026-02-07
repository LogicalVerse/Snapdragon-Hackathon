package com.example.myapplication.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Additional color utilities
val Black = Color(0xFF000000)

// Dark Color Scheme - Primary theme for Formly
private val DarkColorScheme = darkColorScheme(
    // Primary brand color - Electric Green for CTAs and success
    primary = ElectricGreen,
    onPrimary = DeepDark,
    primaryContainer = VeryDarkGray,
    onPrimaryContainer = ElectricGreen,

    // Secondary color - Deep Blue for accents
    secondary = DeepBlue,
    onSecondary = White,
    secondaryContainer = VeryDarkGray,
    onSecondaryContainer = DeepBlue,

    // Tertiary color - Energetic Orange for warnings
    tertiary = EnergeticOrange,
    onTertiary = White,
    tertiaryContainer = VeryDarkGray,
    onTertiaryContainer = EnergeticOrange,

    // Background - Deep Dark
    background = DeepDark,
    onBackground = OffWhite,

    // Surface - Elevated Dark
    surface = ElevatedDark,
    onSurface = OffWhite,
    surfaceVariant = SubtleGray,
    onSurfaceVariant = MediumGray,

    // Semantic colors
    error = AlertRed,
    onError = White,
    errorContainer = VeryDarkGray,
    onErrorContainer = AlertRed,

    // Outline and borders
    outline = MediumGray,
    outlineVariant = SubtleGray,

    // Scrim color for dialogs/modals
    scrim = Black.copy(alpha = 0.5f)
)

// Light Color Scheme - Clean minimalistic light mode
private val LightColorScheme = lightColorScheme(
    primary = ElectricGreen,
    onPrimary = White,
    primaryContainer = ElectricGreen.copy(alpha = 0.15f),
    onPrimaryContainer = DeepDark,

    secondary = DeepBlue,
    onSecondary = White,
    secondaryContainer = DeepBlue.copy(alpha = 0.15f),
    onSecondaryContainer = DeepDark,

    tertiary = EnergeticOrange,
    onTertiary = White,
    tertiaryContainer = EnergeticOrange.copy(alpha = 0.15f),
    onTertiaryContainer = DeepDark,

    background = Color(0xFFFAFAFA),
    onBackground = DeepDark,

    surface = White,
    onSurface = DeepDark,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = MediumGray,

    error = AlertRed,
    onError = White,
    errorContainer = AlertRed.copy(alpha = 0.15f),
    onErrorContainer = AlertRed,

    outline = MediumGray,
    outlineVariant = Color(0xFFE0E0E0),

    scrim = Black.copy(alpha = 0.32f)
)

/**
 * Formly Theme with complete Material3 customization
 * Supports both dark and light modes with dynamic color on Android 12+
 */
@Composable
fun FormlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled to use our custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Use dynamic color on Android 12+ if enabled
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        // Use Formly custom themes
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FormlyTypography,
        shapes = FormlyShapes,
        content = content
    )
}

