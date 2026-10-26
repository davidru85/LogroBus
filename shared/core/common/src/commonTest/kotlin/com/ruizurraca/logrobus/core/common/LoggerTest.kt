package com.ruizurraca.logrobus.core.common

import kotlin.test.Test
import kotlin.test.assertEquals

/** `architecture.md` §7: the `Logger` interface, a console implementation in debug and a quieter one in release. */
class LoggerTest {
    private val lines = mutableListOf<String>()

    @Test
    fun lb004_consoleLoggerPrintsTheLevelTagAndMessage() {
        val logger = ConsoleLogger(output = lines::add)

        logger.debug("Sync", "Lines are fresh")
        logger.info("Sync", "Lines replaced")
        logger.warn("Network", "Item dropped")
        logger.error("Network", "Request failed")

        assertEquals(
            listOf(
                "D/Sync: Lines are fresh",
                "I/Sync: Lines replaced",
                "W/Network: Item dropped",
                "E/Network: Request failed",
            ),
            lines,
        )
    }

    @Test
    fun lb004_consoleLoggerPrintsTheThrowableTypesButNeverTheirMessages() {
        // NFR-PRIV-02: exception messages can carry a request URL with search text, or a payload with coordinates.
        val cause = IllegalArgumentException("q=calle mayor 12")
        val throwable = IllegalStateException("Request failed for 42.4650,-2.4456", cause)

        ConsoleLogger(output = lines::add).error("Network", "Request failed", throwable)

        assertEquals(listOf("E/Network: Request failed [IllegalStateException <- IllegalArgumentException]"), lines)
    }

    @Test
    fun lb004_consoleLoggerDropsEntriesBelowItsMinimumLevel() {
        val logger = ConsoleLogger(minLevel = LogLevel.Warn, output = lines::add)

        logger.debug("Sync", "Lines are fresh")
        logger.info("Sync", "Lines replaced")
        logger.warn("Network", "Item dropped")
        logger.error("Network", "Request failed")

        assertEquals(listOf("W/Network: Item dropped", "E/Network: Request failed"), lines)
    }
}
