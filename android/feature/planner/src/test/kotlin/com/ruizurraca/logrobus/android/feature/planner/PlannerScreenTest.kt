package com.ruizurraca.logrobus.android.feature.planner

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerEvent
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import com.ruizurraca.logrobus.android.core.designsystem.R as DS

/** FR-PLAN-11 on Android: How to Get There's two fields, under its title and the More action. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlannerScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun lb301_withTheLocationPermission_theOriginIsMyLocation_andTheDestinationIsEmpty() {
        show(PlannerPreviewData.myLocation)

        assertField(R.string.plan_origin_spoken, R.string.plan_my_location)
        assertField(R.string.plan_destination_spoken, R.string.plan_destination_hint)
    }

    @Test
    fun lb301_withoutIt_bothFieldsAreEmpty_andShowTheirHints() {
        show(PlannerPreviewData.empty)

        assertField(R.string.plan_origin_spoken, R.string.plan_origin_hint)
        assertField(R.string.plan_destination_spoken, R.string.plan_destination_hint)
    }

    @Test
    fun lb301_theFieldsOpenNothingYet() {
        // Place search comes with LB-301 part 2 (owner decision, 2026-11-06): until then a field is no button.
        show(PlannerPreviewData.myLocation)

        for (field in listOf(R.string.plan_origin_spoken, R.string.plan_destination_spoken)) {
            compose
                .onNodeWithContentDescription(context.getString(field, ""), substring = true)
                .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        }
    }

    @Test
    fun lb304_theSwapButton_isSpokenAsSwapOriginAndDestination_andOneTapSendsSwap() {
        val events = mutableListOf<PlannerEvent>()
        compose.setContent { PlannerScreen(PlannerPreviewData.myLocation, onEvent = { events += it }, onOpenMore = {}) }

        compose
            .onNodeWithContentDescription(context.getString(R.string.plan_swap))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()

        assertEquals(listOf<PlannerEvent>(PlannerEvent.Swap), events)
    }

    @Test
    fun lb304_afterASwap_theDestinationShowsMyLocation_andTheOriginItsHint() {
        show(PlannerPreviewData.swapped)

        assertField(R.string.plan_origin_spoken, R.string.plan_origin_hint)
        assertField(R.string.plan_destination_spoken, R.string.plan_my_location)
    }

    @Test
    fun lb304_talkBackReadsBothFields_beforeTheSwapButton() {
        show(PlannerPreviewData.myLocation)
        val origin = spoken(R.string.plan_origin_spoken, R.string.plan_my_location)
        val destination = spoken(R.string.plan_destination_spoken, R.string.plan_destination_hint)
        val swap = hasContentDescription(context.getString(R.string.plan_swap))

        // A traversal group is read whole before what follows it: the card holds both fields, and not the button,
        // which is centred beside them and would otherwise come between them.
        compose
            .onNode(
                SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true) and
                    hasAnyDescendant(origin) and
                    hasAnyDescendant(destination) and
                    !hasAnyDescendant(swap),
            ).assertExists()
    }

    @Test
    fun lb301_theTitleIsHowToGetThere_andTheMoreActionOpensMore() {
        var opened = 0
        compose.setContent { PlannerScreen(PlannerPreviewData.empty, onEvent = {}, onOpenMore = { opened++ }) }

        compose
            .onNodeWithText(context.getString(R.string.plan_title))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        compose.onNodeWithContentDescription(context.getString(DS.string.action_more)).performClick()

        assertEquals(1, opened)
    }

    private fun spoken(
        name: Int,
        shown: Int,
    ) = hasContentDescription(context.getString(name, context.getString(shown)))

    private fun show(state: PlannerUiState) {
        compose.setContent { PlannerScreen(state, onEvent = {}, onOpenMore = {}) }
    }

    /** The field is spoken as its name and what it shows, which shows on screen too. */
    private fun assertField(
        spoken: Int,
        shown: Int,
    ) {
        val text = context.getString(shown)
        compose.onNodeWithContentDescription(context.getString(spoken, text)).assertExists()
        compose.onNodeWithText(text, useUnmergedTree = true).assertExists()
    }
}
