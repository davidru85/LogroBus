package com.ruizurraca.logrobus.umbrella

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.core.preferences.ThemePreference
import com.ruizurraca.logrobus.core.preferences.UserPreferences
import com.ruizurraca.logrobus.favourites.domain.FAVOURITES_PER_TYPE
import com.ruizurraca.logrobus.feature.lines.domain.FavouriteToggle
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
    fun lb103_scriptingAFavouriteLineTwiceKeepsOne() =
        runTest {
            // As the toggle does: two cards with one id would break SwiftUI's ForEach and Compose's keys.
            val preview = SharedDependencies.preview().favouriteLine(10).favouriteLine(10)

            val favourites = preview.observeLinesWithFavourites()().first()

            assertEquals(listOf(10), (favourites as AppResult.Success<LinesWithFavourites>).value.favouriteIds)
        }

    @Test
    fun lb103_aFavouriteSetInThePreviewShowsAtOnce() =
        runTest {
            val preview = SharedDependencies.preview().favouriteLine(10)

            preview.observeLinesWithFavourites()().test {
                assertEquals(listOf(10), (awaitItem() as AppResult.Success<LinesWithFavourites>).value.favouriteIds)
                assertEquals(
                    AppResult.Success(FavouriteToggle.Done),
                    preview.setFavouriteLine()(lineId = 3, isFavourite = true),
                )
                assertEquals(listOf(10, 3), (awaitItem() as AppResult.Success<LinesWithFavourites>).value.favouriteIds)
                preview.setFavouriteLine()(lineId = 10, isFavourite = false)
                assertEquals(listOf(3), (awaitItem() as AppResult.Success<LinesWithFavourites>).value.favouriteIds)
            }
        }

    @Test
    fun lb110_thePreviewKeepsTheLimitOfFavouriteLines() =
        runTest {
            // Swift tests script 20 favourites to show the limit message; the ids need not be in the catalogue.
            val preview = SharedDependencies.preview()
            (1..FAVOURITES_PER_TYPE).forEach { preview.favouriteLine(it) }

            assertEquals(
                AppResult.Success(FavouriteToggle.LimitReached(limit = FAVOURITES_PER_TYPE)),
                preview.setFavouriteLine()(lineId = 101, isFavourite = true),
            )
        }

    @Test
    fun lb110_aNewOrderSetInThePreviewShowsAtOnce() =
        runTest {
            val preview =
                SharedDependencies
                    .preview()
                    .favouriteLine(10)
                    .favouriteLine(3)
                    .favouriteLine(101)

            preview.observeLinesWithFavourites()().test {
                assertEquals(
                    listOf(10, 3, 101),
                    (awaitItem() as AppResult.Success<LinesWithFavourites>).value.favouriteIds,
                )
                assertEquals(AppResult.Success(Unit), preview.reorderFavouriteLines()(listOf(101, 10, 3)))
                assertEquals(
                    listOf(101, 10, 3),
                    (awaitItem() as AppResult.Success<LinesWithFavourites>).value.favouriteIds,
                )
            }
        }

    @Test
    fun lb110_aNewOrderInThePreviewKeepsALineNamedTwiceOnce() =
        runTest {
            // Two rows with one id would break SwiftUI's ForEach.
            val preview = SharedDependencies.preview().favouriteLine(10).favouriteLine(3)

            preview.reorderFavouriteLines()(listOf(3, 3, 10))

            val favourites = preview.observeLinesWithFavourites()().first()
            assertEquals(listOf(3, 10), (favourites as AppResult.Success<LinesWithFavourites>).value.favouriteIds)
        }

    @Test
    fun lb110_aFailingPreviewFailsToSaveOrReorderAFavourite() =
        runTest {
            // failing() scripts a database that cannot be used, for reads and writes alike.
            val preview = SharedDependencies.preview().favouriteLine(10).failing()

            assertEquals(AppResult.Failure(AppError.Data), preview.setFavouriteLine()(lineId = 3, isFavourite = true))
            assertEquals(AppResult.Failure(AppError.Data), preview.reorderFavouriteLines()(listOf(10)))
        }

    @Test
    fun lb403_thePreviewStartsWithTheDefaultSettings() =
        runTest {
            assertEquals(UserPreferences(), SharedDependencies.preview().observePreferences()().first())
        }

    @Test
    fun lb403_thePreviewCanScriptTheSettings() =
        runTest {
            val preview = SharedDependencies.preview().settings(ThemePreference.DARK, EtaFormat.CLOCK_TIME)

            assertEquals(
                UserPreferences(ThemePreference.DARK, EtaFormat.CLOCK_TIME),
                preview.observePreferences()().first(),
            )
        }

    @Test
    fun lb403_aSettingChangedInThePreviewShowsAtOnce() =
        runTest {
            val preview = SharedDependencies.preview()

            preview.observePreferences()().test {
                assertEquals(UserPreferences(), awaitItem())
                assertEquals(AppResult.Success(Unit), preview.setTheme()(ThemePreference.LIGHT))
                assertEquals(UserPreferences(theme = ThemePreference.LIGHT), awaitItem())
                assertEquals(AppResult.Success(Unit), preview.setEtaFormat()(EtaFormat.CLOCK_TIME))
                assertEquals(UserPreferences(ThemePreference.LIGHT, EtaFormat.CLOCK_TIME), awaitItem())
            }
        }

    @Test
    fun lb403_thePreviewCountsTheTimesTheCacheWasCleared() =
        runTest {
            val preview = SharedDependencies.preview()

            assertEquals(AppResult.Success(Unit), preview.clearCache()())
            assertEquals(1, preview.cacheClears)
        }

    @Test
    fun lb403_aFailingPreviewFailsToSaveASettingOrClearTheCache() =
        runTest {
            val preview = SharedDependencies.preview().failing()

            assertEquals(AppResult.Failure(AppError.Data), preview.setTheme()(ThemePreference.DARK))
            assertEquals(AppResult.Failure(AppError.Data), preview.setEtaFormat()(EtaFormat.CLOCK_TIME))
            assertEquals(AppResult.Failure(AppError.Data), preview.clearCache()())
            assertEquals(0, preview.cacheClears)
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
