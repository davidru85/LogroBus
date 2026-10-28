plugins {
    id("logrobus.android.application")
}

android {
    defaultConfig {
        // OQ-14: the store identifier, shared with iOS. It cannot change after the first release.
        applicationId = "com.ruizurraca.logrobus"
    }
    // VERSION_NAME and DEBUG reach the shared graph from here (LB-010).
    buildFeatures.buildConfig = true
}

dependencies {
    implementation(projects.shared.umbrella)
    implementation(projects.android.feature.settings)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)

    testImplementation(libs.junit4)
    // To read and stop the graph that the Application starts (ADR-0014).
    testImplementation(libs.koin.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.compose.ui.test.junit4)
    // Compose UI Test brings Espresso 3.5.0, which fails on API 37 (InputManager.getInstance is gone).
    testImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.compose.ui.test.manifest)
}
