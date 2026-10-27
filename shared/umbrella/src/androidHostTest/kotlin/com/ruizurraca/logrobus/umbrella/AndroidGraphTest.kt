package com.ruizurraca.logrobus.umbrella

import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.AfterTest
import kotlin.test.Test

/** ADR-0014 on Android: the graph resolves every dependency the app has, so a missing binding fails here. */
@RunWith(RobolectricTestRunner::class)
class AndroidGraphTest {
    @AfterTest
    fun stop() = stopKoin()

    @Test
    fun lb010_theAndroidGraphResolvesEveryCoreDependency() {
        initKoin(RuntimeEnvironment.getApplication())

        assertResolvesTheCore(KoinPlatform.getKoin())
    }
}
