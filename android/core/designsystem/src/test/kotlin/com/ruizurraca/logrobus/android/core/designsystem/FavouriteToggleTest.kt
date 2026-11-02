package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * LB-103 (FR-LINE-15) and NFR-A11Y-04: the favourite toggle is a 48 dp button whose label says what a tap does. The
 * label carries the state, so TalkBack never hears "checked" beside "Remove line 3 from favourites".
 */
@RunWith(AndroidJUnit4::class)
class FavouriteToggleTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lb103_aTapAsksForTheOtherValue() {
        val asked = mutableListOf<Boolean>()
        compose.setContent {
            LogroBusTheme { FavouriteToggle(isFavourite = true, onToggle = { asked += it }, label = "Remove line 3") }
        }

        compose.onNodeWithContentDescription("Remove line 3").performClick()

        assertEquals(listOf(false), asked)
    }

    @Test
    fun lb103_itIsAButtonLabelledByItsAction_notACheckbox() {
        compose.setContent { Toggle(isFavourite = true, label = "Remove line 3") }

        compose
            .onNodeWithContentDescription("Remove line 3")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ToggleableState))
    }

    @Test
    fun lb103_itsTouchTargetIsAtLeast48dp() {
        compose.setContent { Toggle(isFavourite = false, label = "Add line 3") }

        val target = compose.onNodeWithContentDescription("Add line 3").fetchSemanticsNode().touchBoundsInRoot
        with(compose.density) {
            assertTrue("${target.width.toDp()} wide", target.width.toDp() >= 48.dp)
            assertTrue("${target.height.toDp()} high", target.height.toDp() >= 48.dp)
        }
    }

    @Composable
    private fun Toggle(
        isFavourite: Boolean,
        label: String,
    ) {
        LogroBusTheme { FavouriteToggle(isFavourite = isFavourite, onToggle = {}, label = label) }
    }
}
