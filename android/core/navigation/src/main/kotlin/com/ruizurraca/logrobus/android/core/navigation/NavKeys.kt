package com.ruizurraca.logrobus.android.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// The app's destinations (architecture.md §6). Each tab keeps a back stack of these keys (FR-NAV-03, ADR-0005), and the
// app shell resolves each key to its screen, so a feature can name another feature's screen without depending on it.

/** The root of the Lines tab. */
@Serializable
data object LinesKey : NavKey

/** A line's detail (SC-04), opened from the Lines list or a favourite card (LB-103). */
@Serializable
data class LineDetailKey(
    val lineId: Int,
) : NavKey

/** A stop's detail (SC-05), pushed from line detail (LB-205 part 2, FR-NAV-07). */
@Serializable
data class StopDetailKey(
    val stopId: Int,
) : NavKey

/** The root of the Near Me tab. */
@Serializable
data object NearMeKey : NavKey

/** The root of the How to Get There tab. */
@Serializable
data object PlannerKey : NavKey

/** More, which every tab root opens from its toolbar (FR-NAV-04). */
@Serializable
data object MoreKey : NavKey

/** Settings, opened from More. */
@Serializable
data object SettingsKey : NavKey

/** About data, opened from More (FR-SET-03). */
@Serializable
data object AboutDataKey : NavKey
