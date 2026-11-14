package com.ruizurraca.logrobus.android.feature.stops

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.core.presentation.EtaDisplay
import com.ruizurraca.logrobus.core.presentation.ServedArrivalUi
import com.ruizurraca.logrobus.feature.stops.presentation.StopDetailUiState
import com.ruizurraca.logrobus.feature.stops.presentation.StopUpdatedUi
import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.ServedLine
import com.ruizurraca.logrobus.transit.domain.Stop

/** The stop detail's states, for previews and the screenshots. Placeholder data. */
internal object StopDetailPreviewData {
    private const val LINE_1 = 0xFFE30613L
    private const val LINE_33 = 0xFF1E7A3FL
    private const val WHITE = 0xFFFFFFFFL

    private val stop =
        Stop(
            id = 38,
            name = "Gonzalo de Berceo",
            lat = 42.4650,
            lng = -2.4456,
            isOperational = true,
            isProvisional = false,
            servedLines =
                listOf(
                    ServedLine(1, "1", "Hospital San Pedro", Direction.OUTBOUND, LINE_1, WHITE),
                    ServedLine(33, "33", "El Arco", Direction.INBOUND, LINE_33, WHITE),
                ),
        )

    /** Every shape of an ETA the criterion names: Now, minutes, a clock time, a low-confidence and a cancelled one. */
    private val arrivals =
        listOf(
            ServedArrivalUi("1", LINE_1, WHITE, "Hospital San Pedro", EtaDisplay.Now, false, false),
            ServedArrivalUi("33", LINE_33, WHITE, "El Arco", EtaDisplay.Minutes(4), false, false),
            ServedArrivalUi("1", LINE_1, WHITE, "Hospital San Pedro", EtaDisplay.ClockTime(18, 42), true, false),
            ServedArrivalUi("33", LINE_33, WHITE, "El Arco", EtaDisplay.Minutes(2), true, false),
            ServedArrivalUi("1", LINE_1, WHITE, "Hospital San Pedro", null, false, false),
        )

    val loading: StopDetailUiState = StopDetailUiState.Loading

    /** Fetched a moment ago: no "Updated" line, and every ETA as the criterion words it. */
    val content: StopDetailUiState =
        StopDetailUiState.Content(stop, arrivals, StopUpdatedUi(ageSeconds = 12, isStale = false, needsRefresh = false))

    /** 50 s: "Updated 50 s ago" in the warning colour (FR-STOP-05). */
    val stale: StopDetailUiState =
        StopDetailUiState.Content(stop, arrivals, StopUpdatedUi(ageSeconds = 50, isStale = true, needsRefresh = false))

    /** Past 120 s: the rows are already clock times, and "Tap to refresh" shows (spec §5.7). */
    val expired: StopDetailUiState =
        StopDetailUiState.Content(
            stop,
            arrivals.map { it.copy(eta = EtaDisplay.ClockTime(18, 42)) },
            StopUpdatedUi(ageSeconds = 121, isStale = true, needsRefresh = true),
        )

    /** FR-ERR-05: the message for the reason, and Retry. */
    val error: StopDetailUiState = StopDetailUiState.Error(ErrorReason.NETWORK)
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun StopDetailContentPreview() {
    LogroBusTheme { Surface { StopDetailScreen(StopDetailPreviewData.content, onRetry = {}, onBack = {}) } }
}

@PreviewLightDark
@Composable
private fun StopDetailStalePreview() {
    LogroBusTheme { Surface { StopDetailScreen(StopDetailPreviewData.stale, onRetry = {}, onBack = {}) } }
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun StopDetailExpiredPreview() {
    LogroBusTheme { Surface { StopDetailScreen(StopDetailPreviewData.expired, onRetry = {}, onBack = {}) } }
}

@PreviewLightDark
@Composable
private fun StopDetailLoadingPreview() {
    LogroBusTheme { Surface { StopDetailScreen(StopDetailPreviewData.loading, onRetry = {}, onBack = {}) } }
}

@PreviewLightDark
@Composable
private fun StopDetailErrorPreview() {
    LogroBusTheme { Surface { StopDetailScreen(StopDetailPreviewData.error, onRetry = {}, onBack = {}) } }
}
