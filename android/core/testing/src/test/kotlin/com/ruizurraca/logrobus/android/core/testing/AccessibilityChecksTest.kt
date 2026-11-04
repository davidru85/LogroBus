package com.ruizurraca.logrobus.android.core.testing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.hideFromAccessibility
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
                // A small control with room around it, into which Compose widens its touch area.
                Box(
                    Modifier
                        .padding(12.dp)
                        .size(24.dp)
                        .clickable {}
                        .semantics { contentDescription = "Clear search" },
                )
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
    fun lb406_aLabelledControlScrolledOutOfView_passes() {
        // Its touch area is empty outside the screen; it is measured once scrolled to.
        compose.setContent {
            Column(Modifier.height(100.dp).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(300.dp))
                Box(Modifier.size(48.dp).clickable {}.semantics { contentDescription = "Retry" })
            }
        }

        compose.assertAccessible()
    }

    @Test
    fun lb406_aControlScrolledOutOfView_stillNeedsALabel() {
        // TalkBack scrolls to it, so it is read like any other.
        compose.setContent {
            Column(Modifier.height(100.dp).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(300.dp))
                Box(Modifier.size(48.dp).clickable {})
            }
        }

        val failure = assertThrows(AssertionError::class.java) { compose.assertAccessible() }

        assertTrue(failure.message, "no label" in failure.message.orEmpty())
    }

    @Test
    fun lb406_aControlPartlyScrolledOutOfView_isNotTooSmall() {
        // A list clips a row that is half in view: the row is whole once scrolled to.
        compose.setContent {
            Column(Modifier.height(72.dp).verticalScroll(rememberScrollState())) {
                Box(Modifier.size(48.dp).clickable {}.semantics { contentDescription = "Retry" })
                Box(Modifier.size(48.dp).clickable {}.semantics { contentDescription = "Clear search" })
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
    fun lb406_aNodeWithOnlyCustomActions_needsALabel() {
        // TalkBack offers its actions, "Move up" for one, and must say what they act on.
        compose.setContent {
            Box(
                Modifier.size(48.dp).semantics {
                    customActions = listOf(CustomAccessibilityAction("Move up") { true })
                },
            )
        }

        val failure = assertThrows(AssertionError::class.java) { compose.assertAccessible() }

        assertTrue(failure.message, "no label" in failure.message.orEmpty())
    }

    @Test
    fun lb406_aTextFieldNamedOnlyByWhatIsTyped_needsALabel() {
        compose.setContent { BasicTextField(value = "varea", onValueChange = {}, modifier = Modifier.width(200.dp)) }

        val failure = assertThrows(AssertionError::class.java) { compose.assertAccessible() }

        assertTrue(failure.message, "no label" in failure.message.orEmpty())
    }

    @Test
    fun lb406_aControlInAHiddenContainerThatDoesNotMerge_isChecked() {
        // Hiding a container that does not merge its children hides only the container from TalkBack.
        compose.setContent {
            Box(Modifier.semantics { hideFromAccessibility() }) {
                Box(Modifier.size(48.dp).clickable {})
            }
        }

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
    fun lb406_aSmallControlAtTheWindowsEdge_fails() {
        // Compose widens a click's touch area to 48 dp, but TalkBack gets it cut at the window's edge.
        compose.setContent { Box(Modifier.size(24.dp).clickable {}.semantics { contentDescription = "Clear search" }) }

        val failure = assertThrows(AssertionError::class.java) { compose.assertAccessible() }

        assertTrue(failure.message, "Clear search" in failure.message.orEmpty())
        assertTrue(failure.message, "48 dp" in failure.message.orEmpty())
    }

    @Test
    fun lb406_smallControlsSideBySide_fail() {
        // Their widened touch areas overlap, and TalkBack gets what the later one leaves of the earlier one.
        compose.setContent {
            Row(Modifier.padding(24.dp)) {
                Box(Modifier.size(24.dp).clickable {}.semantics { contentDescription = "Move up" })
                Box(Modifier.size(24.dp).clickable {}.semantics { contentDescription = "Move down" })
            }
        }

        val failure = assertThrows(AssertionError::class.java) { compose.assertAccessible() }

        assertTrue(failure.message, "Move up" in failure.message.orEmpty())
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
    fun lb406_textCutByItsContainer_fails() {
        // The text fits its own constraints, but a fixed-height parent clips it.
        compose.setContent {
            Box(Modifier.size(width = 200.dp, height = 8.dp).clipToBounds()) {
                Text("Hospital San Pedro", Modifier.wrapContentHeight(Alignment.Top, unbounded = true))
            }
        }

        val failure = assertThrows(AssertionError::class.java) { compose.assertAccessible() }

        assertTrue(failure.message, "Hospital San Pedro" in failure.message.orEmpty())
    }

    @Test
    fun lb406_textPartlyScrolledOutOfView_isNotCut() {
        // A list clips a text that is half in view: the text is whole once scrolled to.
        compose.setContent {
            Column(Modifier.height(30.dp).verticalScroll(rememberScrollState())) {
                Text("Hospital San Pedro")
                Text("El Arco")
            }
        }

        compose.assertAccessible()
    }

    @Test
    fun lb406_textHiddenFromScreenReaders_isStillCheckedForCuts() {
        // Cut text fails whoever reads the screen, whether or not TalkBack reads it.
        compose.setContent {
            Text(
                "Hospital San Pedro",
                Modifier.width(40.dp).semantics { hideFromAccessibility() },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val failure = assertThrows(AssertionError::class.java) { compose.assertAccessible() }

        assertTrue(failure.message, "Hospital San Pedro" in failure.message.orEmpty())
    }

    @Test
    fun lb406_textUnderClearAndSetSemantics_isStillChecked() {
        // A line badge speaks its own label, and its code must still show whole.
        compose.setContent {
            Box(Modifier.clearAndSetSemantics { contentDescription = "Line B1" }) {
                Text("B1", Modifier.size(8.dp), softWrap = false, overflow = TextOverflow.Clip)
            }
        }

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
