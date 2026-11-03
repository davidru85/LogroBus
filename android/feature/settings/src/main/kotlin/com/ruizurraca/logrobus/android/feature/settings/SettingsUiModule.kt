package com.ruizurraca.logrobus.android.feature.settings

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The ViewModels of the settings screens, which the app adds to the shared graph (ADR-0014). The app also gives them
 * [AppLanguages].
 */
val settingsUiModule: Module =
    module {
        viewModelOf(::AboutDataViewModel)
        viewModelOf(::SettingsViewModel)
    }
