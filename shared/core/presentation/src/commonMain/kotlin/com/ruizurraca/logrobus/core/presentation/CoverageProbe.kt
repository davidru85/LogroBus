package com.ruizurraca.logrobus.core.presentation

/** Throwaway LB-002 probe: untested code in a module gated at 80 % line coverage. Never merged. */
internal object CoverageProbe {
    fun describe(destination: TopLevelDestination): String =
        when (destination) {
            TopLevelDestination.LINES -> "lines"
            TopLevelDestination.NEAR_ME -> "near me"
            TopLevelDestination.PLANNER -> "planner"
        }

    fun isStart(destination: TopLevelDestination): Boolean = destination == TopLevelDestination.start

    fun count(): Int = TopLevelDestination.entries.size
}
