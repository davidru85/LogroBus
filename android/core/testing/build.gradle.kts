plugins {
    id("logrobus.android.library")
}

// LB-406: helpers for the Android UI tests. Modules use it as a test dependency only (ModuleRules).
dependencies {
    // The checks extend Compose UI Test's finders.
    api(libs.compose.ui.test.junit4)
    // Compose UI Test brings Espresso 3.5.0, which fails on API 37 (InputManager.getInstance is gone): the fixed one
    // travels with the checks.
    api(libs.androidx.test.espresso.core)
    implementation(libs.compose.ui)
    // The checks run under Robolectric, and read its graphics mode.
    implementation(libs.robolectric)

    testImplementation(libs.junit4)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.compose.foundation)
    testImplementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.test.manifest)
}
