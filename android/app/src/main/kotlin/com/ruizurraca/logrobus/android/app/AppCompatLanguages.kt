package com.ruizurraca.logrobus.android.app

import androidx.core.os.LocaleListCompat
import com.ruizurraca.logrobus.android.feature.settings.AppLanguages
import com.ruizurraca.logrobus.feature.settings.presentation.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The app's own language through AppCompat, which Android keeps, on every API level (ADR-0020). */
internal class AppCompatLanguages : AppLanguages {
    override val current: StateFlow<AppLanguage> = MutableStateFlow(AppLanguage.SYSTEM)

    override fun set(language: AppLanguage) = Unit
}

/** The locales that set [language]: none follows the system. */
@Suppress("UNUSED_PARAMETER", "UnusedParameter")
internal fun localesOf(language: AppLanguage): LocaleListCompat = LocaleListCompat.getEmptyLocaleList()

/** The language that [locales] set. */
@Suppress("UNUSED_PARAMETER", "UnusedParameter")
internal fun languageOf(locales: LocaleListCompat): AppLanguage = AppLanguage.SYSTEM
