package com.ruizurraca.logrobus.feature.settings.presentation

import com.ruizurraca.logrobus.core.presentation.MadridDateTime
import com.ruizurraca.logrobus.feature.settings.domain.DatasetSync
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** FR-SET-03 and LB-404: each dataset's last successful sync, on the Madrid clock (AGENTS.md §5.5). */
class AboutDataUiMapperTest {
    @Test
    fun lb404_eachLastSyncShowsOnTheMadridClock_orAsNever() {
        val content =
            AboutDataUiMapper.content(
                listOf(
                    DatasetSync(StaticDataset.LINES, Instant.parse("2026-10-28T09:30:00Z")),
                    DatasetSync(StaticDataset.STOPS, null),
                ),
            )

        assertEquals(
            AboutDataUiState.Content(
                listOf(
                    LastSyncUi(StaticDataset.LINES, MadridDateTime(2026, 10, 28, 10, 30)),
                    LastSyncUi(StaticDataset.STOPS, null),
                ),
            ),
            content,
        )
    }
}
