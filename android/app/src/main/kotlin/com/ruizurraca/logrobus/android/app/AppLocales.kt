package com.ruizurraca.logrobus.android.app

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * NFR-I18N-01 (LB-405): of the device's [locales], those in the app's languages, Spanish and English, in their order;
 * with neither, Spanish in the device's region. The app's own strings fall back to Spanish by themselves, from
 * `values/`; this locale makes Material's and Android's strings, the plural rules and the date formats agree with them.
 */
internal fun supportedLocales(locales: LocaleList): LocaleList {
    val spoken =
        (0 until locales.size())
            .map(locales::get)
            .filter { it.language == SPANISH || it.language == ENGLISH }
            .map(Locale::toLanguageTag)
    val region = if (locales.isEmpty) "" else locales[0].country
    val spanish = if (region.isEmpty()) SPANISH else "$SPANISH-$region"
    return LocaleList.forLanguageTags(spoken.ifEmpty { listOf(spanish) }.joinToString(","))
}

/** This context in the [supportedLocales] of its configuration: itself when it already speaks only those. */
internal fun Context.inSupportedLocales(): Context {
    val configuration = resources.configuration
    val locales = supportedLocales(configuration.locales)
    if (locales == configuration.locales) return this
    return createConfigurationContext(Configuration(configuration).apply { setLocales(locales) })
}
