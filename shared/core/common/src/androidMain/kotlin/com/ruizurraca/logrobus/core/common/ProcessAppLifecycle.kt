package com.ruizurraca.logrobus.core.common

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The Android [AppLifecycle]: the app is in the foreground while its process lifecycle is at least started, that is,
 * while any of its activities is visible. Create it on the main thread, as `ProcessLifecycleOwner` requires.
 */
class ProcessAppLifecycle(
    lifecycle: Lifecycle = ProcessLifecycleOwner.get().lifecycle,
) : AppLifecycle {
    private val foreground = MutableStateFlow(false)
    override val isInForeground: StateFlow<Boolean> = foreground.asStateFlow()

    init {
        // A new observer is first brought up to the current state, so this also sets the initial value.
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                foreground.value = event.targetState.isAtLeast(Lifecycle.State.STARTED)
            },
        )
    }
}
