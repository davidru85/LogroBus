package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** LB-012, NFR-A11Y-02 and NFR-A11Y-04: the top app bar actions say what they do, and are 48 dp targets. */
@RunWith(AndroidJUnit4::class)
class TopAppBarActionsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    @Config(qualifiers = "es")
    fun lb012_theActionsSayWhatTheyDo() {
        compose.setContent { Actions() }

        compose.onNodeWithContentDescription("Más").assertExists()
        compose.onNodeWithContentDescription("Atrás").assertExists()
    }

    @Test
    @Config(qualifiers = "en")
    fun lb012_theActionsSayWhatTheyDo_inEnglish() {
        compose.setContent { Actions() }

        compose.onNodeWithContentDescription("More").assertExists()
        compose.onNodeWithContentDescription("Back").assertExists()
    }

    @Test
    @Config(qualifiers = "en")
    fun lb012_theActionsTouchTargetsAreAtLeast48dp() {
        compose.setContent { Actions() }

        // A Material 3 icon button is 40 dp; Compose expands its touch target to the 48 dp it reserves around it.
        listOf("More", "Back").forEach { label ->
            val target = compose.onNodeWithContentDescription(label).fetchSemanticsNode().touchBoundsInRoot
            with(compose.density) {
                assertTrue("$label is ${target.width.toDp()} wide", target.width.toDp() >= 48.dp)
                assertTrue("$label is ${target.height.toDp()} high", target.height.toDp() >= 48.dp)
            }
        }
    }

    @Composable
    private fun Actions() {
        LogroBusTheme {
            Row {
                UpButton(onClick = {})
                MoreActionButton(onClick = {})
            }
        }
    }
}
