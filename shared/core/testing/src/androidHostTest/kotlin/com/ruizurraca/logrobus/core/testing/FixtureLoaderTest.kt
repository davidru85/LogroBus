package com.ruizurraca.logrobus.core.testing

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * LB-005: `FixtureLoader` reaches the golden payloads of `fixtures/` from a test (docs/testing_strategy.md §4.1). The
 * directory arrives in the environment variable the Gradle convention sets on every test task, so these assertions
 * cover the build's plumbing as much as the loader's reading.
 *
 * A JVM host test, because the Android target is the one whose test task runs on this machine by default; the iOS
 * actual reads the same variable and is exercised by the iOS test job.
 */
class FixtureLoaderTest {
    @Test
    fun lb005_theLoaderReadsACommittedFixture() {
        val payload = FixtureLoader.read(LINES_FIXTURE)

        // The capture of API-01 is 865 032 bytes; the floor fails a truncated or empty read without pinning the size,
        // which drifts every time the source is recorded again.
        assertTrue(
            payload.length > MINIMUM_FIXTURE_LENGTH,
            "$LINES_FIXTURE is ${payload.length} characters, expected more than $MINIMUM_FIXTURE_LENGTH",
        )
        assertContains(
            payload,
            "\"lines\"",
            message = "the capture is the source's `lines` envelope (api_spec.md §4.1)",
        )
    }

    @Test
    fun lb005_aMissingFixtureFailsWithItsName() {
        val missing = "no_such_fixture.json"

        val failure = assertFailsWith<IllegalStateException> { FixtureLoader.read(missing) }

        // The file and the directory, so a typo in a fixture name reads as a typo (FixtureLoader.read's contract).
        assertContains(failure.message.orEmpty(), missing, message = "the failure names the missing file")
        assertContains(failure.message.orEmpty(), FIXTURES_DIRECTORY, message = "the failure names the directory")
    }

    private companion object {
        const val LINES_FIXTURE = "lines_2026-10-22.json"

        /** 100 kB, the order of magnitude of every capture in `fixtures/` (LB-003). */
        const val MINIMUM_FIXTURE_LENGTH = 100_000
    }
}
