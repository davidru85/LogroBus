plugins {
    id("logrobus.android.library")
}

dependencies {
    implementation(projects.shared.feature.settings)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    // koinViewModel() and the ViewModel DSL (ADR-0014). The module it exposes is a Koin type.
    api(libs.koin.core)
    implementation(libs.koin.androidx.compose)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.compose.ui.test.junit4)
    // Compose UI Test brings Espresso 3.5.0, which fails on API 37 (InputManager.getInstance is gone).
    testImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.compose.ui.test.manifest)
}
