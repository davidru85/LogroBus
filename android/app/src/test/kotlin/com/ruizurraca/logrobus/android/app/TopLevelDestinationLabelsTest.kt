package com.ruizurraca.logrobus.android.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.core.presentation.TopLevelDestination
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/** The tab labels of design_guidelines.md §2.1 and §9: short labels; "How to get there" is the screen title. */
@RunWith(AndroidJUnit4::class)
class TopLevelDestinationLabelsTest {
    // Robolectric starts a new Application, and so a new Koin graph, for every test in one process (ADR-0014).
    @After
    fun stopTheGraph() = stopKoin()

    @Test
    @Config(qualifiers = "en")
    fun lb012_tabLabelsFollowTheDesignGuidelines() {
        assertEquals(listOf("Lines", "Near me", "Directions"), labels())
    }

    @Test
    @Config(qualifiers = "es")
    fun lb012_tabLabelsFollowTheDesignGuidelines_inSpanish() {
        assertEquals(listOf("Líneas", "Cerca de mí", "Cómo llegar"), labels())
    }

    private fun labels(): List<String> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TopLevelDestination.entries.map { context.getString(it.labelRes) }
    }
}
