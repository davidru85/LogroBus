package com.ruizurraca.logrobus.android.app

import com.ruizurraca.logrobus.android.feature.settings.AppLanguages
import org.koin.core.module.Module
import org.koin.dsl.module

/** What only the app shell can give the feature modules: the app's language, through AppCompat (ADR-0020). */
internal val shellModule: Module =
    module {
        single<AppLanguages> { AppCompatLanguages() }
    }
