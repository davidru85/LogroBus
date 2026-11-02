package com.ruizurraca.logrobus.umbrella

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.feature.lines.domain.LinesWithFavourites
import com.ruizurraca.logrobus.feature.settings.domain.DatasetSync
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
                AppResult.Success(
                    listOf(DatasetSync(StaticDataset.LINES, sample), DatasetSync(StaticDataset.STOPS, sample)),
                ),
                SharedDependencies.preview().observeLastSyncs()().first(),
            )
        }

    @Test
    fun lb010_thePreviewCanScriptAnAppThatNeverSynced() =
        runTest {
            assertEquals(
                AppResult.Success(
                    listOf(DatasetSync(StaticDataset.LINES, null), DatasetSync(StaticDataset.STOPS, null)),
                ),
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

    @Test
    fun lb010_thePreviewCanScriptAFailure_whichEndsTheFlowLikeTheRealOne() =
        runTest {
            SharedDependencies.preview().failing().observeLastSyncs()().test {
                assertEquals(AppResult.Failure(AppError.Data), awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun lb101_thePreviewListsTheSampleLines() =
        runTest {
            assertEquals(AppResult.Success(SampleLines.all), SharedDependencies.preview().lines().first())
        }

    @Test
    fun lb101_thePreviewCanScriptAFirstLaunchWithNoNetwork() =
        runTest {
            val preview = SharedDependencies.preview().offline()

            assertEquals(AppResult.Failure(AppError.Network), preview.lines().first())
            assertEquals(
                AppResult.Success(
                    listOf(DatasetSync(StaticDataset.LINES, null), DatasetSync(StaticDataset.STOPS, null)),
                ),
                preview.observeLastSyncs()().first(),
            )
        }

    @Test
    fun lb101_thePreviewCanScriptTheNetworkComingBack() =
        runTest {
            val preview = SharedDependencies.preview().offline()

            preview.synced()

            assertEquals(AppResult.Success(SampleLines.all), preview.lines().first())
            assertEquals(
                AppResult.Success(
                    listOf(DatasetSync(StaticDataset.LINES, sample), DatasetSync(StaticDataset.STOPS, sample)),
                ),
                preview.observeLastSyncs()().first(),
            )
        }

    @Test
    fun lb101_thePreviewCanScriptASyncThatStoredNoLines() =
        runTest {
            assertEquals(
                AppResult.Success(emptyList<Line>()),
                SharedDependencies
                    .preview()
                    .neverSynced()
                    .lines()
                    .first(),
            )
        }

    @Test
    fun lb101_thePreviewCanScriptLinesThatNeverLoad() =
        runTest {
            SharedDependencies.preview().loading().lines().test {
                expectNoEvents()
            }
        }

    @Test
    fun lb101_thePreviewCanScriptACacheThatCannotBeRead() =
        runTest {
            assertEquals(
                AppResult.Failure(AppError.Data),
                SharedDependencies
                    .preview()
                    .failing()
                    .lines()
                    .first(),
            )
        }

    @Test
    fun lb103_thePreviewStartsWithNoFavouriteLines() =
        runTest {
            assertEquals(
                AppResult.Success(LinesWithFavourites(SampleLines.all, favouriteIds = emptyList())),
                SharedDependencies.preview().observeLinesWithFavourites()().first(),
            )
        }

    @Test
    fun lb103_thePreviewCanScriptFavouriteLines_inTheUsersOrder() =
        runTest {
            val preview = SharedDependencies.preview().favouriteLine(10).favouriteLine(3)

            val favourites = preview.observeLinesWithFavourites()().first()

            assertEquals(listOf(10, 3), (favourites as AppResult.Success<LinesWithFavourites>).value.favouriteIds)
        }

    @Test
    fun lb103_aFavouriteSetInThePreviewShowsAtOnce() =
        runTest {
            val preview = SharedDependencies.preview().favouriteLine(10)

            preview.observeLinesWithFavourites()().test {
                assertEquals(listOf(10), (awaitItem() as AppResult.Success<LinesWithFavourites>).value.favouriteIds)
                assertEquals(AppResult.Success(Unit), preview.setFavouriteLine()(lineId = 3, isFavourite = true))
                assertEquals(listOf(10, 3), (awaitItem() as AppResult.Success<LinesWithFavourites>).value.favouriteIds)
                preview.setFavouriteLine()(lineId = 10, isFavourite = false)
                assertEquals(listOf(3), (awaitItem() as AppResult.Success<LinesWithFavourites>).value.favouriteIds)
            }
        }

    @Test
    fun lb101_thePreviewCountsTheCollectionsOfTheLines() =
        runTest {
            val preview = SharedDependencies.preview()

            preview.lines().test {
                awaitItem()
                assertEquals(1, preview.activeCollections)
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(0, preview.activeCollections)
        }

    /** The lines alone, as the Lines tab reads them. */
    private fun PreviewUseCases.lines(): Flow<AppResult<List<Line>>> =
        observeLinesWithFavourites()().map { result ->
            when (result) {
                is AppResult.Success -> AppResult.Success(result.value.lines)
                is AppResult.Failure -> result
            }
        }
}
