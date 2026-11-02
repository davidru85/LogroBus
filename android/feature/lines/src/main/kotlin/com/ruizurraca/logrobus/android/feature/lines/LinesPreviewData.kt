package com.ruizurraca.logrobus.android.feature.lines

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.color.onColorFor
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.feature.lines.domain.LinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiMapper
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiState
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind

/**
 * One state per UX state of the Lines list, built with the shared mapper, for previews and the screenshots. The lines
 * are placeholders with the network's shape: lines 4 and B1 share a colour (DD-08), and line 11 is not operational.
 */
internal object LinesPreviewData {
    private const val RED = 0xFFE30613

    private val lines =
        listOf(
            line(1, "1", "Lardero - Hospital San Pedro", 0xFF0FDCFC),
            line(2, "2", "Yagüe - Varea", 0xFF2E7D32),
            line(4, "4", "Casa Blanca - Yagüe", RED),
            line(6, "6", "Valdegastea - Polígono Cantabria", 0xFFEF6C00),
            line(10, "10", "El Arco - Hospital San Pedro", 0xFFAD1457),
            line(11, "11", "Artesanos - Manuel de Falla", 0xFF00897B, isOperational = false),
            line(101, "B1", "Varea - La Cava", RED),
            line(103, "B3", "Valdegastea - El Cubo", 0xFF37474F),
        )

    val loading: LinesUiState = LinesUiState.Loading
    val content = LinesUiMapper.state(AppResult.Success(LinesWithFavourites(lines, favouriteIds = emptyList())))
    val empty: LinesUiState = LinesUiState.Empty
    val error: LinesUiState = LinesUiState.Error(ErrorReason.NETWORK)

    private fun line(
        id: Int,
        code: String,
        name: String,
        color: Long,
        isOperational: Boolean = true,
    ) = Line(
        id,
        code,
        name,
        color,
        onColorFor(color),
        if (code.startsWith("B")) LineKind.NIGHT else LineKind.DAY,
        isOperational,
    )
}

@Composable
private fun PreviewTheme(content: @Composable () -> Unit) {
    LogroBusTheme { Surface(content = content) }
}

@PreviewLightDark
@Composable
private fun LinesLoadingPreview() {
    PreviewTheme { LinesScreen(LinesPreviewData.loading, onEvent = {}, onOpenMore = {}) }
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun LinesContentPreview() {
    PreviewTheme { LinesScreen(LinesPreviewData.content, onEvent = {}, onOpenMore = {}) }
}

@PreviewLightDark
@Composable
private fun LinesEmptyPreview() {
    PreviewTheme { LinesScreen(LinesPreviewData.empty, onEvent = {}, onOpenMore = {}) }
}

@PreviewLightDark
@Composable
private fun LinesErrorPreview() {
    PreviewTheme { LinesScreen(LinesPreviewData.error, onEvent = {}, onOpenMore = {}) }
}
