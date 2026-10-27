package com.ruizurraca.logrobus.core.database

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** ADR-0013 on Android: the app's database lives in a file and keeps its data, its search index included. */
@RunWith(RobolectricTestRunner::class)
class AndroidDatabaseTest {
    private val application = RuntimeEnvironment.getApplication()

    @Test
    fun lb007_androidKeepsTheDatabaseInAFileAcrossLaunches() {
        logroBusDriver(application).run {
            insertStop(38, "Vara de Rey")
            close()
        }

        val reopened = logroBusDriver(application)

        assertTrue(application.getDatabasePath("logrobus.db").exists())
        assertEquals(listOf(38L), logroBusDatabase(reopened).searchStops("vara").map { it.id })
        reopened.close()
    }
}
