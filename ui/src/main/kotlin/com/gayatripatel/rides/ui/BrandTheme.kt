package com.gayatripatel.rides.ui

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.gayatripatel.rides.core.BrandConfig
import java.util.Locale

/**
 * Colours come from the brand config, not from resources, so adding a white-label client is a config change.
 * Dynamic colour is deliberately off: a brand must look the same on every phone.
 */
@Composable
fun BrandTheme(brand: BrandConfig, darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val primary = Color(brand.primaryArgb)
    val accent = Color(brand.accentArgb)
    val onPrimary = if (primary.luminance() > 0.5f) Color.Black else Color.White
    val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = if (primary.luminance() < 0.15f) accent else primary.copy(alpha = 1f),
            onPrimary = if (primary.luminance() < 0.15f) onAccent else onPrimary,
            secondary = accent,
            onSecondary = onAccent,
            secondaryContainer = accent.copy(alpha = 0.25f),
            background = Color(0xFF111315),
            surface = Color(0xFF111315),
            surfaceContainerLow = Color(0xFF191B1E),
            surfaceContainer = Color(0xFF1E2023),
            surfaceContainerHigh = Color(0xFF282A2E),
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primary.copy(alpha = 0.12f),
            onPrimaryContainer = primary,
            secondary = accent,
            onSecondary = onAccent,
            secondaryContainer = accent.copy(alpha = 0.22f),
            onSecondaryContainer = Color(0xFF1A1C1E),
            background = Color(0xFFF8F9FA),
            surface = Color.White,
            surfaceContainerLow = Color(0xFFF4F5F6),
            surfaceContainer = Color(0xFFEEF0F2),
            surfaceContainerHigh = Color(0xFFE6E8EB),
        )
    }
    BrandLocale(brand) { MaterialTheme(colorScheme = scheme, content = content) }
}

/** Starts the UI in the brand's language (and direction) regardless of the device locale. */
@Composable
fun BrandLocale(brand: BrandConfig, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val baseConfig = LocalConfiguration.current
    val localized = remember(brand.language, baseConfig) {
        val config = Configuration(baseConfig).apply { setLocale(Locale.forLanguageTag(brand.language)) }
        base.createConfigurationContext(config) to config
    }
    CompositionLocalProvider(
        LocalContext provides localized.first,
        LocalConfiguration provides localized.second,
        LocalLayoutDirection provides if (brand.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        content = content,
    )
}
