plugins {
    id("logrobus.android.library")
}

dependencies {
    api(libs.compose.material3)
    implementation(libs.compose.ui)

    testImplementation(libs.junit4)
    // contrastRatio, to check the placeholder palette (NFR-A11Y-06).
    testImplementation(projects.shared.core.common)
}
