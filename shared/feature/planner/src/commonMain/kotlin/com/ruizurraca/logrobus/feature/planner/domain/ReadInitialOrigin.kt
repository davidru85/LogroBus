package com.ruizurraca.logrobus.feature.planner.domain

import com.ruizurraca.logrobus.core.location.LocationPermission
import com.ruizurraca.logrobus.core.location.LocationProvider

/**
 * A trip's origin or destination (FR-PLAN-01). The user's location for now: stops, saved places, addresses and map
 * points come with LB-301 part 2.
 */
sealed interface PlanPlace {
    /** Wherever the user is when the plan is requested (LB-306). */
    data object MyLocation : PlanPlace
}

/**
 * FR-PLAN-11: the origin a new plan starts from: the user's location with the foreground location permission,
 * approximate or precise, and none without it. It reads the permission only: it never asks for it, which is Near Me's
 * row (LB-201), and starts no location updates. An interface, because Swift calls it and Swift tests replace it
 * (architecture.md §5.2).
 */
fun interface ReadInitialOrigin {
    operator fun invoke(): PlanPlace?
}

class DefaultReadInitialOrigin(
    private val provider: LocationProvider,
) : ReadInitialOrigin {
    override fun invoke(): PlanPlace? =
        if (provider.permission() == LocationPermission.NotGranted) null else PlanPlace.MyLocation
}
