package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

/**
 * A screen's title, for its top app bar. It is a heading, so TalkBack's heading navigation reaches it, as VoiceOver's
 * does on iOS: Material 3's top app bars do not mark their title.
 */
@Composable
fun ScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text = text, modifier = modifier.semantics { heading() })
}
