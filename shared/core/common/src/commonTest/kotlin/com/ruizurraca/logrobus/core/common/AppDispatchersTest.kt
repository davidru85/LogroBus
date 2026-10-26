package com.ruizurraca.logrobus.core.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.minutes

/** AGENTS.md §8: dispatchers are injected, so tests run on virtual time. */
class AppDispatchersTest {
    @Suppress("InjectDispatcher") // This test checks the production defaults themselves.
    @Test
    fun lb004_appDispatchersDefaultToTheMainDefaultAndIoDispatchers() {
        val dispatchers = AppDispatchers()

        assertSame(Dispatchers.Main, dispatchers.main)
        assertSame(Dispatchers.Default, dispatchers.default)
        assertSame(Dispatchers.IO, dispatchers.io)
    }

    @OptIn(ExperimentalCoroutinesApi::class) // testTimeSource
    @Test
    fun lb004_oneTestDispatcherReplacesAllThree() =
        runTest {
            val dispatchers = AppDispatchers(StandardTestDispatcher(testScheduler))
            val finished = mutableListOf<String>()
            val start = testTimeSource.markNow()

            launch(dispatchers.main) { finished += "main" }
            launch(dispatchers.default) { finished += "default" }
            launch(dispatchers.io) {
                delay(5.minutes)
                finished += "io"
            }
            testScheduler.advanceUntilIdle()

            assertEquals(listOf("main", "default", "io"), finished)
            assertEquals(5.minutes, start.elapsedNow())
        }
}
