package com.ruizurraca.logrobus.android.feature.planner

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** The ViewModels of the How to Get There screens, which the app adds to the shared graph (ADR-0014). */
val plannerUiModule: Module =
    module {
        viewModelOf(::PlannerViewModel)
    }
