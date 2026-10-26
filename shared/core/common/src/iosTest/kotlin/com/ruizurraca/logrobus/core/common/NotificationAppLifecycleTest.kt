package com.ruizurraca.logrobus.core.common

import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Spec §8.6: polling pauses in the background. On iOS, the application notifications say where the app is. */
class NotificationAppLifecycleTest {
    // A private centre: the test posts the notifications that UIKit would.
    private val center = NSNotificationCenter()

    @Test
    fun lb004_iosAppFollowsTheEnterBackgroundAndEnterForegroundNotifications() {
        val appLifecycle = NotificationAppLifecycle(initiallyInForeground = true, notificationCenter = center)
        val seen = mutableListOf(appLifecycle.isInForeground.value)

        center.postNotificationName(UIApplicationDidEnterBackgroundNotification, null)
        seen += appLifecycle.isInForeground.value
        center.postNotificationName(UIApplicationWillEnterForegroundNotification, null)
        seen += appLifecycle.isInForeground.value

        assertEquals(listOf(true, false, true), seen)
    }

    @Test
    fun lb004_iosLifecycleStartsFromTheGivenState() {
        val appLifecycle = NotificationAppLifecycle(initiallyInForeground = false, notificationCenter = center)

        assertFalse(appLifecycle.isInForeground.value)
    }

    @Test
    fun lb004_iosBecomingActiveMarksTheAppInTheForeground() {
        // At a cold launch the state can be read before the first scene connects, and UIKit then posts no
        // enter-foreground notification. It always posts did-become-active.
        val appLifecycle = NotificationAppLifecycle(initiallyInForeground = false, notificationCenter = center)

        center.postNotificationName(UIApplicationDidBecomeActiveNotification, null)

        assertTrue(appLifecycle.isInForeground.value)
    }
}
