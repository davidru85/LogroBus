package com.ruizurraca.logrobus.android.feature.nearme

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** The ViewModels of the Near Me screens, which the app adds to the shared graph (ADR-0014). */
val nearMeUiModule: Module =
    module {
        viewModelOf(::NearMeViewModel)
        // LB-208 part 3 (FR-FAV-02): the edit sheet that reorders, renames and removes the saved stops.
        viewModelOf(::MyStopsEditViewModel)
    }
