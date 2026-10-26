package com.ruizurraca.logrobus.core.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationState
import platform.UIKit.UIApplicationWillEnterForegroundNotification

/**
 * The iOS [AppLifecycle]: the app leaves the foreground when UIKit says it entered the background, and returns when
 * it says it will enter the foreground or became active. Becoming active also covers a cold launch, where the initial
 * state may be read before the first scene connects. Being inactive, under Control Centre or a call banner, still
 * counts as foreground, because the app is visible. Create it from the main thread, as one instance for the process:
 * its observers are never removed.
 */
class NotificationAppLifecycle(
    initiallyInForeground: Boolean =
        UIApplication.sharedApplication.applicationState != UIApplicationState.UIApplicationStateBackground,
    notificationCenter: NSNotificationCenter = NSNotificationCenter.defaultCenter,
) : AppLifecycle {
    private val foreground = MutableStateFlow(initiallyInForeground)
    override val isInForeground: StateFlow<Boolean> = foreground.asStateFlow()

    init {
        notificationCenter.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, null) {
            foreground.value = false
        }
        listOf(UIApplicationWillEnterForegroundNotification, UIApplicationDidBecomeActiveNotification).forEach { name ->
            notificationCenter.addObserverForName(name, null, null) { foreground.value = true }
        }
    }
}
