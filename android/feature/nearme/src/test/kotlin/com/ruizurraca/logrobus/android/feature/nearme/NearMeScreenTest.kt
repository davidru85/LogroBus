package com.ruizurraca.logrobus.android.feature.nearme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
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
import com.ruizurraca.logrobus.core.presentation.EtaDisplay
import com.ruizurraca.logrobus.core.presentation.ServedArrivalUi
import com.ruizurraca.logrobus.feature.nearme.domain.StopSearchResultUi
import com.ruizurraca.logrobus.feature.nearme.presentation.MyStopsUi
import com.ruizurraca.logrobus.feature.nearme.presentation.NearMeUiState
import com.ruizurraca.logrobus.feature.nearme.presentation.NearYouUi
import com.ruizurraca.logrobus.feature.nearme.presentation.NearbyStopRowUi
import com.ruizurraca.logrobus.feature.nearme.presentation.SavedStopRowUi
import com.ruizurraca.logrobus.feature.nearme.presentation.StopSearchUi
import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.ServedLine
import com.ruizurraca.logrobus.transit.domain.Stop
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
    fun lb204_aSavedStopShowsItsNextCalls_andTheyAreSpokenWithIt() {
        // FR-HOME-02, NFR-A11Y-02: the row carries the stop's calls, and TalkBack reads the stop and them as one.
        val state =
            NearMeUiState(
                nearYou = NearYouUi.LocationOn(),
                myStops =
                    MyStopsUi(
                        rows =
                            listOf(
                                SavedStopRowUi(
                                    stopId = 38,
                                    label = "Casa",
                                    position = 0,
                                    arrivals =
                                        listOf(
                                            ServedArrivalUi(
                                                "1",
                                                0xFFE30613L,
                                                0xFFFFFFFFL,
                                                "El Arco",
                                                EtaDisplay.Minutes(4),
                                                false,
                                                false,
                                            ),
                                            ServedArrivalUi(
                                                "33",
                                                0xFF1E7A3FL,
                                                0xFFFFFFFFL,
                                                "Varea",
                                                EtaDisplay.Minutes(9),
                                                false,
                                                false,
                                            ),
                                        ),
                                ),
                            ),
                    ),
            )
        compose.setContent {
            NearMeScreen(state, onTurnOnLocation = {}, onOpenMore = {}, onOpenStop = {})
        }

        // The row is one utterance that starts with the stop and includes both calls, in the design system's words.
        compose.onNodeWithContentDescription("Casa", substring = true).assertExists()
        compose.onNodeWithContentDescription("4 min", substring = true).assertExists()
        compose.onNodeWithContentDescription("9 min", substring = true).assertExists()
    }

    @Test
    fun lb209_aSearchShowsItsStops_withTheirLineAndHeadsign() {
        // FR-NEAR-05, DD-16: the result carries the badge and the headsign that tell it apart, read as one utterance.
        val state =
            NearMeUiState(
                nearYou = NearYouUi.LocationOn(),
                search =
                    StopSearchUi(
                        query = "Ayuntamiento",
                        results =
                            listOf(
                                StopSearchResultUi(
                                    Stop(
                                        id = 38,
                                        name = "Ayuntamiento",
                                        lat = 42.46,
                                        lng = -2.44,
                                        isOperational = true,
                                        isProvisional = false,
                                        servedLines =
                                            listOf(
                                                ServedLine(
                                                    1,
                                                    "1",
                                                    "El Arco",
                                                    Direction.OUTBOUND,
                                                    0xFFE30613L,
                                                    0xFFFFFFFFL,
                                                ),
                                            ),
                                    ),
                                ),
                            ),
                    ),
            )
        compose.setContent {
            NearMeScreen(state, onTurnOnLocation = {}, onOpenMore = {}, onOpenStop = {}, onQueryChanged = {})
        }

        compose.onNodeWithContentDescription("Ayuntamiento, 1, Towards El Arco").assertExists()
    }

    @Test
    fun lb209_aQueryThatFindsNothingSaysSo() {
        compose.setContent {
            NearMeScreen(
                NearMeUiState(nearYou = NearYouUi.LocationOn(), search = StopSearchUi(query = "tranvía")),
                onTurnOnLocation = {},
                onOpenMore = {},
                onQueryChanged = {},
            )
        }

        compose.onNodeWithText("No stops match that search").assertExists()
    }

    @Test
    fun lb208_withNoSavedStops_theSectionSaysHowToSaveOne() {
        compose.setContent {
            NearMeScreen(NearMeUiState(), onTurnOnLocation = {}, onOpenMore = {})
        }

        compose.onNodeWithText(context.getString(R.string.nearme_my_stops_empty)).assertExists()
    }

    @Test
    fun lb208_editOpensTheSheet_whichMovesARow() {
        // FR-FAV-02: Edit is offered while there is something to edit, and moving a row sends the new order.
        val rows =
            listOf(
                SavedStopRowUi(stopId = 38, label = "Casa", position = 0),
                SavedStopRowUi(stopId = 61, label = "Trabajo", position = 1),
            )
        var editTapped = 0
        compose.setContent {
            NearMeScreen(
                NearMeUiState(myStops = MyStopsUi(rows = rows)),
                onTurnOnLocation = {},
                onOpenMore = {},
                onEditMyStops = { editTapped++ },
            )
        }

        compose.onNodeWithText(context.getString(R.string.nearme_my_stops_edit)).performClick()

        assertEquals(1, editTapped)
    }

    @Test
    fun lb208_withNothingSaved_thereIsNothingToEdit() {
        compose.setContent {
            NearMeScreen(NearMeUiState(), onTurnOnLocation = {}, onOpenMore = {})
        }

        compose.onNodeWithText(context.getString(R.string.nearme_my_stops_edit)).assertDoesNotExist()
    }

    @Test
    fun lb208_theEditSheetMovesARow_andSaysItsPlace() {
        // FR-FAV-02: the row's place is its state, so TalkBack reads it; moving it sends the reordered ids.
        val rows =
            listOf(
                SavedStopRowUi(stopId = 38, label = "Casa", position = 0),
                SavedStopRowUi(stopId = 61, label = "Trabajo", position = 1),
            )
        val orders = mutableListOf<List<Int>>()
        compose.setContent {
            MyStopsEditSheet(
                rows = rows,
                onReorder = { orders += it },
                onRename = { _, _ -> },
                onDelete = {},
                onDismiss = {},
            )
        }

        val last = compose.onNode(hasText("Trabajo"))
        val place = context.getString(R.string.nearme_my_stops_position, 2, 2)
        assertEquals(place, last.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        // A last row has one move only, up.
        assertEquals(listOf(context.getString(R.string.nearme_my_stops_move_up)), actionsOf(last))

        perform(context.getString(R.string.nearme_my_stops_move_up), last)

        assertEquals(listOf(listOf(61, 38)), orders)
    }

    @Test
    fun lb208_theEditSheetDeletesASavedStop() {
        val rows = listOf(SavedStopRowUi(stopId = 38, label = "Casa", position = 0))
        val deleted = mutableListOf<Int>()
        compose.setContent {
            MyStopsEditSheet(
                rows = rows,
                onReorder = {},
                onRename = { _, _ -> },
                onDelete = { deleted += it },
                onDismiss = {},
            )
        }

        compose.onNodeWithContentDescription(context.getString(R.string.nearme_my_stops_delete)).performClick()

        assertEquals(listOf(38), deleted)
    }

    /** The labels of a row's custom accessibility actions, in the order TalkBack lists them. */
    private fun actionsOf(row: SemanticsNodeInteraction): List<String> =
        row
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.CustomActions)
            .orEmpty()
            .map { it.label }

    /** Runs the custom action labelled [label] on [row], as TalkBack does. */
    private fun perform(
        label: String,
        row: SemanticsNodeInteraction,
    ) {
        val action = row.fetchSemanticsNode().config[SemanticsActions.CustomActions].first { it.label == label }
        compose.runOnIdle { action.action() }
        compose.waitForIdle()
    }
}
