package com.ruizurraca.logrobus.buildlogic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Module dependency rules of ADR-0007 and docs/architecture.md §3.2.
 * Only production (non-test) dependencies are passed to the rules.
 */
class ModuleRulesTest {
    private fun violations(
        from: String,
        vararg to: String,
        external: List<String> = emptyList(),
    ) = ModuleRules.violations(from, to.toList(), external)

    @Test
    fun lb001_featureMayDependOnDomainAndCoreModules() {
        assertEquals(
            emptyList<String>(),
            violations(
                ":shared:feature:lines",
                ":shared:transit:domain",
                ":shared:favourites:domain",
                ":shared:core:common",
                ":shared:core:presentation",
            ),
        )
    }

    @Test
    fun lb001_featureMayNotDependOnAnotherFeature() {
        assertEquals(1, violations(":shared:feature:lines", ":shared:feature:stops").size)
    }

    @Test
    fun lb001_featureMayNotDependOnDataModules() {
        assertEquals(1, violations(":shared:feature:nearme", ":shared:transit:data").size)
    }

    @Test
    fun lb001_domainMayDependOnlyOnCoreCommon() {
        assertEquals(emptyList<String>(), violations(":shared:transit:domain", ":shared:core:common"))
        assertEquals(1, violations(":shared:transit:domain", ":shared:core:network").size)
    }

    @Test
    fun lb001_domainMayNotDependOnDataModules() {
        assertEquals(1, violations(":shared:favourites:domain", ":shared:favourites:data").size)
    }

    @Test
    fun lb001_domainMayNotUseKtorOrSqlDelight() {
        val found =
            violations(
                ":shared:transit:domain",
                external =
                    listOf(
                        "io.ktor:ktor-client-core",
                        "app.cash.sqldelight:runtime",
                        "org.jetbrains.kotlinx:kotlinx-datetime",
                    ),
            )
        assertEquals(2, found.size)
    }

    @Test
    fun lb001_dataMayDependOnItsOwnDomainAndCore() {
        assertEquals(
            emptyList<String>(),
            violations(
                ":shared:transit:data",
                ":shared:transit:domain",
                ":shared:core:network",
                ":shared:core:database",
            ),
        )
    }

    @Test
    fun lb001_dataMayNotDependOnAnotherCapabilityOrFeature() {
        assertEquals(1, violations(":shared:transit:data", ":shared:favourites:domain").size)
        assertEquals(1, violations(":shared:transit:data", ":shared:feature:lines").size)
    }

    @Test
    fun lb001_coreMayDependOnlyOnCore() {
        assertEquals(emptyList<String>(), violations(":shared:core:network", ":shared:core:common"))
        assertEquals(1, violations(":shared:core:network", ":shared:transit:domain").size)
    }

    @Test
    fun lb001_umbrellaMayDependOnEverySharedModule() {
        assertEquals(
            emptyList<String>(),
            violations(
                ":shared:umbrella",
                ":shared:core:common",
                ":shared:transit:domain",
                ":shared:transit:data",
                ":shared:feature:planner",
            ),
        )
    }

    @Test
    fun lb001_androidFeatureMayDependOnItsSharedFeatureDomainAndCores() {
        assertEquals(
            emptyList<String>(),
            violations(
                ":android:feature:lines",
                ":shared:feature:lines",
                ":shared:transit:domain",
                ":shared:core:presentation",
                ":android:core:designsystem",
            ),
        )
    }

    @Test
    fun lb001_androidFeatureMayNotDependOnOtherFeaturesOrData() {
        assertEquals(1, violations(":android:feature:lines", ":shared:feature:stops").size)
        assertEquals(1, violations(":android:feature:lines", ":android:feature:stops").size)
        assertEquals(1, violations(":android:feature:lines", ":shared:transit:data").size)
    }

    @Test
    fun lb001_androidAppMayDependOnAnyAndroidOrSharedModule() {
        assertEquals(
            emptyList<String>(),
            violations(":android:app", ":android:feature:lines", ":android:core:navigation", ":shared:umbrella"),
        )
    }

