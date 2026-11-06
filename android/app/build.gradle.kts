plugins {
    id("logrobus.android.application")
    alias(libs.plugins.roborazzi)
}

// Reference images, checked by verifyRoborazziDebug and refreshed by recordRoborazziDebug (testing_strategy.md §4.6).
roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

android {
    defaultConfig {
        // OQ-14: the store identifier, shared with iOS. It cannot change after the first release.
        applicationId = "com.ruizurraca.logrobus"
    }
    // VERSION_NAME and DEBUG reach the shared graph from here (LB-010).
    buildFeatures.buildConfig = true
    // ADR-0020: the app's language is chosen in the app, so every language's strings must be installed. Play would
    // otherwise install only the device's languages from the bundle.
    bundle {
        language {
            enableSplit = false
        }
    }
}

dependencies {
    implementation(projects.shared.umbrella)
    implementation(projects.android.core.designsystem)
    implementation(projects.android.core.navigation)
    implementation(projects.android.feature.lines)
    implementation(projects.android.feature.nearme)
    implementation(projects.android.feature.planner)
    implementation(projects.android.feature.settings)
    implementation(libs.androidx.activity.compose)
    // The app's language and theme, chosen in Settings (ADR-0020).
    implementation(libs.androidx.appcompat)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.adaptive.navigation.suite)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    // KoinApplicationPreview: the shell's preview gives the Lines root its ViewModel (ADR-0014).
    implementation(libs.koin.androidx.compose)

    testImplementation(projects.android.core.testing)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    // To read and stop the graph that the Application starts (ADR-0014).
    testImplementation(libs.koin.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.compose.ui.test.junit4)
    // Compose UI Test brings Espresso 3.5.0, which fails on API 37 (InputManager.getInstance is gone).
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
