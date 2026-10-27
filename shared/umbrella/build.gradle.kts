import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("logrobus.kmp.library")
}

// The single Kotlin artefact consumed by iOS (ADR-0004, ADR-0007, ADR-0010): a static
// framework named "Shared", packaged as an XCFramework for the local Swift package
// LogroBusUI, through which the app links it once. Exported modules are visible to Swift.
kotlin {
    val exported =
        listOf(
            projects.shared.core.common,
            projects.shared.core.presentation,
            projects.shared.core.telemetry,
            projects.shared.transit.domain,
            projects.shared.favourites.domain,
            projects.shared.feature.lines,
            projects.shared.feature.stops,
            projects.shared.feature.nearme,
            projects.shared.feature.planner,
            projects.shared.feature.settings,
        )
    val xcFramework = XCFramework("Shared")
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        // SQLDelight's native driver calls the system SQLite, which SQLiter's cinterop does not link (ADR-0013). This
        // covers the test binaries; for the static framework, the app links it through LogroBusUI's Package.swift.
        target.binaries.all { linkerOpts("-lsqlite3") }
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            exported.forEach { export(it) }
            xcFramework.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            exported.forEach { api(it) }
            implementation(projects.shared.transit.data)
            implementation(projects.shared.favourites.data)
            implementation(projects.shared.core.config)
            implementation(projects.shared.core.network)
            implementation(projects.shared.core.database)
            implementation(projects.shared.core.location)
            implementation(projects.shared.core.polling)
            implementation(libs.koin.core)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.robolectric)
        }
    }
}
