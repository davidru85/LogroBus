package com.ruizurraca.logrobus.core.common

import kotlinx.coroutines.flow.StateFlow

/** Whether the app is in the foreground. Polling pauses while it is not (spec §8.6, LB-009). */
interface AppLifecycle {
    val isInForeground: StateFlow<Boolean>
}