    @Test
    fun lb001_productionCodeMayNotDependOnTheTestingModule() {
        assertEquals(1, violations(":shared:feature:lines", ":shared:core:testing").size)
    }

    @Test
    fun lb001_domainMayNotUseAndroidPlatformLibraries() {
        val found =
            violations(
                ":shared:transit:domain",
                external =
                    listOf(
                        "androidx.annotation:annotation",
                        "com.google.android.gms:play-services-location",
                        "com.android.support:appcompat-v7",
                    ),
            )
        assertEquals(3, found.size)
    }

    @Test
    fun lb001_ktorIsLimitedToNetworkCoreDataAndFeatureModules() {
        val ktor = listOf("io.ktor:ktor-client-core")
        assertEquals(emptyList<String>(), violations(":shared:core:network", external = ktor))
        assertEquals(emptyList<String>(), violations(":shared:transit:data", external = ktor))
        assertEquals(emptyList<String>(), violations(":shared:feature:planner", external = ktor))
        assertEquals(1, violations(":shared:core:common", external = ktor).size)
        assertEquals(1, violations(":shared:umbrella", external = ktor).size)
    }

    @Test
    fun lb001_sqlDelightIsLimitedToDatabaseCoreDataAndFeatureModules() {
        val sqlDelight = listOf("app.cash.sqldelight:runtime")
        assertEquals(emptyList<String>(), violations(":shared:core:database", external = sqlDelight))
        assertEquals(emptyList<String>(), violations(":shared:favourites:data", external = sqlDelight))
        assertEquals(1, violations(":shared:core:polling", external = sqlDelight).size)
    }

    @Test
    fun lb001_testingModuleMayDependOnCoreAndDomainModules() {
        assertEquals(
            emptyList<String>(),
            violations(
                ":shared:core:testing",
                ":shared:core:common",
                ":shared:transit:domain",
                ":shared:favourites:domain",
            ),
        )
        assertEquals(1, violations(":shared:core:testing", ":shared:transit:data").size)
    }

    @Test
    fun lb001_unclassifiedModulesAreReported() {
        assertEquals(1, violations(":shared:misc", ":shared:core:common").size)
        assertEquals(1, violations(":shared:feature:lines", ":shared:misc").size)
    }

    @Test
    fun lb001_violationMessageNamesBothModulesAndTheDecision() {
        val message = violations(":shared:feature:lines", ":shared:feature:stops").single()
        assertTrue(":shared:feature:lines" in message, message)
        assertTrue(":shared:feature:stops" in message, message)
        assertTrue("ADR-0007" in message, message)
    }

    @Test
    fun lb017_noModuleButTheAndroidAppMayUseAVendorTelemetrySdk() {
        // ADR-0009: the vendor implementations are classes in the app module, so no shared module, Android core or
        // Android feature ever references a vendor SDK.
        val vendors =
            listOf(
                "com.google.firebase:firebase-analytics",
                "com.google.firebase:firebase-crashlytics",
                "com.google.android.gms:play-services-measurement-api",
                "io.sentry:sentry-kotlin-multiplatform",
                "com.bugsnag:bugsnag-android",
                "com.datadoghq:dd-sdk-android-rum",
                "com.amplitude:analytics-android",
                "com.mixpanel.android:mixpanel-android",
            )
        val modules =
            listOf(
                ":shared:core:telemetry",
                ":shared:core:common",
                ":shared:transit:data",
                ":shared:feature:lines",
                ":shared:umbrella",
                ":android:core:designsystem",
                ":android:feature:lines",
            )

        modules.forEach { from -> assertEquals(vendors.size, violations(from, external = vendors).size, from) }
    }

    @Test
    fun lb017_theAndroidAppMayUseAVendorTelemetrySdk() {
        val firebase = listOf("com.google.firebase:firebase-analytics", "com.google.firebase:firebase-crashlytics")

        assertEquals(emptyList<String>(), violations(":android:app", external = firebase))
    }

    @Test
    fun lb017_googlePlayServicesLocationIsNotAVendorTelemetrySdk() {
        val location = listOf("com.google.android.gms:play-services-location")

        assertEquals(emptyList<String>(), violations(":shared:core:location", external = location))
    }
}
