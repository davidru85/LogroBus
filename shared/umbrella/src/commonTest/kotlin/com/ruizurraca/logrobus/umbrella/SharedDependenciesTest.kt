package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.presentation.TopLevelDestination
import kotlin.test.Test
import kotlin.test.assertEquals

/** SharedDependencies is the only entry point Swift uses (architecture.md §5.2). */
class SharedDependenciesTest {
    @Test
    fun lb001_swiftEntryPointExposesTheStartDestination() {
        assertEquals(TopLevelDestination.LINES, SharedDependencies.startDestination)
    }
}
