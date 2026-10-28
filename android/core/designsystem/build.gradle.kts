plugins {
    id("logrobus.android.library")
    alias(libs.plugins.roborazzi)
}

// Reference images, checked by verifyRoborazziDebug and refreshed by recordRoborazziDebug (testing_strategy.md §4.6).
roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

dependencies {
    api(libs.compose.material3)
    implementation(libs.compose.ui)

    testImplementation(libs.junit4)
    // contrastRatio, to check the placeholder palette (NFR-A11Y-06).
    testImplementation(projects.shared.core.common)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.compose.ui.test.junit4)
    // Compose UI Test brings Espresso 3.5.0, which fails on API 37 (InputManager.getInstance is gone).
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    debugImplementation(libs.compose.ui.test.manifest)
}
