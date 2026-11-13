package com.ruizurraca.logrobus.android.feature.stops

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** The ViewModel of the stop detail, which the app adds to the shared graph (ADR-0014). */
val stopsUiModule: Module =
    module {
        // The stop comes from the navigation key, as a parameter of koinViewModel().
        viewModel { (stopId: Int) -> StopDetailViewModel(stopId, get(), get(), get(), get(), get()) }
        // LB-208 (FR-STOP-08): the stop's own save state, from the same navigation key.
        viewModel { (stopId: Int) -> SavedStopViewModel(stopId, get()) }
    }
