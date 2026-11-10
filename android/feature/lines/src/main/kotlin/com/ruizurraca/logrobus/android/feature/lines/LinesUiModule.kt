package com.ruizurraca.logrobus.android.feature.lines

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** The ViewModels of the Lines screens, which the app adds to the shared graph (ADR-0014). */
val linesUiModule: Module =
    module {
        viewModelOf(::LinesViewModel)
        // The line comes from the navigation key, as a parameter of koinViewModel().
        viewModel { (lineId: Int) -> LineDetailViewModel(lineId, get(), get(), get(), get(), get()) }
    }
