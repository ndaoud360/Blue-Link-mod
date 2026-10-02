package com.bluedeck.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode(val storageKey: String, val label: String) {
    SYSTEM("system", "System default"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}

// Legacy names kept so untouched screens keep compiling.
val SuccessGreen = Color(0xFF4F7D69)
val WarningAmber = Color(0xFFB7791F)
val ErrorRed = Color(0xFFB3432F)
val ChargingGreen = Color(0xFF5E9C82)

data class BlueDeckDynamicColors(
    val success: Color = SuccessGreen,
    val warning: Color = WarningAmber,
    val error: Color = ErrorRed,
    val charging: Color = ChargingGreen,
    val commandBanner: Color = Color(0xFF1C1F21),
    val dashboardCardBlend: Color = Color(0xFF23272A)
)

val LocalBlueDeckDynamicColors = staticCompositionLocalOf { BlueDeckDynamicColors() }

/**
 * Design tokens for the Ioniq 6 redesign. Sage accent on graphite (dark) or
 * warm stone (light), matching the supplied concept pair.
 */
@Immutable
data class DeckColors(
    val isDark: Boolean,
    val background: Color,
    val card: Color,
    val cardRaised: Color,
    val cardPressed: Color,
    val hairline: Color,
    val highlight: Color,
    val shadow: Color,
    val text: Color,
    val textMuted: Color,
    val textFaint: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val caution: Color,
    val cautionSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val carBody: Color,
    val carGlass: Color
)

val DarkDeck = DeckColors(
    isDark = true,
    background = Color(0xFF141618),
    card = Color(0xFF1C1F21),
    cardRaised = Color(0xFF23272A),
    cardPressed = Color(0xFF181A1C),
    hairline = Color(0xFF2C3134),
    highlight = Color(0x14FFFFFF),
    shadow = Color(0x66000000),
    text = Color(0xFFE6E9E7),
    textMuted = Color(0xFF9AA39F),
    textFaint = Color(0xFF6A726E),
    accent = Color(0xFF8FB5A3),
    accentSoft = Color(0x268FB5A3),
    onAccent = Color(0xFF102119),
    caution = Color(0xFFE0B36E),
    cautionSoft = Color(0x24E0B36E),
    danger = Color(0xFFE38B7C),
    dangerSoft = Color(0x24E38B7C),
    carBody = Color(0xFF5E8C77),
    carGlass = Color(0xFF1E2A26)
)

val LightDeck = DeckColors(
    isDark = false,
    background = Color(0xFFEAE8E3),
    card = Color(0xFFF4F3EF),
    cardRaised = Color(0xFFFBFAF8),
    cardPressed = Color(0xFFE4E2DC),
    hairline = Color(0xFFDAD7D0),
    highlight = Color(0xB3FFFFFF),
    shadow = Color(0x26706A5C),
    text = Color(0xFF1D2220),
    textMuted = Color(0xFF5F6763),
    textFaint = Color(0xFF8C928E),
    accent = Color(0xFF4F7D69),
    accentSoft = Color(0x1F4F7D69),
    onAccent = Color(0xFFFFFFFF),
    caution = Color(0xFF9A6216),
    cautionSoft = Color(0x1F9A6216),
    danger = Color(0xFFB3432F),
    dangerSoft = Color(0x1AB3432F),
    carBody = Color(0xFF4F7D69),
    carGlass = Color(0xFF2B3632)
)

val LocalDeckColors = staticCompositionLocalOf { DarkDeck }

object DeckTheme {
    val colors: DeckColors
        @Composable get() = LocalDeckColors.current
}

private fun DeckColors.toScheme(): ColorScheme =
    if (isDark) {
        darkColorScheme(
            primary = accent, onPrimary = onAccent,
            primaryContainer = Color(0xFF2B3D35), onPrimaryContainer = Color(0xFFCFE3D9),
            secondary = textMuted, onSecondary = background,
            secondaryContainer = cardRaised, onSecondaryContainer = text,
            tertiary = accent, onTertiary = onAccent,
            tertiaryContainer = Color(0xFF2B3D35), onTertiaryContainer = Color(0xFFCFE3D9),
            error = danger, onError = Color(0xFF3A0F08),
            errorContainer = Color(0xFF4A231C), onErrorContainer = Color(0xFFFFDAD3),
            background = background, onBackground = text,
            surface = card, onSurface = text,
            surfaceVariant = cardRaised, onSurfaceVariant = textMuted,
            outline = Color(0xFF4A5155), outlineVariant = hairline,
            inverseSurface = text, inverseOnSurface = background, inversePrimary = Color(0xFF4F7D69),
            surfaceTint = Color.Transparent, scrim = Color.Black,
            surfaceContainerLowest = background, surfaceContainerLow = card,
            surfaceContainer = card, surfaceContainerHigh = cardRaised,
            surfaceContainerHighest = Color(0xFF2A2F32)
        )
    } else {
        lightColorScheme(
            primary = accent, onPrimary = onAccent,
            primaryContainer = Color(0xFFD5E5DC), onPrimaryContainer = Color(0xFF0E2A1E),
            secondary = textMuted, onSecondary = Color.White,
            secondaryContainer = cardPressed, onSecondaryContainer = text,
            tertiary = accent, onTertiary = onAccent,
            tertiaryContainer = Color(0xFFD5E5DC), onTertiaryContainer = Color(0xFF0E2A1E),
            error = danger, onError = Color.White,
            errorContainer = Color(0xFFF6DAD3), onErrorContainer = Color(0xFF410C02),
            background = background, onBackground = text,
            surface = card, onSurface = text,
            surfaceVariant = cardPressed, onSurfaceVariant = textMuted,
            outline = Color(0xFF8C928E), outlineVariant = hairline,
            inverseSurface = Color(0xFF2B302E), inverseOnSurface = background, inversePrimary = Color(0xFF8FB5A3),
            surfaceTint = Color.Transparent, scrim = Color.Black,
            surfaceContainerLowest = cardRaised, surfaceContainerLow = card,
            surfaceContainer = card, surfaceContainerHigh = cardRaised,
            surfaceContainerHighest = cardPressed
        )
    }

/** When Material You is enabled, keep the deck structure but take accents from the wallpaper. */
private fun DeckColors.withDynamic(scheme: ColorScheme): DeckColors = copy(
    accent = scheme.primary,
    accentSoft = scheme.primary.copy(alpha = if (isDark) 0.15f else 0.12f),
    onAccent = scheme.onPrimary,
    carBody = scheme.primary
)

@Composable
fun BlueDeckTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val baseDeck = if (darkTheme) DarkDeck else LightDeck
    val dynamicScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else null
    val deck = dynamicScheme?.let { baseDeck.withDynamic(it) } ?: baseDeck
    val colorScheme = deck.toScheme().let { scheme ->
        dynamicScheme?.let { scheme.copy(primary = it.primary, onPrimary = it.onPrimary) } ?: scheme
    }

    val legacy = BlueDeckDynamicColors(
        success = deck.accent,
        warning = deck.caution,
        error = deck.danger,
        charging = deck.accent,
        commandBanner = deck.cardRaised,
        dashboardCardBlend = deck.card
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalBlueDeckDynamicColors provides legacy,
        LocalDeckColors provides deck
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BlueDeckTypography,
            content = content
        )
    }
}
