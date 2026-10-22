plugins {
    id("logrobus.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.transit.domain)
            implementation(projects.shared.core.network)
            implementation(projects.shared.core.database)
            implementation(projects.shared.core.config)
        }
    }
}
