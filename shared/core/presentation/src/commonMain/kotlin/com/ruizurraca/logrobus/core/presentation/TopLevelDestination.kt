package com.ruizurraca.logrobus.core.presentation

/**
 * The app's top-level destinations, in tab order (FR-NAV-01). Each platform maps them to its own labels,
 * icons and back stacks (ADR-0002, ADR-0005); shared code carries only the meaning.
 */
enum class TopLevelDestination {
  LINES,
  NEAR_ME,
  PLANNER,
    ;

    companion object {
        /** FR-NAV-02: the app always opens on Lines (OQ-15). */
        val start: TopLevelDestination = LINES
    }
}
