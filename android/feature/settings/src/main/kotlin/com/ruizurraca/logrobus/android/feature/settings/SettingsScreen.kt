package com.ruizurraca.logrobus.android.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruizurraca.logrobus.android.core.designsystem.ScreenTitle
import com.ruizurraca.logrobus.android.core.designsystem.UpButton
import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.core.preferences.ThemePreference
import com.ruizurraca.logrobus.feature.settings.presentation.AppLanguage
import com.ruizurraca.logrobus.feature.settings.presentation.CacheMessage
import com.ruizurraca.logrobus.feature.settings.presentation.DataStatusUi
import com.ruizurraca.logrobus.feature.settings.presentation.SettingsEvent
import com.ruizurraca.logrobus.feature.settings.presentation.SettingsUiState
import org.koin.androidx.compose.koinViewModel

/** Settings with its ViewModel from the graph (ADR-0014), opened from More (LB-012). [onBack] leaves it. */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cacheMessage by viewModel.cacheMessage.collectAsStateWithLifecycle()
    SettingsScreen(state, viewModel::onEvent, onBack, modifier, cacheMessage)
}

/**
 * Settings (LB-403, FR-SET-01): renders [state] under its top app bar and sends [onEvent]. Appearance holds the
 * language, which opens a dialog, and the theme; Arrivals the ETA format; Data the data status and Clear cache, whose
 * [cacheMessage] shows in a snackbar.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    cacheMessage: CacheMessage? = null,
) {
    val snackbars = remember { SnackbarHostState() }
    CacheMessageSnackbar(cacheMessage, snackbars) { onEvent(SettingsEvent.CacheMessageShown) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { ScreenTitle(stringResource(R.string.settings_title)) },
                navigationIcon = { UpButton(onClick = onBack) },
            )
        },
        snackbarHost = { SnackbarHost(snackbars) },
    ) { padding ->
        // Spec §7.3: the first load shows the final layout, with nothing chosen yet.
        SettingsContent(state as? SettingsUiState.Content, onEvent, padding)
    }
}

@Composable
private fun SettingsContent(
    content: SettingsUiState.Content?,
    onEvent: (SettingsEvent) -> Unit,
    contentPadding: PaddingValues,
) {
    var choosingLanguage by rememberSaveable { mutableStateOf(false) }
    val enabled = content != null
    // The padding goes inside the scroll, so the rows scroll behind the system bars rather than stop above them.
    Column(
        Modifier
            .consumeWindowInsets(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SectionHeading(R.string.settings_appearance)
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_language)) },
            supportingContent = { content?.let { LanguageName(it.language) } ?: TextPlaceholder() },
            modifier = Modifier.clickable(enabled = enabled, role = Role.Button) { choosingLanguage = true },
        )
        Choices(R.string.settings_theme) {
            ThemePreference.entries.forEach { theme ->
                Choice(theme.labelRes, selected = content?.theme == theme, enabled) {
                    onEvent(SettingsEvent.SetTheme(theme))
                }
            }
        }
        SectionHeading(R.string.settings_arrivals)
        Choices(R.string.settings_eta_format) {
            EtaFormat.entries.forEach { format ->
                Choice(format.labelRes, selected = content?.etaFormat == format, enabled, format.exampleRes) {
                    onEvent(SettingsEvent.SetEtaFormat(format))
                }
            }
        }
        SectionHeading(R.string.settings_data)
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_data_status)) },
            supportingContent = { content?.let { DataStatus(it.dataStatus) } ?: TextPlaceholder() },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_clear_cache)) },
            supportingContent = { Text(stringResource(R.string.settings_clear_cache_detail)) },
            modifier = Modifier.clickable(enabled = enabled, role = Role.Button) { onEvent(SettingsEvent.ClearCache) },
        )
    }
    if (choosingLanguage && content != null) {
        LanguageDialog(
            current = content.language,
            onChoose = { language ->
                choosingLanguage = false
                onEvent(SettingsEvent.SetLanguage(language))
            },
            onDismiss = { choosingLanguage = false },
        )
    }
}

@Composable
private fun DataStatus(status: DataStatusUi) {
    val format = rememberDateTimeFormat()
    Text(
        when (status) {
            is DataStatusUi.Synced -> stringResource(R.string.settings_data_synced, format.format(status.at))
            DataStatusUi.NeverSynced -> stringResource(R.string.settings_data_never_synced)
            DataStatusUi.Unknown -> stringResource(R.string.settings_data_unknown)
        },
    )
}

/** A section's title, a heading for TalkBack's heading navigation. */
@Composable
private fun SectionHeading(
    @StringRes title: Int,
) {
    ListItem(
        headlineContent = {
            Text(
                stringResource(title),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
            )
        },
        modifier = Modifier.semantics { heading() },
    )
}

/**
 * A setting's [choices], one of which is selected, under its name: a heading of their group, so TalkBack says whose
 * choices they are, styled as a label rather than a row to tap.
 */
@Composable
private fun Choices(
    @StringRes name: Int,
    choices: @Composable () -> Unit,
) {
    Column(Modifier.selectableGroup()) {
        ListItem(
            headlineContent = {
                Text(
                    stringResource(name),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleSmall,
                )
            },
            modifier = Modifier.semantics { heading() },
        )
        choices()
    }
}

/** One choice of a setting, a radio button with its [label] and, for the ETA format, an [example]. */
@Composable
private fun Choice(
    @StringRes label: Int,
    selected: Boolean,
    enabled: Boolean,
    @StringRes example: Int? = null,
    onSelect: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(label)) },
        supportingContent = example?.let { { Text(stringResource(it)) } },
        leadingContent = { RadioButton(selected = selected, onClick = null, enabled = enabled) },
        modifier = Modifier.selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect),
    )
}

/** Says [message] once, in a snackbar, which TalkBack announces without taking its focus (NFR-A11Y). */
@Composable
private fun CacheMessageSnackbar(
    message: CacheMessage?,
    snackbars: SnackbarHostState,
    onShown: () -> Unit,
) {
    val text = message?.let { stringResource(it.textRes) }
    val shown by rememberUpdatedState(onShown)
    LaunchedEffect(text) {
        if (text != null) {
            // Shown once, even when the screen leaves before the message ends: it must not come back on return.
            try {
                snackbars.showSnackbar(text)
            } finally {
                shown()
            }
        }
    }
}

@get:StringRes
private val ThemePreference.labelRes: Int
    get() =
        when (this) {
            ThemePreference.SYSTEM -> R.string.settings_theme_system
            ThemePreference.LIGHT -> R.string.settings_theme_light
            ThemePreference.DARK -> R.string.settings_theme_dark
        }

@get:StringRes
private val EtaFormat.labelRes: Int
    get() =
        when (this) {
            EtaFormat.MINUTES -> R.string.settings_eta_minutes
            EtaFormat.CLOCK_TIME -> R.string.settings_eta_clock_time
        }

@get:StringRes
private val EtaFormat.exampleRes: Int
    get() =
        when (this) {
            EtaFormat.MINUTES -> R.string.settings_eta_minutes_example
            EtaFormat.CLOCK_TIME -> R.string.settings_eta_clock_time_example
        }

@get:StringRes
private val CacheMessage.textRes: Int
    get() =
        when (this) {
            CacheMessage.CLEARED -> R.string.settings_cache_cleared
            CacheMessage.FAILED -> R.string.settings_cache_failed
        }
