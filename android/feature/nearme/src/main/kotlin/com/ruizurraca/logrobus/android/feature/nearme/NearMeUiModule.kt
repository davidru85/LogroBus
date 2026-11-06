package com.ruizurraca.logrobus.android.feature.nearme

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** The ViewModels of the Near Me screens, which the app adds to the shared graph (ADR-0014). */
val nearMeUiModule: Module =
    module {
        viewModelOf(::NearMeViewModel)
    }
