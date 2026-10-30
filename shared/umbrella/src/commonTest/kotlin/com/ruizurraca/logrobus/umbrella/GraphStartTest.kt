package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.NoOpLogger
import org.koin.core.context.stopKoin
import org.koin.core.error.DefinitionOverrideException
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Instant

/** ADR-0014: the graph has one instance of each core service, and nothing may replace one quietly. */
class GraphStartTest {
    @AfterTest
    fun stop() = stopKoin()

    @Test
    fun lb010_aSecondDefinitionOfACoreServiceFailsAtStart() {
        // A second AppClock would be one the source client never corrects (spec §8.6).
        val platform = module { single<Logger> { NoOpLogger } }
        val duplicate = module { single<AppClock> { AppClock { Instant.DISTANT_PAST } } }

        val modules = sharedModules(platform, telemetryModule(debug = false)) + duplicate

        assertFailsWith<DefinitionOverrideException> { startGraph(modules) }
    }
}
