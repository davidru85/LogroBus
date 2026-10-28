package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.error
import com.ruizurraca.logrobus.core.common.warn
import com.ruizurraca.logrobus.core.network.ClientIdentity
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
import kotlin.test.assertFalse
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
}
