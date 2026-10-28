plugins {
    id("logrobus.kmp.library")
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
