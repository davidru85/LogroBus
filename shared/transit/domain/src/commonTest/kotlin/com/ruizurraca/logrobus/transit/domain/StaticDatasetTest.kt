package com.ruizurraca.logrobus.transit.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/** Spec §8.5 and ADR-0008: the MVP caches lines and stops; the other datasets join with v1.0. */
class StaticDatasetTest {
    @Test
    fun lb404_theMvpSyncsLinesAndStops() {
        assertEquals(listOf(StaticDataset.LINES, StaticDataset.STOPS), StaticDataset.entries)
    }
}
