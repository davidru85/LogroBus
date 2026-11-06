package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.feature.planner.domain.ReadInitialOrigin
import kotlin.time.Instant

/**
 * How to Get There's use cases for previews and Swift tests: the initial origin over [location]'s scripted
 * permission (LB-301), and a clock that stands at [now] (LB-303). [PreviewUseCases] delegates to it, as it does
 * Settings' and Near Me's.
 */
internal class PreviewPlanner(
    private val location: PreviewLocation,
    private val now: Instant,
) : PlannerUseCases {
    override fun readInitialOrigin(): ReadInitialOrigin = location.readInitialOrigin()

    override fun clock(): AppClock = AppClock { now }
}
