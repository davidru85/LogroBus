package com.ruizurraca.logrobus.umbrella

import app.cash.sqldelight.db.SqlDriver
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.error
import com.ruizurraca.logrobus.core.common.warn
import com.ruizurraca.logrobus.core.network.ClientIdentity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
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
            analytics = native,
            crashReporter = native,
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
}
