package com.ruizurraca.logrobus.core.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** AGENTS.md §5.5: instants are stored in UTC, as epoch milliseconds, and read back as `Instant`. */
class InstantColumnTest {
    @Test
    fun lb007_instantsAreStoredAsUtcEpochMilliseconds() {
        val driver = testDriver()
        val database = logroBusDatabase(driver)
        val at = Instant.parse("2026-10-27T08:00:00.123Z")

        database.syncQueries.recordSync(dataset = "stops", last_success = at, content_hash = "e3b0c442")

        assertEquals(listOf(at.toEpochMilliseconds().toString()), driver.strings("SELECT last_success FROM sync_state"))
        assertEquals(at, database.syncQueries.syncState("stops").executeAsOne().last_success)
    }
}
