plugins {
    id("logrobus.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.transit.domain)
            api(projects.shared.favourites.domain)
            api(projects.shared.core.presentation)
            implementation(projects.shared.core.network)
            implementation(projects.shared.core.location)
            implementation(projects.shared.core.telemetry)
            implementation(projects.shared.core.config)
        }
    }
}
