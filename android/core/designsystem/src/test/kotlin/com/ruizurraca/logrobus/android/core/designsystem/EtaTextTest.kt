package com.ruizurraca.logrobus.android.core.designsystem

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ruizurraca.logrobus.core.common.color.contrastRatio
import com.ruizurraca.logrobus.core.presentation.EtaDelay
import com.ruizurraca.logrobus.core.presentation.EtaDisplay
import com.ruizurraca.logrobus.core.presentation.EtaUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Spec §5.7, design_guidelines.md §6.3 and NFR-A11Y-02: every ETA case, drawn and spoken, in Spanish and English. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EtaTextTest {
    @get:Rule
    val compose = createComposeRule()

    private val resources get() = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    @Config(qualifiers = "en")
    fun lb011_eachEtaCaseShowsItsValueAndSecondaryLine() {
        assertEquals(everyCase.map { it.english }, everyCase.map { resources.etaLines(it.eta) })
    }

    @Test
    @Config(qualifiers = "es")
    fun lb011_eachEtaCaseShowsItsValueAndSecondaryLine_inSpanish() {
        assertEquals(everyCase.map { it.spanish }, everyCase.map { resources.etaLines(it.eta) })
    }

    @Test
    @Config(qualifiers = "en")
    fun lb011_eachEtaCaseIsSpoken() {
        assertEquals(everyCase.map { it.englishSpoken }, everyCase.map { resources.etaSpoken(it.eta) })
    }

    @Test
    @Config(qualifiers = "es")
    fun lb011_eachEtaCaseIsSpoken_inSpanish() {
        assertEquals(everyCase.map { it.spanishSpoken }, everyCase.map { resources.etaSpoken(it.eta) })
    }

    @Test
    @Config(qualifiers = "en")
    fun lb011_talkBackHearsTheSpokenLabel() {
        compose.setContent { LogroBusTheme { EtaText(eta(EtaDisplay.Minutes(4)), Modifier.testTag("eta")) } }

        compose.onNodeWithTag("eta").assertContentDescriptionEquals("in 4 minutes")
    }

    @Test
    fun lb011_etasUseTabularFigures() {
        var style: TextStyle? = null
        compose.setContent { LogroBusTheme { style = etaStyle } }

        assertEquals("tnum", style?.fontFeatureSettings)
    }

    @Test
    fun lb011_nowPulsesWithAnimationsOn_andHoldsStillWithThemOff() {
        assertEquals(0.8f, nowAlpha(pulse = 0.8f, animationsEnabled = true))
        assertEquals(1f, nowAlpha(pulse = 0.8f, animationsEnabled = false))
    }

    @Test
    fun lb011_thePulseKeepsNowReadable() {
        // NFR-A11Y-06: at its faintest, "Now" still reaches 4.5:1 on the surfaces a row sits on.
        val faintest =
            listOf(Tokens.lightColors, Tokens.darkColors).flatMap { scheme ->
                listOf(scheme.surface, scheme.surfaceContainerHighest).map { background ->
                    val text = scheme.onSurface.copy(alpha = PULSE_MIN_ALPHA).compositeOver(background)
                    contrastRatio(text.toArgb().toLong() and 0xFFFFFFFF, background.toArgb().toLong() and 0xFFFFFFFF)
                }
            }
        assertTrue("$faintest", faintest.all { it >= MINIMUM_CONTRAST })
    }

    @Test
    // A tall window, so every case fits at 200 %.
    @Config(qualifiers = "en-w360dp-h1600dp", sdk = [SCREENSHOT_SDK])
    fun lb011_everyEtaCase_screenshots() {
        captureRoboImage("src/test/screenshots/EtaText_light.png") { EveryEta(darkTheme = false) }
        captureRoboImage("src/test/screenshots/EtaText_dark.png") { EveryEta(darkTheme = true) }
        captureRoboImage("src/test/screenshots/EtaText_fontScale2.png") {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                EveryEta(darkTheme = false)
            }
        }
    }

    @Composable
    private fun EveryEta(darkTheme: Boolean) {
        LogroBusTheme(darkTheme) {
            Surface(Modifier.fillMaxSize()) {
                Column(
                    Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    everyCase.forEach { EtaText(it.eta) }
                }
            }
        }
    }

    /** An ETA with what it shows and says in English and Spanish. */
    private data class Case(
        val eta: EtaUi,
        val english: EtaLines,
        val englishSpoken: String,
        val spanish: EtaLines,
        val spanishSpoken: String,
    )

    private companion object {
        const val MINIMUM_CONTRAST = 4.5

        fun eta(
            display: EtaDisplay,
            lowConfidence: Boolean = false,
            scheduled: Boolean = false,
            delay: EtaDelay? = null,
            cancelled: Boolean = false,
        ) = EtaUi(display, lowConfidence, scheduled, delay, cancelled)

        val minutes = EtaDisplay.Minutes(4)
        val clock = EtaDisplay.ClockTime(9, 5)

        // Every case of spec §5.7 that shows, and the combinations EtaUi allows. The screen and TalkBack always agree.
        val everyCase =
            listOf(
                Case(
                    eta(EtaDisplay.Now),
                    EtaLines("Now", null),
                    "arriving now",
                    EtaLines("Ahora", null),
                    "llega ahora",
                ),
                Case(
                    eta(EtaDisplay.Minutes(1)),
                    EtaLines("1 min", null),
                    "in 1 minute",
                    EtaLines("1 min", null),
                    "en 1 minuto",
                ),
                Case(eta(minutes), EtaLines("4 min", null), "in 4 minutes", EtaLines("4 min", null), "en 4 minutos"),
                Case(eta(clock), EtaLines("09:05", null), "at 09:05", EtaLines("09:05", null), "a las 09:05"),
                // A Búho time: "a la una", in the singular.
                Case(
                    eta(EtaDisplay.ClockTime(1, 5)),
                    EtaLines("01:05", null),
                    "at 01:05",
                    EtaLines("01:05", null),
                    "a la 01:05",
                ),
                Case(
                    eta(minutes, lowConfidence = true),
                    EtaLines("~4 min", null),
                    "approximately in 4 minutes",
                    EtaLines("~4 min", null),
                    "aproximadamente en 4 minutos",
                ),
                Case(
                    eta(EtaDisplay.Now, lowConfidence = true),
                    EtaLines("~Now", null),
                    "arriving now, approximately",
                    EtaLines("~Ahora", null),
                    "llega ahora, aproximadamente",
                ),
                Case(
                    eta(clock, scheduled = true),
                    EtaLines("09:05", "Scheduled"),
                    "scheduled at 09:05",
                    EtaLines("09:05", "Programado"),
                    "programado a las 09:05",
                ),
                Case(
                    eta(minutes, scheduled = true),
                    EtaLines("4 min", "Scheduled"),
                    "in 4 minutes, scheduled",
                    EtaLines("4 min", "Programado"),
                    "en 4 minutos, programado",
                ),
                // Scheduled hides the delay, on screen and in speech.
                Case(
                    eta(clock, scheduled = true, delay = EtaDelay.Late(3)),
                    EtaLines("09:05", "Scheduled"),
                    "scheduled at 09:05",
                    EtaLines("09:05", "Programado"),
                    "programado a las 09:05",
                ),
                Case(
                    eta(minutes, delay = EtaDelay.Late(3)),
                    EtaLines("4 min", "+3 min late"),
                    "in 4 minutes, 3 minutes late",
                    EtaLines("4 min", "+3 min tarde"),
                    "en 4 minutos, 3 minutos tarde",
                ),
                Case(
                    eta(minutes, delay = EtaDelay.Early(1)),
                    EtaLines("4 min", "1 min early"),
                    "in 4 minutes, 1 minute early",
                    EtaLines("4 min", "1 min antes"),
                    "en 4 minutos, 1 minuto antes",
                ),
                Case(
                    eta(minutes, cancelled = true),
                    EtaLines("4 min", "Cancelled"),
                    "cancelled",
                    EtaLines("4 min", "Cancelado"),
                    "cancelado",
                ),
            )
    }
}
