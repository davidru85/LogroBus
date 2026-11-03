package com.ruizurraca.logrobus.android.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.feature.settings.presentation.AppLanguage

/** FR-SET-01: the three languages, the one in use selected. Choosing one applies it at once. */
@Composable
internal fun LanguageDialog(
    current: AppLanguage,
    onChoose: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) } },
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            // On a small screen at large text the three languages do not fit, so they scroll.
            Column(Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                AppLanguage.entries.forEach { language ->
                    ListItem(
                        headlineContent = { LanguageName(language) },
                        leadingContent = { RadioButton(selected = language == current, onClick = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier =
                            Modifier.selectable(language == current, role = Role.RadioButton) { onChoose(language) },
                    )
                }
            }
        },
    )
}

@PreviewLightDark
@Composable
private fun LanguageDialogPreview() {
    LogroBusTheme { LanguageDialog(AppLanguage.SPANISH, onChoose = {}, onDismiss = {}) }
}

/** A language's name. Spanish and English are named in themselves, and TalkBack says each in its own language. */
@Composable
internal fun LanguageName(language: AppLanguage) {
    val name = stringResource(language.labelRes)
    val tag =
        when (language) {
            AppLanguage.SYSTEM -> null
            AppLanguage.SPANISH -> "es"
            AppLanguage.ENGLISH -> "en"
        }
    Text(tag?.let { AnnotatedString(name, SpanStyle(localeList = LocaleList(it))) } ?: AnnotatedString(name))
}

@get:StringRes
private val AppLanguage.labelRes: Int
    get() =
        when (this) {
            AppLanguage.SYSTEM -> R.string.settings_language_system
            AppLanguage.SPANISH -> R.string.settings_language_spanish
            AppLanguage.ENGLISH -> R.string.settings_language_english
        }
