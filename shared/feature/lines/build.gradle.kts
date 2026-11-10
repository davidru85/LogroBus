plugins {
    id("logrobus.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.transit.domain)
            api(projects.shared.favourites.domain)
            api(projects.shared.core.presentation)
            // LB-105 (FR-LINE-05, FR-SET-01): the ETA format the user chose decides how each stop's next call shows.
            implementation(projects.shared.core.preferences)
            implementation(projects.shared.core.polling)
            implementation(projects.shared.core.telemetry)
            implementation(projects.shared.core.config)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
