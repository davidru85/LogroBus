package com.ruizurraca.logrobus.umbrella

import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

/** ADR-0014 on iOS: the graph resolves every dependency the app has. */
class IosGraphTest {
    @AfterTest
    fun stop() = stopKoin()

    @Test
    fun lb010_theIosGraphResolvesEveryCoreDependency() {
        assertResolvesTheCore(initKoin().koin)
    }
}
