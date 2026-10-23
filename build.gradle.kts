plugins {
    base
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Root aggregates: `check` and `ciUnitTest` also cover the build-logic included build.
tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":convention:check"))
}

tasks.register("ciUnitTest") {
    group = "verification"
    description = "Runs build-logic unit tests; module-level ciUnitTest tasks run alongside."
    dependsOn(gradle.includedBuild("build-logic").task(":convention:test"))
}
