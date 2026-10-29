package com.ruizurraca.logrobus.android.app

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * LB-012: each tab keeps its own back stack, and re-selecting a tab pops it to its root (FR-NAV-03). Each tab root opens
 * More, with Settings and About data (FR-NAV-04). "Two screens deep in Lines" is Lines, More, About data.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en")
class TabNavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun lb012_eachTabRootOpensMore_withSettingsAndAboutData() {
        listOf("Lines", "Near me", "Directions").forEach { label ->
            tab(label).performClick()
            moreAction().performClick()

            compose.onNodeWithText("Settings").assertIsDisplayed()
            compose.onNodeWithText("About data").assertIsDisplayed()
        }
    }

    @Test
    fun lb012_switchingTabsKeepsEachTabsScreen() {
        openAboutDataInLines()

        tab("Near me").performClick()
        moreAction().assertIsDisplayed()
        tab("Lines").performClick()

        aboutData().assertIsDisplayed()
    }

    @Test
    fun lb012_reselectingATabPopsItToItsRoot() {
        openAboutDataInLines()

        tab("Lines").performClick()

        moreAction().assertIsDisplayed()
        aboutData().assertDoesNotExist()
    }

    @Test
    fun lb012_backWalksBackThroughTheTabsStack() {
        openAboutDataInLines()

        back()
        compose.onNodeWithText("Settings").assertIsDisplayed()
        back()
        moreAction().assertIsDisplayed()
    }

    @Test
    fun lb012_backAtTheRootOfAnotherTabReturnsToLines() {
        tab("Directions").performClick()

        back()

        tab("Lines").assertIsSelected()
        assertTrue("the app is still open", !compose.activity.isFinishing)
    }

    @Test
    fun lb012_backAtTheRootOfLinesLeavesTheApp() {
        back()

        assertTrue("the activity finishes", compose.activity.isFinishing)
    }

    @Test
    fun lb012_theBackStacksSurviveRecreation() {
        openAboutDataInLines()
        tab("Near me").performClick()

        compose.activityRule.scenario.recreate()

        tab("Near me").assertIsSelected()
        tab("Lines").performClick()
        aboutData().assertIsDisplayed()
    }

    private fun openAboutDataInLines() {
        moreAction().performClick()
        compose.onNodeWithText("About data").performClick()
        aboutData().assertIsDisplayed()
    }

    /** About data's own content, which neither More nor a tab root shows. */
    private fun aboutData(): SemanticsNodeInteraction = compose.onNodeWithText("Last sync")

    private fun moreAction(): SemanticsNodeInteraction = compose.onNodeWithContentDescription("More")

    private fun tab(label: String): SemanticsNodeInteraction = compose.onNode(hasText(label) and isTab)

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
}
