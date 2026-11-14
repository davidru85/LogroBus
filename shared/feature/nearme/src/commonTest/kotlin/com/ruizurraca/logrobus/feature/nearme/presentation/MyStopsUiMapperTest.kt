package com.ruizurraca.logrobus.feature.nearme.presentation

import com.ruizurraca.logrobus.core.presentation.EtaDisplay
import com.ruizurraca.logrobus.core.presentation.ServedArrivalUi
import com.ruizurraca.logrobus.favourites.domain.SavedStop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * LB-208 and LB-204 (FR-HOME-02, FR-STOP-08): what "My stops" shows. The alias wins over the catalogue's name, a stop
 * the catalogue no longer holds keeps a row so it can be removed, and an empty section says what to do.
 */
class MyStopsUiMapperTest {
    @Test
    fun lb204_aRowShowsOnlyTheNextThreeArrivals() {
        // FR-HOME-02: a saved stop with five calls shows its next three. The order and the past-drop are
        // `StopDetailUiMapper.rows`' job, which hands these already chronological, so this takes the first three.
        val five = listOf(arrival("5", 1), arrival("3", 2), arrival("1", 5), arrival("6", 7), arrival("4", 9))
        val row =
            SavedStopsUiMapper
                .myStops(
                    saved = listOf(saved(38)),
                    names = mapOf(38 to "Campillo"),
                    arrivals = mapOf(38 to five),
                ).rows
                .single()

        assertEquals(listOf("5", "3", "1"), row.arrivals.map { it.lineCode })
        assertEquals(3, row.arrivals.size)
    }

    @Test
    fun lb204_aCancelledArrivalIsNotCountedAmongTheThree() {
        // The criterion reads "it is not counted among the 3 shown", so a cancelled call takes no slot: the three are
        // the next three that are still running.
        val row =
            SavedStopsUiMapper
                .myStops(
                    saved = listOf(saved(38)),
                    names = mapOf(38 to "Campillo"),
                    arrivals =
                        mapOf(
                            38 to
                                listOf(
                                    arrival("1", 1),
                                    arrival("3", 2, cancelled = true),
                                    arrival("4", 3),
                                    arrival("5", 4),
                                ),
                        ),
                ).rows
                .single()

        assertEquals(listOf("1", "4", "5"), row.arrivals.map { it.lineCode })
    }

    @Test
    fun lb204_aStopWhoseRequestFailedKeepsItsPreviousArrivals() {
        // FR-ERR-03: the row keeps what it had, so a failure does not blank the section.
        val row =
            SavedStopsUiMapper
                .myStops(
                    saved = listOf(saved(38)),
                    names = mapOf(38 to "Campillo"),
                    arrivals = mapOf(38 to listOf(arrival("1", 4))),
                    failures = setOf(38),
                ).rows
                .single()

        assertEquals(listOf("1"), row.arrivals.map { it.lineCode })
        assertTrue(row.hasFailed, "the row says its list could not be refreshed")
    }

    @Test
    fun lb204_aStopWithNoArrivalsYetShowsNoRows() {
        val row =
            SavedStopsUiMapper.myStops(saved = listOf(saved(38)), names = mapOf(38 to "Campillo")).rows.single()

        assertTrue(row.arrivals.isEmpty())
        assertFalse(row.hasFailed)
    }

    private fun saved(stopId: Int) = SavedStop(stopId = stopId, alias = null, position = 0)

    private fun arrival(
        lineCode: String,
        minutes: Int,
        cancelled: Boolean = false,
    ) = ServedArrivalUi(
        lineCode = lineCode,
        color = 0xFF0FDCFCL,
        onColor = 0xFF000000L,
        headsign = "El Arco",
        eta = EtaDisplay.Minutes(minutes),
        isCancelled = cancelled,
        isLowConfidence = false,
    )

    @Test
    fun lb208_noSavedStops_saysHowToSaveOne() {
        val ui = SavedStopsUiMapper.myStops(saved = emptyList(), names = emptyMap())

        assertTrue(ui.isEmpty)
        assertEquals("Save a stop to see its next buses here", MyStopsUi.EMPTY_HINT)
    }

    @Test
    fun lb208_anAliasWinsOverTheCatalogueName() {
        val ui =
            SavedStopsUiMapper.myStops(
                saved = listOf(SavedStop(stopId = 38, alias = "Casa", position = 0)),
                names = mapOf(38 to "Gonzalo de Berceo"),
            )

        assertEquals(listOf("Casa"), ui.rows.map { it.label })
    }

    @Test
    fun lb208_aStopWithoutAnAliasShowsItsCatalogueName() {
        val ui =
            SavedStopsUiMapper.myStops(
                saved = listOf(SavedStop(stopId = 38, alias = null, position = 0)),
                names = mapOf(38 to "Gonzalo de Berceo"),
            )

        assertEquals(listOf("Gonzalo de Berceo"), ui.rows.map { it.label })
    }

    @Test
    fun lb208_aStopTheCatalogueNoLongerHolds_keepsARowItCanBeRemovedFrom() {
        // An alias still shows: the user named that stop, and the alias is all that is left of it.
        val ui = SavedStopsUiMapper.myStops(saved = listOf(SavedStop(99, "Casa", 0)), names = emptyMap())
        assertEquals(listOf("Casa"), ui.rows.map { it.label })

        // Without one there is nothing to show but the fact that the catalogue lost it, never an empty row.
        val unnamed = SavedStopsUiMapper.myStops(saved = listOf(SavedStop(99, null, 0)), names = emptyMap())
        assertEquals(listOf(SavedStopsUiMapper.UNKNOWN_STOP), unnamed.rows.map { it.label })
        assertEquals(listOf(99), unnamed.rows.map { it.stopId })
    }

    @Test
    fun lb208_theRowsKeepTheUsersOrder() {
        val ui =
            SavedStopsUiMapper.myStops(
                saved = listOf(SavedStop(92, "Cole", 0), SavedStop(38, "Casa", 1), SavedStop(61, null, 2)),
                names = mapOf(61 to "Varea"),
            )

        assertEquals(listOf(92, 38, 61), ui.rows.map { it.stopId })
        assertEquals(listOf(0, 1, 2), ui.rows.map { it.position })
    }

    @Test
    fun lb208_theLimitMessageIsCarried_untilItIsShown() {
        assertTrue(SavedStopsUiMapper.myStops(emptyList(), emptyMap(), limitReached = true).limitReached)
        assertFalse(SavedStopsUiMapper.myStops(emptyList(), emptyMap()).limitReached)
    }

    @Test
    fun lb208_theLimitIsTheFavouritesOne() {
        assertEquals(20, SavedStopsUiMapper.limit)
    }
}
