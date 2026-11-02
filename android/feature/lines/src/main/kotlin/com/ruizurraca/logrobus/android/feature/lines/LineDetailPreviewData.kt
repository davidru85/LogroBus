package com.ruizurraca.logrobus.android.feature.lines

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.core.common.color.onColorFor
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUi
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUiState

/** One state per UX state of the line detail (LB-103), for previews and the screenshots. The line is a placeholder. */
internal object LineDetailPreviewData {
    private const val COLOR = 0xFFAD1457
    private val line10 = LineDetailUi(10, "10", "El Arco - Hospital San Pedro", COLOR, onColorFor(COLOR), false)

    val loading: LineDetailUiState = LineDetailUiState.Loading
    val content: LineDetailUiState = LineDetailUiState.Content(line10)
    val favourite: LineDetailUiState = LineDetailUiState.Content(line10.copy(isFavourite = true))
    val error: LineDetailUiState = LineDetailUiState.Error(ErrorReason.NETWORK)
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun LineDetailContentPreview() {
    LogroBusTheme { Surface { LineDetailScreen(LineDetailPreviewData.content, onEvent = {}, onBack = {}) } }
}

@PreviewLightDark
@Composable
private fun LineDetailFavouritePreview() {
    LogroBusTheme { Surface { LineDetailScreen(LineDetailPreviewData.favourite, onEvent = {}, onBack = {}) } }
}

@PreviewLightDark
@Composable
private fun LineDetailErrorPreview() {
    LogroBusTheme { Surface { LineDetailScreen(LineDetailPreviewData.error, onEvent = {}, onBack = {}) } }
}
