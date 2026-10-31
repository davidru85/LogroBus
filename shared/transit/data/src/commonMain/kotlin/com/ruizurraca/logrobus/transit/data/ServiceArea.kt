package com.ruizurraca.logrobus.transit.data

/**
 * Spec §5.4, step 7: where the network runs. An item outside it, such as a vehicle at 0,0, is dropped and counted in
 * the `DataQualityReport` (LB-006 part 2).
 */
internal object ServiceArea {
    /** Whether [lat] and [lng] are inside the service area; a missing position is not. */
    fun contains(
        lat: Double?,
        lng: Double?,
    ): Boolean = lat != null && lng != null // TDD: red
}
