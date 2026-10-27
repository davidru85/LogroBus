package com.ruizurraca.logrobus.buildlogic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

/** detekt (ADR-0011) judges the code people write, not the code tools generate. */
class DetektConventionsTest {
    @Test
    fun lb007_detektSkipsSourcesGeneratedUnderTheBuildDirectory() {
        // SQLDelight writes Kotlin named after the tables, such as Fare_product, which no style rule should judge.
        val module = File("/repo/shared/core/database")
        val build = File(module, "build")
        val files =
            listOf(
                "build/generated/sqldelight/code/LogroBusDatabase/commonMain/Fare_product.kt",
                "src/commonMain/kotlin/StopSearch.kt",
                "buildSrc/Other.kt",
            ).map { File(module, it) }

        assertEquals(listOf(true, false, false), files.map { isGeneratedSource(it, build) })
    }
}
