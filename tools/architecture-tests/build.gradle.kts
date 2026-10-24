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
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    // Konsist and the file-system checks read the whole repository, so declare it as input.
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
                    "iosApp/Packages/*/Package.swift",
                )
                exclude("**/build/**", ".gradle/**", ".kotlin/**", "build-logic/.gradle/**")
            },
        ).withPathSensitivity(PathSensitivity.RELATIVE)
}

tasks.register("ciUnitTest") {
    group = "verification"
    description = "Runs the architecture tests, as CI does."
    dependsOn(tasks.test)
}
