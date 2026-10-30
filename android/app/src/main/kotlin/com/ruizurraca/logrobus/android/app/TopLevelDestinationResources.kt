package com.ruizurraca.logrobus.android.app

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ruizurraca.logrobus.core.presentation.TopLevelDestination

// Android's resources for each top-level destination. Shared code carries the destination; Android supplies the words
// and the icons (AGENTS.md §5, rule 4).

/** The tab label of each destination (design_guidelines.md §2.1, keys `tab.*` in §9). */
@get:StringRes
internal val TopLevelDestination.labelRes: Int
    get() =
        when (this) {
            TopLevelDestination.LINES -> R.string.tab_lines
            TopLevelDestination.NEAR_ME -> R.string.tab_near_me
            TopLevelDestination.PLANNER -> R.string.tab_directions
        }

/**
 * The title of each tab's root screen (design_guidelines.md §2.1): the tab label, except How to Get There, whose short
 * label "Directions" reads "How to get there" as a title (`plan.title` in §9).
 */
@get:StringRes
internal val TopLevelDestination.titleRes: Int
    get() =
        when (this) {
            TopLevelDestination.LINES -> R.string.tab_lines
            TopLevelDestination.NEAR_ME -> R.string.tab_near_me
            TopLevelDestination.PLANNER -> R.string.plan_title
        }

/** The tab icon (design_guidelines.md §2.1): outlined, and filled while selected, as Material 3 navigation items. */
@DrawableRes
internal fun TopLevelDestination.iconRes(selected: Boolean): Int =
    when (this) {
        TopLevelDestination.LINES -> {
            if (selected) R.drawable.ic_tab_lines_selected else R.drawable.ic_tab_lines
        }

        TopLevelDestination.NEAR_ME -> {
            if (selected) R.drawable.ic_tab_near_me_selected else R.drawable.ic_tab_near_me
        }

        TopLevelDestination.PLANNER -> {
            if (selected) R.drawable.ic_tab_directions_selected else R.drawable.ic_tab_directions
        }
    }
