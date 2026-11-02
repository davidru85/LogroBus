plugins {
    id("logrobus.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.transit.domain)
            api(projects.shared.favourites.domain)
            api(projects.shared.core.presentation)
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
