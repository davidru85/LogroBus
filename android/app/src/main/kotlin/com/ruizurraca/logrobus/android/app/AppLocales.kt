package com.ruizurraca.logrobus.android.app

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * NFR-I18N-01 (LB-405): of the device's [locales], those in the app's languages, Spanish and English, in their order;
 * with neither, Spanish with the device's region and regional preferences. The app's own strings fall back to Spanish
 * by themselves, from `values/`; this locale makes Material's and Android's strings, the plural rules and the date
 * formats agree with them.
 */
internal fun supportedLocales(locales: LocaleList): LocaleList {
    val spoken =
        (0 until locales.size())
            .map(locales::get)
            .filter { it.language == SPANISH || it.language == ENGLISH }
    val fallback = if (locales.isEmpty) Locale.forLanguageTag(SPANISH) else spanishIn(locales[0])
    return LocaleList.forLanguageTags(spoken.ifEmpty { listOf(fallback) }.joinToString(",") { it.toLanguageTag() })
}

/** This context in the [supportedLocales] of its configuration: itself when it already speaks only those. */
internal fun Context.inSupportedLocales(): Context {
    val configuration = resources.configuration
    val supported = supportedLocales(configuration.locales)
    if (supported == configuration.locales) return this
    // A whole copy: the activity is recreated at every change, so nothing it pins goes stale.
    return createConfigurationContext(Configuration(configuration).apply { setLocales(supported) })
}

/** Spanish with [locale]'s region and extensions, or plain Spanish when they do not make a well-formed locale. */
private fun spanishIn(locale: Locale): Locale =
    runCatching {
        Locale
            .Builder()
            .setLocale(locale)
            .setLanguage(SPANISH)
            .setScript("")
            .setVariant("")
            .build()
    }.getOrDefault(Locale.forLanguageTag(SPANISH))
