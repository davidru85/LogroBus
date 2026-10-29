package com.ruizurraca.logrobus.android.app

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** LB-012: the three tabs of FR-NAV-01, on Lines at a cold start (FR-NAV-02), as a bar or a rail (FR-NAV-05). */
@RunWith(AndroidJUnit4::class)
class AppShellTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    @Config(qualifiers = "en")
    fun lb012_aColdStartSelectsLines_withTheTabsInOrder() {
        assertTabsInOrder("Lines", "Near me", "Directions")
    }

    @Test
    @Config(qualifiers = "es")
    fun lb012_aColdStartSelectsLines_withTheTabsInOrder_inSpanish() {
        assertTabsInOrder("Líneas", "Cerca de mí", "Cómo llegar")
    }

    @Test
    @Config(qualifiers = "en")
    fun lb012_eachTabIsATabWithItsSelectedState() {
        // The automated half of the TalkBack criterion: a tab role, its label and its selected state.
        tab("Near me").performClick()

        tab("Lines").assertIsNotSelected()
        tab("Near me").assertIsSelected()
        tab("Directions").assertIsNotSelected()
    }

    @Test
    @Config(qualifiers = "en-w411dp-h891dp")
    fun lb012_aCompactWindowShowsANavigationBar() {
        val bounds = tabBounds("Lines", "Near me", "Directions")

        assertTrue("one row", bounds.all { it.top == bounds.first().top })
        assertTrue("left to right", bounds.zipWithNext().all { (a, b) -> a.right <= b.left })
        assertTrue("at the bottom", bounds.first().top > rootBounds().height / 2)
    }

    @Test
    @Config(qualifiers = "en-w700dp-h1000dp")
    fun lb012_aMediumWindowShowsANavigationRail() {
        val bounds = tabBounds("Lines", "Near me", "Directions")

        assertTrue("one column", bounds.all { it.left == bounds.first().left })
        assertTrue("top to bottom", bounds.zipWithNext().all { (a, b) -> a.bottom <= b.top })
        assertTrue("at the start", bounds.first().right < rootBounds().width / 2)
    }

    @Test
    @Config(qualifiers = "es-w320dp-h470dp", fontScale = 2f)
    fun lb012_atTwiceTheFontSizeEveryTabLabelFitsItsTab() {
        // NFR-A11Y-03 on a small phone: no label draws outside its own space, over the next tab.
        listOf("Líneas", "Cerca de mí", "Cómo llegar").forEach { label ->
            val layout = labelLayout(label)
            assertFalse("\"$label\" overflows its tab: ${layout.size}", layout.hasVisualOverflow)
        }
    }

    private fun assertTabsInOrder(vararg labels: String) {
        val bounds = tabBounds(*labels)
        assertEquals("the labels, in order", bounds.sortedBy { it.left + it.top }, bounds)
        tab(labels.first()).assertIsSelected()
        labels.drop(1).forEach { tab(it).assertIsNotSelected() }
    }

    private fun tabBounds(vararg labels: String): List<Rect> = labels.map { tab(it).fetchSemanticsNode().boundsInRoot }

    private fun tab(label: String): SemanticsNodeInteraction = compose.onNode(hasText(label) and isTab)

    private fun labelLayout(label: String): TextLayoutResult {
        val text = compose.onNode(hasText(label) and hasAnyAncestor(isTab), useUnmergedTree = true).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        text.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        return layouts.single()
    }

    private fun rootBounds(): Rect = compose.onRoot().fetchSemanticsNode().boundsInRoot

    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
}
