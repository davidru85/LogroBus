package com.ruizurraca.logrobus.android.core.designsystem

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * LB-102 (FR-LINE-03): a search field that filters what is under it as the user types, in the search bar's shape. M3's
 * collapsed `SearchBar` only opens an expanded search view, and keeps the keyboard away until it does.
 */
@RunWith(AndroidJUnit4::class)
// Text measures its real size only with native graphics (testing_strategy.md §4.5).
@GraphicsMode(GraphicsMode.Mode.NATIVE)
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
    fun lb102_theFieldKeepsItsNameOnceItHasText() {
        // The placeholder goes with the first key; TalkBack still says it is the search field, as M3's does.
        compose.setContent { LogroBusTheme { SearchField(TextFieldState("varea"), placeholder = "Buscar línea") } }

        compose.onNode(hasSetTextAction()).assert(hasContentDescription("Buscar línea"))
    }

    @Test
    fun lb102_clearingTheTextLeavesTheFocusInTheField() {
        // The clear button disappears with the text: the focus must not go with it (design_guidelines.md §8).
        compose.setContent { LogroBusTheme { SearchField(TextFieldState(), placeholder = "Buscar línea") } }
        compose.onNode(hasSetTextAction()).performTextInput("varea")

        compose.onNodeWithContentDescription(clear).performClick()

        compose.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun lb102_theLoadingPlaceholderIsAsTallAsTheField_atEveryTextSize() {
        // Spec §7.3: the first load draws the final layout, so nothing moves when the lines arrive.
        var fontScale by mutableFloatStateOf(1f)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                LogroBusTheme {
                    Column {
                        SearchField(TextFieldState(), placeholder = "Buscar línea", Modifier.testTag(FIELD))
                        SearchFieldPlaceholder(Modifier.testTag(PLACEHOLDER))
                    }
                }
            }
        }

        listOf(1f, 2f).forEach { scale ->
            fontScale = scale
            val field =
                compose
                    .onNodeWithTag(FIELD)
                    .fetchSemanticsNode()
                    .size.height
            val placeholder =
                compose
                    .onNodeWithTag(PLACEHOLDER)
                    .fetchSemanticsNode()
                    .size.height
            assertEquals("at $scale", field.toFloat(), placeholder.toFloat(), 1f)
        }
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

    private companion object {
        const val FIELD = "field"
        const val PLACEHOLDER = "placeholder"
    }
}
