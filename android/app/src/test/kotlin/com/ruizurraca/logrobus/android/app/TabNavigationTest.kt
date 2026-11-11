package com.ruizurraca.logrobus.android.app

import android.Manifest
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * LB-012: each tab keeps its own back stack, and re-selecting a tab pops it to its root (FR-NAV-03). Each tab root
 * opens More, with Settings and About data (FR-NAV-04). "Two screens deep in Lines" is Lines, More, About data.
 * LB-103: favourite lines, end to end through the shell, from the Lines rows and a line's detail. LB-301: How to Get
 * There's fields, with and without the location permission.
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

    @Test
    fun lb012_everyScreenTitleIsAHeading() {
        // TalkBack's heading navigation reaches each screen's title, as VoiceOver does on iOS.
        heading("Lines").assertIsDisplayed()
        moreAction().performClick()
        heading("More").assertIsDisplayed()
        compose.onNodeWithText("Settings").performClick()
        heading("Settings").assertIsDisplayed()
        back()
        compose.onNodeWithText("About data").performClick()
        heading("About data").assertIsDisplayed()
    }

    @Test
    fun lb012_theHowToGetThereRootHasItsScreenTitle() {
        // design_guidelines.md §2.1: the tab says "Directions"; its screen says "How to get there" (plan.title).
        tab("Directions").performClick()

        heading("How to get there").assertIsDisplayed()
    }

    @Test
    fun lb201_nearMesRootShowsNearYou_withTheRowThatTurnsLocationOn() {
        // A fresh install: location is off, and nothing asks for it until the row is tapped (FR-LOC-01).
        tab("Near me").performClick()

        heading("Near me").assertIsDisplayed()
        heading("Near you").assertIsDisplayed()
        compose.onNodeWithText("Turn on location to see nearby stops").assertIsDisplayed()
    }

    @Test
    fun lb301_withoutLocation_howToGetTheresFieldsAreBothEmpty() {
        // A fresh install: no location permission, so no origin (FR-PLAN-11).
        tab("Directions").performClick()

        compose.onNodeWithContentDescription("Origin, Where from?").assertIsDisplayed()
        compose.onNodeWithContentDescription("Destination, Where to?").assertIsDisplayed()
    }

    @Test
    fun lb301_withLocation_howToGetThereStartsFromMyLocation() {
        // Approximate location is enough (FR-LOC-01), granted in Near Me or the system's settings.
        shadowOf(compose.activity.application).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)

        tab("Directions").performClick()

        compose.onNodeWithContentDescription("Origin, My location").assertIsDisplayed()
        compose.onNodeWithContentDescription("Destination, Where to?").assertIsDisplayed()
    }

    @Test
    fun lb012_aTabSwitchSettlesOnTheMotionSchemesEffectsSpring() {
        // design_guidelines.md §4: screens change on the expressive motion scheme's effects spring, which settles well
        // before the library's 700 ms fade; until then both screens are in the semantics tree.
        compose.mainClock.autoAdvance = false
        tab("Near me").performClick()

        compose.mainClock.advanceTimeBy(SETTLED_MILLIS)

        heading("Lines").assertDoesNotExist()
        heading("Near me").assertIsDisplayed()
    }

    @Test
    fun lb012_theUpButtonGoesBackToThePreviousScreen() {
        openAboutDataInLines()

        compose.onNodeWithContentDescription("Back").performClick()

        compose.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun lb101_reselectingLinesAtItsRootScrollsTheListToItsTop() {
        // FR-NAV-03 end to end through the shell, moved from LB-012: the debug build's sample lines fill more than a
        // screen.
        compose.onNode(isLinesList).performScrollToNode(hasContentDescription("Line B3"))
        compose.onNodeWithContentDescription("Line 1").assertIsNotDisplayed()

        tab("Lines").performClick()

        compose.onNodeWithContentDescription("Line 1").assertIsDisplayed()
    }

    @Test
    fun lb103_aLineAddedToFavouritesShowsAtTheEndOfTheCarousel_andItsToggleSaysSo() {
        // FR-LINE-12, FR-LINE-15, through the debug build's graph and the device's database.
        rowToggle("Add line 10 to favourites").performClick()
        awaitNode(hasContentDescription("Remove line 10 from favourites"))
        rowToggle("Add line 3 to favourites").performClick()

        awaitNode(hasContentDescription("Remove line 3 from favourites"))
        awaitCarousel(listOf("Line 10", "Line 3"))
    }

    @Test
    fun lb103_aFavouriteCardOpensItsLine_andBackReturnsToLines() {
        rowToggle("Add line 10 to favourites").performClick()
        awaitNode(hasContentDescription("Remove line 10 from favourites"))

        card("Line 10").performClick()
        awaitNode(hasText("Line 10") and isHeading()).assertIsDisplayed()
        back()

        awaitNode(hasText("Lines") and isHeading()).assertIsDisplayed()
    }

    @Test
    fun lb103_aLineAddedFromItsDetailIsInTheCarousel_backOnLines() {
        // FR-LINE-09: the row opens the line's detail, whose toolbar has the toggle once the line has loaded.
        compose.onNode(isLinesList).performScrollToNode(hasText("Campillo - El Cubo"))
        compose.onNode(hasText("Campillo - El Cubo") and hasClickAction()).performClick()
        awaitNode(hasContentDescription("Add line 3 to favourites")).performClick()
        awaitNode(hasContentDescription("Remove line 3 from favourites"))
        back()

        awaitCarousel(listOf("Line 3"))
    }

    @Test
    fun lb205_aStopRowOpensItsStopDetail_andBackReturnsToTheLine() {
        // FR-NAV-07 end to end through the shell and the preview's scripted graph: line 3's first outbound stop.
        compose.onNode(isLinesList).performScrollToNode(hasText("Campillo - El Cubo"))
        compose.onNode(hasText("Campillo - El Cubo") and hasClickAction()).performClick()
        awaitNode(hasText("Line 3") and isHeading()).assertIsDisplayed()

        val stop = awaitNode(hasContentDescription("Stop 1, Campillo", substring = true) and hasClickAction())
        stop.performClick()

        // The pushed screen, not a dialog: its own title is a heading, and the line's is gone.
        awaitNode(hasText("Campillo", substring = true) and isHeading())
        assertTrue(
            "the line's detail is under it",
            compose.onAllNodes(hasText("Line 3") and isHeading()).fetchSemanticsNodes().isEmpty(),
        )

        compose.onNodeWithContentDescription("Back").performClick()

        awaitNode(hasText("Line 3") and isHeading()).assertIsDisplayed()
    }

    /**
     * A row's toggle, scrolled into view: a lazy list composes only the rows it shows. The list scrolls down from its
     * top, so the row ends up above the tab bar, never under the top app bar, which the list runs behind.
     */
    private fun rowToggle(label: String): SemanticsNodeInteraction {
        compose.onNode(isLinesList).performScrollToIndex(0).performScrollToNode(hasContentDescription(label))
        return compose.onNodeWithContentDescription(label)
    }

    /**
     * The node [matcher] finds, once it exists. The lines and the favourites are read and written on the I/O threads,
     * which the test's idle sync does not wait for: on a slow CI runner the screen shows the change a moment later.
     */
    private fun awaitNode(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        compose.waitUntil(AWAIT_MILLIS) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
        return compose.onNode(matcher)
    }

    /** A favourite line's card: it alone says where the line heads ("Towards …"). */
    private fun card(badge: String): SemanticsNodeInteraction {
        compose.onNode(isLinesList).performScrollToIndex(0)
        return awaitNode(hasContentDescription(badge) and hasContentDescription("Towards", substring = true))
    }

    /** Waits until the carousel shows [badges], then checks them, in their order. */
    private fun awaitCarousel(badges: List<String>) {
        compose.onNode(isLinesList).performScrollToIndex(0)
        runCatching { compose.waitUntil(AWAIT_MILLIS) { carouselCards() == badges } }
        assertEquals(badges, carouselCards())
    }

    /**
     * The carousel's cards, by their badge, from left to right: the order the user sees and TalkBack reads, which sorts
     * siblings by position. The carousel adds its items to the semantics tree in another order.
     */
    private fun carouselCards(): List<String?> =
        compose
            .onAllNodes(hasContentDescription("Towards", substring = true) and hasClickAction())
            .fetchSemanticsNodes()
            .sortedBy { it.boundsInRoot.left }
            .map { it.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull() }

    private fun openAboutDataInLines() {
        moreAction().performClick()
        compose.onNodeWithText("About data").performClick()
        aboutData().assertIsDisplayed()
    }

    /** About data's own content, which neither More nor a tab root shows. */
    private fun aboutData(): SemanticsNodeInteraction = compose.onNodeWithText("Last sync")

    private fun heading(title: String): SemanticsNodeInteraction = compose.onNode(hasText(title) and isHeading())

    private fun moreAction(): SemanticsNodeInteraction = compose.onNodeWithContentDescription("More")

    private fun tab(label: String): SemanticsNodeInteraction = compose.onNode(hasText(label) and isTab)

    /** System back, once the last action has recomposed, as a user's back always comes after the screen updates. */
    private fun back() {
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    // The Lines list is the lazy list that scrolls vertically; the favourites carousel inside it and the search field
    // above its rows scroll horizontally.
    private val isLinesList =
        SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange) and hasScrollToIndexAction()

    private companion object {
        const val SETTLED_MILLIS = 400L

        // Far longer than a read or a write takes, even on a busy runner.
        const val AWAIT_MILLIS = 5_000L
    }
}
