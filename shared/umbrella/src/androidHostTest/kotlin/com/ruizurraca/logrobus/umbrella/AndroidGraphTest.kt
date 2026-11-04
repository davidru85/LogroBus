package com.ruizurraca.logrobus.umbrella

import app.cash.sqldelight.db.SqlDriver
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.error
import com.ruizurraca.logrobus.core.common.warn
import com.ruizurraca.logrobus.core.network.ClientIdentity
import com.ruizurraca.logrobus.core.preferences.ThemePreference
import com.ruizurraca.logrobus.feature.lines.domain.LinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLines
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.SetFavouriteLine
import com.ruizurraca.logrobus.feature.settings.domain.ObservePreferences
import com.ruizurraca.logrobus.feature.settings.domain.SetTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** ADR-0014 on Android: the graph resolves every dependency the app has, so a missing binding fails here. */
@RunWith(RobolectricTestRunner::class)
class AndroidGraphTest {
    @AfterTest
    fun stop() = stopKoin()

    @Test
    fun lb010_theAndroidGraphResolvesEveryCoreDependency() {
        initKoin(RuntimeEnvironment.getApplication(), appVersion = "1.2.0", debug = true)

        assertResolvesTheCore(KoinPlatform.getKoin())
    }

    @Test
    fun lb017_initKoinTakesTheAppsOwnTelemetry() {
        // The injection point for the vendor implementations of LB-409, in the app module (ADR-0009).
        val native = RecordingTelemetry()

        initKoin(
            RuntimeEnvironment.getApplication(),
            appVersion = "1.2.0",
            debug = false,
            telemetry = PlatformTelemetry(analytics = native, crashReporter = native),
        )

        assertTheGraphUsesTheNativeTelemetry(KoinPlatform.getKoin(), native)
    }

    @Test
    fun lb010_theGraphGivesSwiftEveryUseCase() {
        initKoin(RuntimeEnvironment.getApplication(), appVersion = "1.2.0", debug = true)

        // Every function of UseCases, so a use case added for Swift is checked without editing this test.
        val useCases = UseCases::class.java.methods
        assertTrue(useCases.isNotEmpty())
        useCases.forEach {
            // Swift asks for a use case with no arguments and passes its inputs to the use case itself.
            assertEquals(0, it.parameterCount, "${it.name} takes parameters")
            assertNotNull(it.invoke(SharedDependencies), it.name)
        }
    }

    @Test
    fun lb010_sharedDependenciesKnowsWhenTheGraphIsReady() {
        assertFalse(SharedDependencies.isReady)

        initKoin(RuntimeEnvironment.getApplication(), appVersion = "1.2.0", debug = true)

        assertTrue(SharedDependencies.isReady)
    }

    @Test
    fun lb010_theAppShellSetsTheVersionAndTheLogLevel() {
        initKoin(RuntimeEnvironment.getApplication(), appVersion = "1.2.0", debug = false)
        val koin = KoinPlatform.getKoin()

        // Spec §8.7: the version the operator sees in the User-Agent.
        assertEquals("LogronoBus/1.2.0", koin.get<ClientIdentity>().userAgent.substringBefore(" "))
        // security_privacy.md §4: a release build logs errors only, whatever the binary's debuggable flag says.
        val printed = ByteArrayOutputStream()
        val console = System.out
        System.setOut(PrintStream(printed))
        try {
            koin.get<Logger>().warn("Test", "Warn")
            koin.get<Logger>().error("Test", "Error")
        } finally {
            System.setOut(console)
        }
        assertEquals("E/Test: Error", printed.toString().trim())
    }

    @Test
    fun lb010_stoppingTheGraphReleasesItsScopeAndDatabase() {
        // Robolectric starts a graph per test, and part 2's tests will poll: nothing may outlive its graph.
        initKoin(RuntimeEnvironment.getApplication(), appVersion = "1.2.0", debug = true)
        val koin = KoinPlatform.getKoin()
        val scope = koin.get<CoroutineScope>(APPLICATION_SCOPE)
        val driver = koin.get<SqlDriver>().apply { execute(null, "DELETE FROM sync_state", 0) }

        stopKoin()

        assertFalse(scope.isActive)
        assertFailsWith<IllegalStateException> { driver.execute(null, "DELETE FROM sync_state", 0) }
    }

    @Test
    fun lb101_debugBuildsListTheSampleLines() =
        runTest {
            // Until LB-008 part 2 downloads the real catalogue (owner decision, 2026-11-02).
            initKoin(RuntimeEnvironment.getApplication(), appVersion = "1.2.0", debug = true)

            assertEquals(AppResult.Success(SampleLines.all), KoinPlatform.getKoin().get<ObserveLines>()().first())
        }

    @Test
    fun lb403_settingsAreKeptOnTheDevice_acrossARestart() =
        runTest {
            // DataStore allows one active instance per file in a process: stopping the graph must release it, or the
            // next graph, Robolectric's next test say, could not read the settings.
            initKoin(RuntimeEnvironment.getApplication(), appVersion = "1.2.0", debug = true)
            KoinPlatform.getKoin().get<SetTheme>()(ThemePreference.DARK)
            stopKoin()

            initKoin(RuntimeEnvironment.getApplication(), appVersion = "1.2.0", debug = true)
            val restarted = KoinPlatform.getKoin().get<ObservePreferences>()().first()

            assertEquals(ThemePreference.DARK, restarted.theme)
        }

    @Test
    fun lb103_favouriteLinesAreKeptInTheDevicesDatabase_acrossARestart() =
        runTest {
            // FR-FAV-03: favourites are the user's own, so debug builds keep them in the real database too.
            initKoin(RuntimeEnvironment.getApplication(), appVersion = "1.2.0", debug = true)
            KoinPlatform.getKoin().get<SetFavouriteLine>()(lineId = 10, isFavourite = true)
            KoinPlatform.getKoin().get<SetFavouriteLine>()(lineId = 3, isFavourite = true)
            stopKoin()

            initKoin(RuntimeEnvironment.getApplication(), appVersion = "1.2.0", debug = true)
            val restarted = KoinPlatform.getKoin().get<ObserveLinesWithFavourites>()().first()

            assertEquals(listOf(10, 3), (restarted as AppResult.Success<LinesWithFavourites>).value.favouriteIds)
        }

    @Test
    fun lb101_releaseBuildsReadTheCache_whichNothingCanFillYet() =
        runTest {
            // No sample data in a release build: the cache is empty, and there is no source to sync from until
            // LB-008 part 2, so the list shows the error with Retry.
            initKoin(RuntimeEnvironment.getApplication(), appVersion = "1.2.0", debug = false)

            assertEquals(AppResult.Failure(AppError.Data), KoinPlatform.getKoin().get<ObserveLines>()().first())
        }
}
