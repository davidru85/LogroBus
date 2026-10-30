plugins {
    id("logrobus.android.library")
    // NavKeys are @Serializable, so the back stacks survive process death (ADR-0005).
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // NavKey is part of this module's API.
    api(libs.androidx.navigation3.runtime)
    // LazyListState, which a tab root scrolls to its top.
    api(libs.compose.foundation)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.compose.ui.test.junit4)
    // Compose UI Test brings Espresso 3.5.0, which fails on API 37 (InputManager.getInstance is gone).
    testImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.compose.ui.test.manifest)
}
