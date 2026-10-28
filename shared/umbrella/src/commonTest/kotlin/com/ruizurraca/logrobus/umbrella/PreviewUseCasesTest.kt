package com.ruizurraca.logrobus.umbrella

import app.cash.turbine.test
import com.ruizurraca.logrobus.feature.settings.domain.DatasetSync
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** architecture.md §5.2: SharedDependencies.preview() scripts the use cases for SwiftUI previews and Swift tests. */
class PreviewUseCasesTest {
    private val sample = Instant.parse("2026-10-28T09:30:00Z")

    @Test
    fun lb010_thePreviewShowsEveryDatasetSynced() =
        runTest {
            assertEquals(
                listOf(DatasetSync(StaticDataset.LINES, sample), DatasetSync(StaticDataset.STOPS, sample)),
                SharedDependencies.preview().observeLastSyncs()().first(),
            )
        }

    @Test
    fun lb010_thePreviewCanScriptAnAppThatNeverSynced() =
        runTest {
            assertEquals(
                listOf(DatasetSync(StaticDataset.LINES, null), DatasetSync(StaticDataset.STOPS, null)),
                SharedDependencies
                    .preview()
                    .neverSynced()
                    .observeLastSyncs()()
                    .first(),
            )
        }

    @Test
    fun lb010_thePreviewCanScriptAFirstLoadThatNeverEnds() =
        runTest {
            SharedDependencies.preview().loading().observeLastSyncs()().test {
                expectNoEvents()
            }
        }

    @Test
    fun lb010_thePreviewCountsTheCollectionsThatAreRunning() =
        runTest {
            val preview = SharedDependencies.preview()
            assertEquals(0, preview.activeCollections)

            preview.observeLastSyncs()().test {
                awaitItem()
                assertEquals(1, preview.activeCollections)
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(0, preview.activeCollections)
        }
}
