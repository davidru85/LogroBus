plugins {
    id("logrobus.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.transit.domain)
            api(projects.shared.favourites.domain)
            api(projects.shared.core.presentation)
            // LB-205 (FR-STOP-05, FR-SET-01): the user's ETA format decides how each arrival shows, and an expired list
            // replaces every ETA with a clock time.
            implementation(projects.shared.core.preferences)
            implementation(projects.shared.core.polling)
            implementation(projects.shared.core.telemetry)
            // `api`: `NightServiceText` returns a `LocalizedText` (LB-206), which a screen must be able to name.
            api(projects.shared.core.config)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
