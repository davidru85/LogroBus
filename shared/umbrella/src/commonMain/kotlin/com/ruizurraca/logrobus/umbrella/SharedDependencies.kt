package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.presentation.TopLevelDestination
import org.koin.mp.KoinPlatform

/**
 * The only entry point Swift uses to reach shared code (architecture.md §5.2, ADR-0004). It hands out shared
 * values and, from LB-010, the use cases; Swift never reaches Koin directly.
 */
object SharedDependencies {
    /** FR-NAV-02: the tab the app opens on. */
    val startDestination: TopLevelDestination
        get() = TopLevelDestination.start

    /** Whether the app has started the dependency graph, which the use cases come from (ADR-0014). */
    val isReady: Boolean
        get() = KoinPlatform.getKoinOrNull() != null
}
