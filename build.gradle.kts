plugins {
    base
    `jvm-toolchains`
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
    dependsOn("ktlintCheck")
}

// ktlint (docs/code_style.md §2.1): the CLI from the catalog, the ktlint_official style from .editorconfig,
// over every Kotlin source and build script in the repository, build-logic included.
val ktlint: Configuration =
    configurations.create("ktlint") {
        // ktlint-cli publishes a plain and a shadowed (single jar) variant; use the shadowed one.
        attributes { attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED)) }
    }

dependencies {
    ktlint(libs.ktlint.cli)
}

val ktlintJvm: JavaLanguageVersion =
    JavaLanguageVersion.of(
        libs.versions.jvm.toolchain
            .get()
            .toInt(),
    )
val ktlintSources = listOf("**/src/**/*.kt", "**/*.gradle.kts", "!**/build/**")

tasks.register<JavaExec>("ktlintCheck") {
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(ktlintJvm) })
    group = "verification"
    description = "Checks the Kotlin style with ktlint."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args(ktlintSources)
}

tasks.register<JavaExec>("ktlintFormat") {
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(ktlintJvm) })
    group = "formatting"
    description = "Fixes the Kotlin style with ktlint, where it can."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args(listOf("--format") + ktlintSources)
}

tasks.register("ciUnitTest") {
    group = "verification"
    description = "Runs build-logic unit tests; module-level ciUnitTest tasks run alongside."
    dependsOn(gradle.includedBuild("build-logic").task(":convention:test"))
}
