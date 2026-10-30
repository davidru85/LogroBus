package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember

/**
 * How one screen gives way to another (design_guidelines.md §4, Motion): a cross-fade on the motion scheme's default
 * effects spring, for pushes, pops and tab switches alike.
 */
@Immutable
class ScreenTransitions internal constructor(
    private val spec: FiniteAnimationSpec<Float>,
) {
    /** A new transform on each call, because the animation writes into the one it is given. */
    fun crossFade(): ContentTransform = fadeIn(spec) togetherWith fadeOut(spec)
}

/** The [ScreenTransitions] of the current motion scheme. */
@Composable
fun rememberScreenTransitions(): ScreenTransitions {
    val spec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    return remember(spec) { ScreenTransitions(spec) }
}
