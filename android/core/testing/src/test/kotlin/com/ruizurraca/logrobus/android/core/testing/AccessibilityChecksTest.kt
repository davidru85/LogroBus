package com.ruizurraca.logrobus.android.core.testing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** LB-406 (NFR-A11Y-02, NFR-A11Y-03, NFR-A11Y touch targets): the checks every Android screen test can run. */
@RunWith(AndroidJUnit4::class)
// Text is only measured with native graphics; Robolectric's legacy graphics give it no width.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccessibilityChecksTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lb406_labelledControlsOfTheMinimumSizeAndTextThatFits_pass() {
        compose.setContent {
            Column {
                Box(Modifier.size(48.dp).clickable {}.semantics { contentDescription = "Retry" })
                // A small control whose touch area Compose widens to the minimum.
                Box(Modifier.size(24.dp).clickable {}.semantics { contentDescription = "Clear search" })
                // Text that wraps instead of being cut.
                Text("Hospital San Pedro - El Arco", Modifier.width(60.dp))
            }
        }

        compose.assertAccessible()
    }

    @Test
    fun lb406_textNarrowerThanTheRoomItMayTake_isNotCut() {
        // Semantics lay a text's paragraph out at the widest the text may be, wider than its own width.
        compose.setContent { Text("Línea") }

        compose.assertAccessible()
    }

    @Test
    fun lb406_aControlScrolledOutOfView_isNotChecked() {
        // TalkBack does not see a node outside the screen, and its touch area is empty there.
        compose.setContent {
            Column(Modifier.height(100.dp).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(300.dp))
                Box(Modifier.size(48.dp).clickable {}.semantics { contentDescription = "Retry" })
            }
        }

        compose.assertAccessible()
    }

    @Test
    fun lb406_aControlWithoutALabel_fails() {
        compose.setContent { Box(Modifier.size(48.dp).clickable {}) }

        val failure = assertThrows(AssertionError::class.java) { compose.assertAccessible() }

        assertTrue(failure.message, "no label" in failure.message.orEmpty())
    }

    @Test
    fun lb406_aTouchTargetUnder48dp_fails_namingTheControl() {
        // Compose widens the touch area of a click only, so a long press alone keeps the layout's 24 dp.
        compose.setContent {
            Box(
                Modifier.size(24.dp).semantics {
                    contentDescription = "Hold to edit"
                    onLongClick { true }
                },
            )
        }

        val failure = assertThrows(AssertionError::class.java) { compose.assertAccessible() }

        assertTrue(failure.message, "Hold to edit" in failure.message.orEmpty())
        assertTrue(failure.message, "48 dp" in failure.message.orEmpty())
    }

    @Test
    fun lb406_textCutShortWithAnEllipsis_fails_namingTheText() {
        compose.setContent {
            Text("Hospital San Pedro", Modifier.width(40.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        val failure = assertThrows(AssertionError::class.java) { compose.assertAccessible() }

        assertTrue(failure.message, "Hospital San Pedro" in failure.message.orEmpty())
    }

    @Test
    fun lb406_textThatOverflowsItsBox_fails() {
        compose.setContent { Text("B1", Modifier.size(8.dp), softWrap = false, overflow = TextOverflow.Clip) }

        val failure = assertThrows(AssertionError::class.java) { compose.assertAccessible() }

        assertTrue(failure.message, "B1" in failure.message.orEmpty())
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.LEGACY)
    fun lb406_withLegacyGraphics_whichMeasureNoText_theChecksFail_insteadOfPassingUnseen() {
        compose.setContent { Text("Hospital San Pedro") }

        val failure = assertThrows(AssertionError::class.java) { compose.assertAccessible() }

        assertTrue(failure.message, "GraphicsMode.Mode.NATIVE" in failure.message.orEmpty())
    }
}
