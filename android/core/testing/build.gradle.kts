plugins {
    id("logrobus.android.library")
}

// LB-406: helpers for the Android UI tests. Modules use it as a test dependency only (ModuleRules).
dependencies {
    // The checks extend Compose UI Test's finders.
    api(libs.compose.ui.test.junit4)
    implementation(libs.compose.ui)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.compose.foundation)
    testImplementation(libs.compose.material3)
    // Compose UI Test brings Espresso 3.5.0, which fails on API 37 (InputManager.getInstance is gone).
    testImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.compose.ui.test.manifest)
}
