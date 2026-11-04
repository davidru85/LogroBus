package com.ruizurraca.logrobus.android.feature.settings

import com.ruizurraca.logrobus.feature.settings.presentation.AppLanguage
import kotlinx.coroutines.flow.StateFlow

/**
 * The app's own language, which Android keeps rather than the app's settings (ADR-0020). A change applies at once:
 * the activity is recreated in the new language, and the ViewModels survive it.
 */
interface AppLanguages {
    /** The language the app is set to now, then every change. */
    val current: StateFlow<AppLanguage>

    fun set(language: AppLanguage)
}
