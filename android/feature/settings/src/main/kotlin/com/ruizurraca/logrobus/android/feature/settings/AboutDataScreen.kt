package com.ruizurraca.logrobus.android.feature.settings

import android.icu.text.SimpleDateFormat
import android.icu.util.GregorianCalendar
import android.icu.util.TimeZone
import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruizurraca.logrobus.android.core.designsystem.Tokens
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.core.presentation.MadridDateTime
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataEvent
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataUiState
import com.ruizurraca.logrobus.feature.settings.presentation.LastSyncUi
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import org.koin.androidx.compose.koinViewModel

/**
 * About data with its ViewModel from the graph (ADR-0014). More opens it once the app shell lands (LB-012), whose
 * scaffold passes its insets through [modifier].
 */
@Composable
fun AboutDataRoute(
    modifier: Modifier = Modifier,
    viewModel: AboutDataViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AboutDataScreen(state, viewModel::onEvent, modifier)
}

/** Renders [state] and sends [onEvent]. The data source, disclaimer and attributions come with LB-404. */
@Composable
fun AboutDataScreen(
    state: AboutDataUiState,
    onEvent: (AboutDataEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        // Spec §7.3: the final layout with placeholders, never a lone spinner.
        AboutDataUiState.Loading -> {
            LastSyncs(StaticDataset.entries.map { LastSyncUi(it, null) }, loading = true, modifier)
        }

        is AboutDataUiState.Content -> {
            LastSyncs(state.lastSyncs, loading = false, modifier)
        }

        is AboutDataUiState.Error -> {
            ErrorMessage(state.reason, onRetry = { onEvent(AboutDataEvent.Retry) }, modifier)
        }
    }
}

@Composable
private fun LastSyncs(
    lastSyncs: List<LastSyncUi>,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    val format = rememberDateTimeFormat()
    Column(modifier.verticalScroll(rememberScrollState())) {
        ListItem(
            headlineContent = {
                Text(
                    stringResource(R.string.about_last_sync_title),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall,
                )
            },
            modifier = Modifier.semantics { heading() },
        )
        lastSyncs.forEach { sync ->
            ListItem(
                headlineContent = { Text(stringResource(sync.dataset.labelRes)) },
                supportingContent = {
                    val lastSuccess = sync.lastSuccess
                    when {
                        loading -> TextPlaceholder()
                        lastSuccess == null -> Text(stringResource(R.string.about_last_sync_never))
                        else -> Text(format.format(lastSuccess))
                    }
                },
            )
        }
    }
}

/** FR-ERR-05: the reason, and Retry. */
@Composable
private fun ErrorMessage(
    reason: ErrorReason,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().padding(Tokens.Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xl, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(reason.messageRes), textAlign = TextAlign.Center)
        Button(onClick = onRetry) { Text(stringResource(R.string.error_retry)) }
    }
}

/** A line-high bar where text will be, hidden from accessibility services. */
@Composable
private fun TextPlaceholder() {
    val lineHeight =
        with(LocalDensity.current) {
            MaterialTheme.typography.bodyMedium.lineHeight
                .toDp()
        }
    Box(
        Modifier
            .fillMaxWidth(PLACEHOLDER_WIDTH)
            .height(lineHeight)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clearAndSetSemantics {},
    )
}

@get:StringRes
private val StaticDataset.labelRes: Int
    get() =
        when (this) {
            StaticDataset.LINES -> R.string.about_dataset_lines
            StaticDataset.STOPS -> R.string.about_dataset_stops
        }

@get:StringRes
private val ErrorReason.messageRes: Int
    get() =
        when (this) {
            ErrorReason.NETWORK -> R.string.error_network
            ErrorReason.SERVER -> R.string.error_server
            ErrorReason.DATA -> R.string.error_data
        }

/** Formats a Madrid wall-clock time in the device locale, as a medium date and a time in the device's 12/24-h style. */
private class DateTimeFormat(
    pattern: String,
    locale: java.util.Locale,
) {
    // The fields are already on the Madrid clock (AGENTS.md §5.5), so one fixed zone carries them through unchanged.
    private val formatter = SimpleDateFormat(pattern, locale).apply { timeZone = TimeZone.GMT_ZONE }

    fun format(time: MadridDateTime): String {
        val calendar = GregorianCalendar(TimeZone.GMT_ZONE)
        calendar.clear()
        calendar.set(time.year, time.month - 1, time.day, time.hour, time.minute)
        return formatter.format(calendar.time)
    }
}

@Composable
private fun rememberDateTimeFormat(): DateTimeFormat {
    val locale = LocalConfiguration.current.locales[0]
    val twentyFourHour = DateFormat.is24HourFormat(LocalContext.current)
    return remember(locale, twentyFourHour) {
        DateTimeFormat(DateFormat.getBestDateTimePattern(locale, if (twentyFourHour) "yMMMdHm" else "yMMMdhm"), locale)
    }
}

// A placeholder spans this share of its row: long enough to read as text, short enough not to look like content.
private const val PLACEHOLDER_WIDTH = 0.4f
