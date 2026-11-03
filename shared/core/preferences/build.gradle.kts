plugins {
    id("logrobus.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.common)
            // ADR-0018: Multiplatform Settings, with its flows.
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
        }
        androidMain.dependencies {
            // ADR-0018: DataStore on Android.
            implementation(libs.multiplatform.settings.datastore)
            implementation(libs.androidx.datastore.preferences.core)
        }
        commonTest.dependencies {
            implementation(libs.multiplatform.settings.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
