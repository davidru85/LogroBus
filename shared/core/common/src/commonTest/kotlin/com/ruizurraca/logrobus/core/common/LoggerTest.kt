package com.ruizurraca.logrobus.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun lb004_consoleLoggerPrintsTheThrowable() {
        ConsoleLogger(output = lines::add).error("Network", "Request failed", IllegalStateException("timeout"))

        val line = lines.single()
        assertTrue(line.startsWith("E/Network: Request failed\n"), line)
        assertTrue("IllegalStateException: timeout" in line, line)
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
