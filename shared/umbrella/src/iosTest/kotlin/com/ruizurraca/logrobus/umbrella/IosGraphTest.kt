package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.network.ClientIdentity
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** ADR-0014 on iOS: the graph resolves every dependency the app has. */
class IosGraphTest {
    @AfterTest
    fun stop() = stopKoin()

    @Test
    fun lb010_theIosGraphResolvesEveryCoreDependency() {
        initKoin(appVersion = "1.2.0", debug = true)

        assertResolvesTheCore(KoinPlatform.getKoin())
        // The path a Swift ViewModel takes by default. Kotlin/Native has no reflection, so each use case is listed;
        // the Android host test checks every function of UseCases.
        SharedDependencies.observeLastSyncs()
    }

    @Test
    fun lb010_theAppShellSetsTheVersion() {
        initKoin(appVersion = "1.2.0", debug = false)

        // Spec §8.7: the version the operator sees in the User-Agent.
        assertEquals(
            "LogronoBus/1.2.0",
            KoinPlatform
                .getKoin()
                .get<ClientIdentity>()
                .userAgent
                .substringBefore(" "),
        )
    }
}
