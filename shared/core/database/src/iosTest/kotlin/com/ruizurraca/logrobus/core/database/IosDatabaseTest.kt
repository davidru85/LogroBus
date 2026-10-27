package com.ruizurraca.logrobus.core.database

import co.touchlab.sqliter.DatabaseFileContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** ADR-0013 on iOS: the app's database lives in a file and keeps its data, its search index included. */
class IosDatabaseTest {
    private val name = "lb007-test.db"

    @AfterTest
    fun deleteTheFile() = DatabaseFileContext.deleteDatabase(name)

    @Test
    fun lb007_iosKeepsTheDatabaseInAFileAcrossLaunches() {
        logroBusDriver(name).run {
            insertStop(38, "Vara de Rey")
            close()
        }

        val reopened = logroBusDriver(name)

        assertEquals(listOf(38L), LogroBusDatabase(reopened).searchStops("vara").map { it.id })
        reopened.close()
    }
}
