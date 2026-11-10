package com.ruizurraca.logrobus.android.feature.lines

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.core.common.color.onColorFor
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.core.presentation.EtaDisplay
import com.ruizurraca.logrobus.feature.lines.presentation.DirectionOptionUi
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUi
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUiState
import com.ruizurraca.logrobus.feature.lines.presentation.StopArrivalUi
import com.ruizurraca.logrobus.feature.lines.presentation.StopRowUi
import com.ruizurraca.logrobus.transit.domain.Direction

/** One state per UX state of the line detail (LB-103, LB-104), for previews and the screenshots. Placeholder data. */
internal object LineDetailPreviewData {
    private const val COLOR = 0xFFAD1457
    private val line10 = LineDetailUi(10, "10", "El Arco - Hospital San Pedro", COLOR, onColorFor(COLOR), false)

    /** LB-104: both directions, each towards its headsign, and the selected direction's stops. */
    private val directions =
        listOf(
            DirectionOptionUi(Direction.OUTBOUND, "Hospital San Pedro", isSelected = true),
            DirectionOptionUi(Direction.INBOUND, "El Arco", isSelected = false),
        )
    private val stops =
        listOf("El Arco", "Ayuntamiento", "Gonzalo de Berceo", "Hospital San Pedro", "Varea")
            .mapIndexed { index, name ->
                StopRowUi(stopId = 100 + index, order = index + 1, name = name, lat = 42.4650, lng = -2.4456)
            }

    val loading: LineDetailUiState = LineDetailUiState.Loading
    val content: LineDetailUiState = LineDetailUiState.Content(line10, Direction.OUTBOUND, directions, stops)
    val favourite: LineDetailUiState =
        LineDetailUiState.Content(line10.copy(isFavourite = true), Direction.OUTBOUND, directions, stops)
    val error: LineDetailUiState = LineDetailUiState.Error(ErrorReason.NETWORK)

    /**
     * LB-105: every shape of a stop's next call, one per row, so a preview and its screenshots show them all: minutes,
     * the low-confidence "~", "Now", the dash of a stop with no call, and a cancelled one struck through.
     */
    val arrivals: Map<Int, StopArrivalUi> =
        mapOf(
            stops[0].stopId to StopArrivalUi(EtaDisplay.Minutes(4), isLowConfidence = false, isCancelled = false),
            stops[1].stopId to StopArrivalUi(EtaDisplay.ClockTime(18, 42), isLowConfidence = true, isCancelled = false),
            stops[2].stopId to StopArrivalUi(EtaDisplay.Now, isLowConfidence = false, isCancelled = false),
            stops[3].stopId to StopArrivalUi(null, isLowConfidence = false, isCancelled = false),
            stops[4].stopId to StopArrivalUi(EtaDisplay.Minutes(2), isLowConfidence = false, isCancelled = true),
        )
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun LineDetailContentPreview() {
    LogroBusTheme {
        Surface {
            LineDetailScreen(
                LineDetailPreviewData.content,
                onEvent = {},
                onBack = {},
                arrivals = LineDetailPreviewData.arrivals,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun LineDetailLoadingPreview() {
    LogroBusTheme { Surface { LineDetailScreen(LineDetailPreviewData.loading, onEvent = {}, onBack = {}) } }
}

@PreviewLightDark
@Composable
private fun LineDetailFavouritePreview() {
    LogroBusTheme {
        Surface {
            LineDetailScreen(
                LineDetailPreviewData.favourite,
                onEvent = {},
                onBack = {},
                arrivals = LineDetailPreviewData.arrivals,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun LineDetailErrorPreview() {
    LogroBusTheme { Surface { LineDetailScreen(LineDetailPreviewData.error, onEvent = {}, onBack = {}) } }
}
