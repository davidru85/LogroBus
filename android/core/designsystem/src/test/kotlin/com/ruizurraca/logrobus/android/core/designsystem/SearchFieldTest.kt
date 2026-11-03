package com.ruizurraca.logrobus.android.core.designsystem

import android.content.Context
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * LB-102 (FR-LINE-03): a search field that filters what is under it as the user types, in the search bar's shape. M3's
 * collapsed `SearchBar` only opens an expanded search view, and keeps the keyboard away until it does.
 */
@RunWith(AndroidJUnit4::class)
class SearchFieldTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clear get() = context.getString(R.string.search_clear)

    @Test
    fun lb102_theFieldSaysWhatItSearches_andTakesText() {
        val state = TextFieldState()
        compose.setContent { LogroBusTheme { SearchField(state, placeholder = "Buscar línea") } }

        compose.onNode(hasSetTextAction() and hasText("Buscar línea")).performTextInput("varea")

        assertEquals("varea", state.text.toString())
    }

    @Test
    fun lb102_theKeyboardsActionIsSearch() {
        compose.setContent { LogroBusTheme { SearchField(TextFieldState(), placeholder = "Buscar línea") } }

        compose.onNode(hasSetTextAction() and hasImeAction(ImeAction.Search)).assertExists()
    }

    @Test
    fun lb102_aClearButtonShowsOnlyWithText_andEmptiesTheField() {
        val state = TextFieldState()
        compose.setContent { LogroBusTheme { SearchField(state, placeholder = "Buscar línea") } }
        compose.onAllNodesWithContentDescription(clear).assertCountEquals(0)

        compose.onNode(hasSetTextAction()).performTextInput("varea")
        compose.onNodeWithContentDescription(clear).performClick()

        assertEquals("", state.text.toString())
        compose.onAllNodesWithContentDescription(clear).assertCountEquals(0)
    }

    @Test
    fun lb102_theClearButtonsTouchTargetIsAtLeast48dp() {
        compose.setContent { LogroBusTheme { SearchField(TextFieldState("varea"), placeholder = "Buscar línea") } }

        val target = compose.onNodeWithContentDescription(clear).fetchSemanticsNode().touchBoundsInRoot
        with(compose.density) {
            assertTrue("${target.width.toDp()} wide", target.width.toDp() >= 48.dp)
            assertTrue("${target.height.toDp()} high", target.height.toDp() >= 48.dp)
        }
    }
}
