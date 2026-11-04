package com.ruizurraca.logrobus.android.app

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.ruizurraca.logrobus.android.feature.settings.AppLanguages
import com.ruizurraca.logrobus.feature.settings.presentation.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The app's own language through AppCompat, which Android keeps: from Android 13 in the system's per-app language,
 * and below it in AppCompat's own storage (ADR-0020). Setting one recreates the activity in it, with no restart.
 */
internal class AppCompatLanguages : AppLanguages {
    private val language = MutableStateFlow(languageOf(AppCompatDelegate.getApplicationLocales()))
    override val current: StateFlow<AppLanguage> = language.asStateFlow()

    /** Reads the language again, which Android's own settings may have changed since (ADR-0020). */
    fun refresh() {
        language.value = languageOf(AppCompatDelegate.getApplicationLocales())
    }

    override fun set(language: AppLanguage) {
        this.language.value = language
        AppCompatDelegate.setApplicationLocales(localesOf(language))
    }
}

/** The locales that set [language]: none follows the system. */
internal fun localesOf(language: AppLanguage): LocaleListCompat =
    when (language) {
        AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
        AppLanguage.SPANISH -> LocaleListCompat.forLanguageTags(SPANISH)
        AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags(ENGLISH)
    }

/** The language that [locales] set, whatever their region. */
internal fun languageOf(locales: LocaleListCompat): AppLanguage =
    when (locales[0]?.language) {
        SPANISH -> AppLanguage.SPANISH
        ENGLISH -> AppLanguage.ENGLISH
        else -> AppLanguage.SYSTEM
    }

private const val SPANISH = "es"
private const val ENGLISH = "en"
