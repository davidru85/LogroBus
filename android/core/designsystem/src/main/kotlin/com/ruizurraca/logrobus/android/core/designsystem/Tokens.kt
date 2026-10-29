package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Every brand value of the Android app (NFR-MNT-04, design_guidelines.md §4 and §6.1). Until OQ-18's designs
 * arrive (LB-413), the palette is a neutral placeholder: greys plus one accessible accent. It is defined here and
 * nowhere else.
 */
@Suppress("MagicNumber") // Design tokens: the values are the point (code_style.md §2.1).
object Tokens {
    // The placeholder accent: a muted blue, and its tones for dark mode and containers.
    private val accent = Color(0xFF2F5D8A)
    private val accentLight = Color(0xFFA7C8EC)
    private val accentContainer = Color(0xFFD3E3F5)
    private val accentContainerDark = Color(0xFF2A4A6B)
    private val onAccentContainer = Color(0xFF0E2F4D)

    val lightColors: ColorScheme =
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accentContainer,
            onPrimaryContainer = onAccentContainer,
            inversePrimary = accentLight,
            secondary = Color(0xFF5C5C5C),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE3E3E3),
            onSecondaryContainer = Color(0xFF1B1B1B),
            tertiary = Color(0xFF5C5C5C),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFE3E3E3),
            onTertiaryContainer = Color(0xFF1B1B1B),
            background = Color(0xFFFCFCFC),
            onBackground = Color(0xFF1B1B1B),
            surface = Color(0xFFFCFCFC),
            onSurface = Color(0xFF1B1B1B),
            surfaceVariant = Color(0xFFE3E3E3),
            onSurfaceVariant = Color(0xFF464646),
            surfaceTint = Color(0xFF5C5C5C),
            inverseSurface = Color(0xFF303030),
            inverseOnSurface = Color(0xFFF1F1F1),
            error = Color(0xFFB3261E),
            onError = Color.White,
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
            outline = Color(0xFF767676),
            outlineVariant = Color(0xFFC6C6C6),
            scrim = Color.Black,
            surfaceBright = Color(0xFFFCFCFC),
            surfaceDim = Color(0xFFDADADA),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF6F6F6),
            surfaceContainer = Color(0xFFF0F0F0),
            surfaceContainerHigh = Color(0xFFEAEAEA),
            surfaceContainerHighest = Color(0xFFE3E3E3),
        )

    val darkColors: ColorScheme =
        darkColorScheme(
            primary = accentLight,
            onPrimary = onAccentContainer,
            primaryContainer = accentContainerDark,
            onPrimaryContainer = accentContainer,
            inversePrimary = accent,
            secondary = Color(0xFFC6C6C6),
            onSecondary = Color(0xFF2E2E2E),
            secondaryContainer = Color(0xFF464646),
            onSecondaryContainer = Color(0xFFE3E3E3),
            tertiary = Color(0xFFC6C6C6),
            onTertiary = Color(0xFF2E2E2E),
            tertiaryContainer = Color(0xFF464646),
            onTertiaryContainer = Color(0xFFE3E3E3),
            background = Color(0xFF121212),
            onBackground = Color(0xFFE3E3E3),
            surface = Color(0xFF121212),
            onSurface = Color(0xFFE3E3E3),
            surfaceVariant = Color(0xFF464646),
            onSurfaceVariant = Color(0xFFC6C6C6),
            surfaceTint = Color(0xFFC6C6C6),
            inverseSurface = Color(0xFFE3E3E3),
            inverseOnSurface = Color(0xFF303030),
            error = Color(0xFFF2B8B5),
            onError = Color(0xFF601410),
            errorContainer = Color(0xFF8C1D18),
            onErrorContainer = Color(0xFFF9DEDC),
            outline = Color(0xFF909090),
            outlineVariant = Color(0xFF464646),
            scrim = Color.Black,
            surfaceBright = Color(0xFF383838),
            surfaceDim = Color(0xFF121212),
            surfaceContainerLowest = Color(0xFF0D0D0D),
            surfaceContainerLow = Color(0xFF1B1B1B),
            surfaceContainer = Color(0xFF1F1F1F),
            surfaceContainerHigh = Color(0xFF2A2A2A),
            surfaceContainerHighest = Color(0xFF353535),
        )

    /** The status colours of the light and dark schemes, such as the stale warning (design_guidelines.md §6.1). */
    val lightStatus = StatusColors(warning = Color(0xFF8A5300))
    val darkStatus = StatusColors(warning = Color(0xFFF2B861))

    /** Material's type scale, emphasized styles included, on the system font until OQ-18. */
    val typography = Typography()

    /** Cards use 12 dp corners (spec §7.5). */
    val shapes = Shapes(medium = RoundedCornerShape(12.dp))

    /** A line badge's corners: an identity element that never morphs (design_guidelines.md §4). */
    val lineBadgeCorner = 6.dp

    /** Keeps a line badge's code off its edges when the font grows. */
    val lineBadgeVerticalPadding = 2.dp

    /** The spacing scale and the screen margin (design_guidelines.md §6.1). */
    object Spacing {
        val xs = 4.dp
        val s = 8.dp
        val m = 12.dp
        val l = 16.dp
        val xl = 24.dp
        val xxl = 32.dp
        val screenMargin = 16.dp
    }

    /** The smallest touch target (spec §7.5, AGENTS.md §7). */
    val minTouchTarget = 48.dp
}
