package com.ruizurraca.logrobus.android.app

import androidx.annotation.StringRes
import com.ruizurraca.logrobus.core.presentation.TopLevelDestination

/**
 * The tab label of each destination (design_guidelines.md §2.1, keys `tab.*` in §9). Shared code carries the
 * destination; Android supplies the localised words (AGENTS.md §5, rule 4).
 */
@get:StringRes
internal val TopLevelDestination.labelRes: Int
    get() =
        when (this) {
            TopLevelDestination.LINES -> R.string.tab_lines
            TopLevelDestination.NEAR_ME -> R.string.tab_near_me
            TopLevelDestination.PLANNER -> R.string.tab_directions
        }
