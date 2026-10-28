package com.ruizurraca.logrobus.android.core.designsystem

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.ruizurraca.logrobus.core.presentation.EtaDelay
import com.ruizurraca.logrobus.core.presentation.EtaDisplay
import com.ruizurraca.logrobus.core.presentation.EtaUi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Spec §5.7, design_guidelines.md §6.3 and NFR-A11Y-02: every ETA case, drawn and spoken. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EtaTextTest {
    @get:Rule
    val compose = createComposeRule()

    private val resources get() = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    @Config(qualifiers = "en")
    fun lb011_eachEtaCaseShowsItsValueAndSecondaryLine() {
        val shown = everyCase.map { (eta, _) -> resources.etaLines(eta) }

        assertEquals(
            listOf(
                EtaLines("Now", null),
                EtaLines("1 min", null),
                EtaLines("4 min", null),
                EtaLines("09:05", null),
                EtaLines("~4 min", null),
                EtaLines("09:05", "Scheduled"),
                EtaLines("4 min", "+3 min late"),
                EtaLines("4 min", "1 min early"),
                EtaLines("4 min", "Cancelled"),
            ),
            shown,
        )
    }

    @Test
    @Config(qualifiers = "en")
    fun lb011_eachEtaCaseIsSpoken() {
        assertEquals(everyCase.map { it.second }, everyCase.map { (eta, _) -> resources.etaSpoken(eta) })
    }

    @Test
    @Config(qualifiers = "es")
    fun lb011_etasAreSpokenInSpanish() {
        assertEquals("llega ahora", resources.etaSpoken(eta(EtaDisplay.Now)))
        assertEquals("en 1 minuto", resources.etaSpoken(eta(EtaDisplay.Minutes(1))))
        assertEquals(
            "aproximadamente en 4 minutos, 3 minutos tarde",
            resources.etaSpoken(eta(EtaDisplay.Minutes(4), lowConfidence = true, delay = EtaDelay.Late(3))),
        )
        assertEquals("programado a las 09:05", resources.etaSpoken(eta(EtaDisplay.ClockTime(9, 5), scheduled = true)))
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
    @Config(qualifiers = "en")
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
            Surface {
                Column(
                    Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    everyCase.forEach { (eta, _) -> EtaText(eta) }
                }
            }
        }
    }

    private companion object {
        fun eta(
            display: EtaDisplay,
            lowConfidence: Boolean = false,
            scheduled: Boolean = false,
            delay: EtaDelay? = null,
            cancelled: Boolean = false,
        ) = EtaUi(display, lowConfidence, scheduled, delay, cancelled)

        // Every case of spec §5.7 that shows, with its English spoken label.
        val everyCase =
            listOf(
                eta(EtaDisplay.Now) to "arriving now",
                eta(EtaDisplay.Minutes(1)) to "in 1 minute",
                eta(EtaDisplay.Minutes(4)) to "in 4 minutes",
                eta(EtaDisplay.ClockTime(9, 5)) to "at 09:05",
                eta(EtaDisplay.Minutes(4), lowConfidence = true) to "approximately in 4 minutes",
                eta(EtaDisplay.ClockTime(9, 5), scheduled = true) to "scheduled at 09:05",
                eta(EtaDisplay.Minutes(4), delay = EtaDelay.Late(3)) to "in 4 minutes, 3 minutes late",
                eta(EtaDisplay.Minutes(4), delay = EtaDelay.Early(1)) to "in 4 minutes, 1 minute early",
                eta(EtaDisplay.Minutes(4), cancelled = true) to "cancelled",
            )
    }
}
