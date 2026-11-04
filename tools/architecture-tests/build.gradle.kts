plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(
        libs.versions.jvm.toolchain
            .get()
            .toInt(),
    )
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.konsist)
    testImplementation(libs.kotlinx.serialization.json)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    // Konsist and the file-system checks read the whole repository, so declare it as input: the Swift sources and
    // string catalogues too, for the iOS rules (Package.swift among them).
    inputs
        .files(
            fileTree(isolated.rootProject.projectDirectory) {
                include(
                    "**/*.kt",
                    "**/*.gradle.kts",
                    ".github/workflows/*.yml",
                    "scripts/ci/*.sh",
                    ".gitignore",
                    "iosApp/project.yml",
                    "iosApp/**/*.swift",
                    "iosApp/**/*.xcstrings",
                )
                exclude("**/build/**", "**/.build/**", ".gradle/**", ".kotlin/**", "build-logic/.gradle/**")
            },
        ).withPathSensitivity(PathSensitivity.RELATIVE)
}

tasks.register("ciUnitTest") {
    group = "verification"
    description = "Runs the architecture tests, as CI does."
    dependsOn(tasks.test)
}
