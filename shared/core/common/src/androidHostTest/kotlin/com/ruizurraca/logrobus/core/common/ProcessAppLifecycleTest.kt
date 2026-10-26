package com.ruizurraca.logrobus.core.common

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Spec §8.6: polling pauses in the background. On Android, the process lifecycle says where the app is. */
class ProcessAppLifecycleTest {
    private val owner =
        object : LifecycleOwner {
            override val lifecycle: Lifecycle get() = registry
        }

    // createUnsafe: no main-thread checks, so the test runs on the JVM.
    private val registry: LifecycleRegistry = LifecycleRegistry.createUnsafe(owner)

    @Test
    fun lb004_androidAppIsInTheForegroundWhileTheProcessIsStarted() {
        registry.currentState = Lifecycle.State.CREATED
        val appLifecycle = ProcessAppLifecycle(registry)
        val seen = mutableListOf(appLifecycle.isInForeground.value)

        listOf(Lifecycle.State.STARTED, Lifecycle.State.RESUMED, Lifecycle.State.CREATED).forEach {
            registry.currentState = it
            seen += appLifecycle.isInForeground.value
        }

        assertEquals(listOf(false, true, true, false), seen)
    }

    @Test
    fun lb004_androidLifecycleStartsFromTheCurrentProcessState() {
        registry.currentState = Lifecycle.State.RESUMED
        assertTrue(ProcessAppLifecycle(registry).isInForeground.value)

        registry.currentState = Lifecycle.State.CREATED
        assertFalse(ProcessAppLifecycle(registry).isInForeground.value)
    }
}
