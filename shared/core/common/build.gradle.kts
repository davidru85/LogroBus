plugins {
    id("logrobus.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            // TimeZone is part of this module's API: madridTimeZone (ADR-0015).
            api(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            // Dispatchers.Main on Android.
            implementation(libs.kotlinx.coroutines.android)
            // ProcessLifecycleOwner, behind AppLifecycle.
            implementation(libs.androidx.lifecycle.process)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
