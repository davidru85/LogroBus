package com.ruizurraca.logrobus.android.feature.nearme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.feature.nearme.presentation.MyStopsUi
import com.ruizurraca.logrobus.feature.nearme.presentation.NearMeUiState
import com.ruizurraca.logrobus.feature.nearme.presentation.NearYouUi
import com.ruizurraca.logrobus.feature.nearme.presentation.NearbyStopRowUi
import com.ruizurraca.logrobus.feature.nearme.presentation.SavedStopRowUi
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

        for (nearYou in listOf(NearYouUi.LocationOn(), NearYouUi.Unknown)) {
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

    @Test
    fun lb203_eachNearbyStopShowsItsNameAndHowFarItIs_andOpensTheStop() {
        // FR-HOME-03 and NFR-A11Y-04: the list is the map's equivalent, and opening a stop from it never needs the map.
        val opened = mutableListOf<Int>()
        val section =
            NearYouUi.LocationOn(
                stops =
                    listOf(
                        NearbyStopRowUi(
                            stopId = 38,
                            name = "Gonzalo de Berceo",
                            distanceMeters = 120,
                            walkingMinutes = 2,
                        ),
                        NearbyStopRowUi(stopId = 61, name = "Varea", distanceMeters = 400),
                    ),
                isRefined = true,
            )
        compose.setContent {
            NearMeScreen(NearMeUiState(section), onTurnOnLocation = {}, onOpenMore = {}, onOpenStop = { opened += it })
        }

        // The row clears its children to be one utterance, so its words are read from the unmerged tree.
        compose.onNodeWithText("Gonzalo de Berceo", useUnmergedTree = true).assertExists()
        compose
            .onNodeWithText(context.getString(R.string.nearme_nearby_walk_minutes, 2), useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithText("Varea", useUnmergedTree = true).assertExists()
        // A stop the walk has not reached yet says its straight-line distance rather than a walk it does not know.
        compose
            .onNodeWithText(context.getString(R.string.nearme_nearby_distance_meters, 400), useUnmergedTree = true)
            .assertExists()

        compose.onNodeWithText("Varea", useUnmergedTree = true).performClick()

        assertEquals(listOf(61), opened)
    }

    @Test
    fun lb203_eachNearbyStopIsOneUtterance() {
        // NFR-A11Y-02: TalkBack reads the stop and how far it is as one row, then what a double tap does.
        compose.setContent {
            NearMeScreen(
                NearMeUiState(
                    NearYouUi.LocationOn(
                        stops = listOf(NearbyStopRowUi(38, "Gonzalo de Berceo", 120, walkingMinutes = 2)),
                        isRefined = true,
                    ),
                ),
                onTurnOnLocation = {},
                onOpenMore = {},
            )
        }

        val spoken =
            context.getString(
                R.string.nearme_nearby_stop_spoken,
                "Gonzalo de Berceo",
                context.getString(R.string.nearme_nearby_walk_minutes, 2),
            )
        compose.onNodeWithContentDescription(spoken).assertExists()
    }

    @Test
    fun lb208_myStopsListsTheSavedOnes_underTheirAliases() {
        // FR-HOME-02: the section above "Near you", in the user's order, each row read as one utterance.
        val opened = mutableListOf<Int>()
        val state =
            NearMeUiState(
                nearYou = NearYouUi.LocationOn(),
                myStops =
                    MyStopsUi(
                        rows =
                            listOf(
                                SavedStopRowUi(stopId = 38, label = "Casa", position = 0),
                                SavedStopRowUi(stopId = 61, label = "Trabajo", position = 1),
                            ),
                    ),
            )
        compose.setContent {
            NearMeScreen(state, onTurnOnLocation = {}, onOpenMore = {}, onOpenStop = { opened += it })
        }

        compose.onNode(isHeading() and hasText(context.getString(R.string.nearme_my_stops_title))).assertExists()
        compose.onNodeWithContentDescription("Casa").performClick()
        compose.onNodeWithContentDescription("Trabajo").assertExists()

        assertEquals(listOf(38), opened)
    }

    @Test
    fun lb208_withNoSavedStops_theSectionSaysHowToSaveOne() {
        compose.setContent {
            NearMeScreen(NearMeUiState(), onTurnOnLocation = {}, onOpenMore = {})
        }

        compose.onNodeWithText(context.getString(R.string.nearme_my_stops_empty)).assertExists()
    }
}
