package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.presentation.TopLevelDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** SharedDependencies is the only entry point Swift uses (architecture.md §5.2). */
class SharedDependenciesTest {
    @Test
    fun lb001_swiftEntryPointExposesTheStartDestination() {
        assertEquals(TopLevelDestination.LINES, SharedDependencies.startDestination)
    }

    @Test
    fun lb010_aUseCaseAskedForBeforeTheGraphStartsSaysWhatToDo() {
        // A Swift preview or test that forgets preview() would otherwise stop at Koin's own message.
        val failure = assertFailsWith<IllegalStateException> { SharedDependencies.observeLastSyncs() }

        assertTrue(failure.message.orEmpty().contains("preview()"), failure.message)
    }
}
