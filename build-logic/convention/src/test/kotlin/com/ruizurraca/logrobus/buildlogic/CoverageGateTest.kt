package com.ruizurraca.logrobus.buildlogic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Which shared modules the Kover gate covers (docs/testing_strategy.md §3, NFR-MNT-01). */
class CoverageGateTest {

    @Test
    fun lb002_domainAndDataModulesAreGatedWhole() {
        assertEquals(CoverageGate.WholeModule, coverageGate(":shared:transit:domain"))
        assertEquals(CoverageGate.WholeModule, coverageGate(":shared:favourites:data"))
    }

    @Test
    fun lb002_sharedPresentationMappersAreGated() {
        assertEquals(CoverageGate.WholeModule, coverageGate(":shared:core:presentation"))
    }

    @Test
    fun lb002_featureModulesAreGatedOnTheirDomainDataAndPresentationPackages() {
        val feature = "com.ruizurraca.logrobus.feature.lines"
        assertEquals(
            CoverageGate.Packages(listOf("$feature.domain", "$feature.data", "$feature.presentation")),
            coverageGate(":shared:feature:lines"),
        )
    }

    @Test
    fun lb002_otherModulesAreMeasuredButNotGated() {
        listOf(":shared:core:common", ":shared:core:testing", ":shared:umbrella", ":android:app").forEach {
            assertNull(coverageGate(it), it)
        }
    }
}
