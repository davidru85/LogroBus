plugins {
    id("logrobus.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.common)
        }
        androidMain.dependencies {
            // The Fused Location Provider (architecture.md §5.1).
            implementation(libs.play.services.location)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.robolectric)
        }
    }
}
