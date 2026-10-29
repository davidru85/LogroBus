plugins {
    id("logrobus.android.library")
    // NavKeys are @Serializable, so the back stacks survive process death (ADR-0005).
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // NavKey is part of this module's API.
    api(libs.androidx.navigation3.runtime)
}
