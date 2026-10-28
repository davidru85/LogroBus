package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ruizurraca.logrobus.core.common.color.contrastRatio
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/** NFR-MNT-04, NFR-A11Y-06 and OQ-18: the placeholder palette is neutral, and all its text is readable. */
class TokensTest {
    private val schemes = mapOf("light" to Tokens.lightColors, "dark" to Tokens.darkColors)

    @Test
    fun lb011_everyTextColourOfThePlaceholderSchemesReaches4point5To1() {
        val unreadable =
            schemes.flatMap { (name, scheme) ->
                scheme.textPairs().mapNotNull { (role, colours) ->
                    val ratio = contrastRatio(colours.first.argb, colours.second.argb)
                    "$name $role: %.2f".format(Locale.ROOT, ratio).takeIf { ratio < MINIMUM_CONTRAST }
                }
            }
        assertEquals(emptyList<String>(), unreadable)
    }

    @Test
    fun lb011_theWarningColourIsReadableOnEverySurface() {
        val unreadable =
            mapOf(
                "light" to (Tokens.lightStatus to Tokens.lightColors),
                "dark" to (Tokens.darkStatus to Tokens.darkColors),
            ).flatMap { (name, colours) ->
                val (status, scheme) = colours
                listOf(scheme.surface, scheme.surfaceContainerHighest).mapNotNull { background ->
                    val ratio = contrastRatio(status.warning.argb, background.argb)
                    "$name: %.2f".format(Locale.ROOT, ratio).takeIf { ratio < MINIMUM_CONTRAST }
                }
            }
        assertEquals(emptyList<String>(), unreadable)
    }

    @Test
    fun lb011_thePlaceholderSurfacesAreGrey() {
        // Greys plus one accent (design_guidelines.md §4): no tint on the surfaces, unlike Material's baseline.
        val tinted =
            schemes.flatMap { (name, scheme) ->
                scheme
                    .surfaces()
                    .filterValues { !it.isGrey() }
                    .keys
                    .map { "$name $it" }
            }
        assertEquals(emptyList<String>(), tinted)
    }

    private fun ColorScheme.textPairs(): Map<String, Pair<Color, Color>> =
        mapOf(
            "primary" to (primary to onPrimary),
            "primaryContainer" to (primaryContainer to onPrimaryContainer),
            "secondary" to (secondary to onSecondary),
            "secondaryContainer" to (secondaryContainer to onSecondaryContainer),
            "tertiary" to (tertiary to onTertiary),
            "tertiaryContainer" to (tertiaryContainer to onTertiaryContainer),
            "error" to (error to onError),
            "errorContainer" to (errorContainer to onErrorContainer),
            "background" to (background to onBackground),
            "surface" to (surface to onSurface),
            "surfaceVariant" to (surfaceVariant to onSurfaceVariant),
            "surfaceContainerHighest" to (surfaceContainerHighest to onSurfaceVariant),
            "inverseSurface" to (inverseSurface to inverseOnSurface),
        )

    private fun ColorScheme.surfaces(): Map<String, Color> =
        mapOf(
            "background" to background,
            "surface" to surface,
            "surfaceVariant" to surfaceVariant,
            "surfaceContainerLowest" to surfaceContainerLowest,
            "surfaceContainerLow" to surfaceContainerLow,
            "surfaceContainer" to surfaceContainer,
            "surfaceContainerHigh" to surfaceContainerHigh,
            "surfaceContainerHighest" to surfaceContainerHighest,
            "surfaceDim" to surfaceDim,
            "surfaceBright" to surfaceBright,
            "inverseSurface" to inverseSurface,
            "outline" to outline,
            "outlineVariant" to outlineVariant,
        )

    private val Color.argb: Long get() = toArgb().toLong() and 0xFFFFFFFF

    // Equal red, green and blue, give or take rounding.
    private fun Color.isGrey(): Boolean {
        val channels = listOf(red, green, blue)
        return channels.max() - channels.min() < GREY_TOLERANCE
    }

    private companion object {
        const val MINIMUM_CONTRAST = 4.5
        const val GREY_TOLERANCE = 0.02f
    }
}
