package com.ruizurraca.logrobus.android.feature.planner

import android.content.Context
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.android.core.testing.assertAccessible
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * AGENTS.md §9 and LB-406: the departure's pickers (LB-303), the date's dialog and then the time's, in light, dark and
 * at 200 % font, in Spanish.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es-w411dp-h891dp", sdk = [SCREENSHOT_SDK])
class DepartureDialogsScreenshotTest(
    private val dialog: String,
    private val variant: String,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lb303_departureDialog_screenshot() {
        show()

        compose.onNode(isDialog()).captureRoboImage("src/test/screenshots/Planner_${dialog}Dialog_$variant.png")
    }

    /** LB-406 (NFR-A11Y): the dialog's controls, labelled, large enough, and whole at 200 %. */
    @Test
    fun lb406_departureDialog_isAccessible() {
        // Labels, touch targets and text do not change with the colours: dark repeats light.
        assumeTrue(variant != DARK)
        show()

        compose.assertAccessible(knownIssues = materialIssues.getValue("${dialog}_$variant"))
    }

    private fun show() {
        // A dialog is a window of its own, whose density comes from the configuration, not from the screen's.
        if (variant == FONT_SCALE_2) RuntimeEnvironment.setFontScale(2f)
        val picker = PlannerPreviewData.picking.departurePicker!!
        compose.setContent {
            LogroBusTheme(darkTheme = variant == DARK) {
                DepartureTimeDialogs(picker, onPicked = {}, onCancel = {})
            }
        }
        // The time's dialog follows the date's OK.
        if (dialog == TIME) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            compose.onNodeWithText(context.getString(R.string.plan_departure_ok)).performClick()
            compose.waitForIdle()
        }
    }

    companion object {
        /**
         * Material's own issues inside its pickers (Compose Material 3 1.5.0-beta02), which the screen cannot change
         * (design_guidelines.md §3.3). For the owner to confirm in LB-303's pull request.
         * - **The date's previous-month arrow at 200 %:** the month's name takes the room beside it, 41 dp of 48.
         * - **The time's ":" between its fields:** a glyph in a box 24 dp wide, whose text layout overflows it, although
         *   it shows whole.
         */
        private val materialIssues =
            mapOf(
                "date_light" to emptySet(),
                "date_fontScale2" to setOf("Cambiar al mes anterior: its touch target is 41 × 48 dp, under 48 dp"),
                "time_light" to setOf("\":\" is cut short"),
                "time_fontScale2" to emptySet(),
            )

        private const val TIME = "time"
        private const val DARK = "dark"
        private const val FONT_SCALE_2 = "fontScale2"

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<String>> =
            listOf("date", TIME).flatMap { dialog ->
                listOf("light", DARK, FONT_SCALE_2).map { arrayOf(dialog, it) }
            }
    }
}

/**
 * NFR-A11Y-03: the departure's time dialog at 200 % on SDK 33, whose font scaling is linear, unlike 34's on, and so
 * makes the time's large digits twice their size.
 */
@RunWith(org.robolectric.RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es-w411dp-h891dp", sdk = [LINEAR_FONT_SCALING_SDK])
class DepartureTimeDialogLinearScalingTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lb406_departureTimeDialog_atLinear200Percent_isAccessible() {
        RuntimeEnvironment.setFontScale(2f)
        compose.setContent {
            LogroBusTheme {
                DepartureTimeDialogs(
                    PlannerPreviewData.picking.departurePicker!!,
                    onPicked = {},
                    onCancel = {},
                )
            }
        }
        val context = ApplicationProvider.getApplicationContext<Context>()
        compose.onNodeWithText(context.getString(R.string.plan_departure_ok)).performClick()
        compose.waitForIdle()

        compose.onNode(isDialog()).captureRoboImage("src/test/screenshots/Planner_timeDialog_linearFontScale2.png")
        compose.assertAccessible()
    }
}

/** The last SDK whose font scaling is linear: from 34 on, large text grows less. */
private const val LINEAR_FONT_SCALING_SDK = 33
