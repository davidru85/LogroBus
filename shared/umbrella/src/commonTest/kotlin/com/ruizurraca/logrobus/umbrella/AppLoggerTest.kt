package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.LogLevel
import kotlin.test.Test
import kotlin.test.assertEquals

/** `architecture.md` §7 and `security_privacy.md` §4: release builds log errors only. */
class AppLoggerTest {
    private fun printed(debug: Boolean): List<String> {
        val lines = mutableListOf<String>()
        val logger = appLogger(debug, output = { lines += it })
        LogLevel.entries.forEach { level -> logger.log(level, "Test", level.name) }
        return lines
    }

    @Test
    fun lb010_debugBuildsLogEveryLevel() {
        assertEquals(listOf("D/Test: Debug", "I/Test: Info", "W/Test: Warn", "E/Test: Error"), printed(debug = true))
    }

    @Test
    fun lb010_releaseBuildsLogErrorsOnly() {
        assertEquals(listOf("E/Test: Error"), printed(debug = false))
    }
}
