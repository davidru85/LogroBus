plugins {
    id("logrobus.kmp.library")
    // The source DTOs are @Serializable, so the mappers decode them through SourceJson (spec §3).
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // SQLDelight's native driver calls the system SQLite. Its plugin links it only in modules that apply it, so the
    // iOS test binaries here link it themselves (ADR-0013).
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.all { linkerOpts("-lsqlite3") }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.transit.domain)
            implementation(projects.shared.core.network)
            implementation(projects.shared.core.database)
            implementation(projects.shared.core.config)
            implementation(libs.sqldelight.coroutines)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            // The contract tests drive SourceClient over a mock engine and never call the source (AGENTS.md §9).
            implementation(libs.ktor.client.mock)
            // Contract tests decode the captures in fixtures/ through FixtureLoader (docs/testing_strategy.md §2).
            implementation(projects.shared.core.testing)
        }
        getByName("androidHostTest").dependencies {
            // An in-memory JDBC database for tests (ADR-0013).
            implementation(libs.sqldelight.sqlite.driver)
        }
        iosTest.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}
