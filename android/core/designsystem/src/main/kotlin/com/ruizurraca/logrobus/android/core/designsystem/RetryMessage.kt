package com.ruizurraca.logrobus.android.core.designsystem

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.ruizurraca.logrobus.core.presentation.ErrorReason

/**
 * The Error and Empty states of spec §7.3: one sentence and Retry (FR-ERR-05), centred. At large font sizes in a short
 * window it scrolls rather than clip.
 */
@Composable
fun RetryMessage(
    text: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .fillMaxWidth()
                .padding(Tokens.Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xl, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text, textAlign = TextAlign.Center)
            Button(onClick = onRetry) { Text(stringResource(R.string.error_retry)) }
        }
    }
}

/** FR-ERR-05: the short reason an Error state gives (design_guidelines.md §9, keys `error.*`). */
@get:StringRes
val ErrorReason.messageRes: Int
    get() =
        when (this) {
            ErrorReason.NETWORK -> R.string.error_network
            ErrorReason.SERVER -> R.string.error_server
            ErrorReason.DATA -> R.string.error_data
        }
