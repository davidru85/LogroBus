package com.ruizurraca.logrobus.android.feature.nearme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.feature.nearme.presentation.NearMeUiState
import com.ruizurraca.logrobus.feature.nearme.presentation.NearYouUi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import com.ruizurraca.logrobus.android.core.designsystem.R as DS

/** FR-NEAR-06 on Android: "Near you" without and with location, under Near Me's title and the More action. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NearMeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun lb201_withoutLocation_nearYouIsOneRow_thatTurnsLocationOn() {
        var taps = 0
        compose.setContent {
            NearMeScreen(NearMeUiState(NearYouUi.TurnOnLocation), onTurnOnLocation = { taps++ }, onOpenMore = {})
        }

        compose
            .onNodeWithText(context.getString(R.string.nearme_nearby_title))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        compose
            .onNodeWithText(context.getString(R.string.nearme_nearby_turn_on_location))
            .assertHasClickAction()
            .performClick()

        assertEquals(1, taps)
    }

    @Test
    fun lb201_withLocation_orUntilItIsKnown_thereIsNoRow() {
        val row = context.getString(R.string.nearme_nearby_turn_on_location)
        var state by mutableStateOf(NearMeUiState(NearYouUi.TurnOnLocation))
        compose.setContent { NearMeScreen(state, onTurnOnLocation = {}, onOpenMore = {}) }
        // The row shows first, so that its absence below is the state's doing.
        compose.onNodeWithText(row).assertExists()

        for (nearYou in listOf(NearYouUi.LocationOn, NearYouUi.Unknown)) {
            state = NearMeUiState(nearYou)

            compose.onNodeWithText(row).assertDoesNotExist()
            compose.onNodeWithText(context.getString(R.string.nearme_nearby_title)).assertDoesNotExist()
        }
    }

    @Test
    fun lb201_theMoreActionOpensMore() {
        var opened = 0
        compose.setContent {
            NearMeScreen(NearMeUiState(NearYouUi.TurnOnLocation), onTurnOnLocation = {}, onOpenMore = { opened++ })
        }

        compose.onNodeWithText(context.getString(R.string.nearme_title)).assertExists()
        compose.onNodeWithContentDescription(context.getString(DS.string.action_more)).performClick()

        assertEquals(1, opened)
    }
}
