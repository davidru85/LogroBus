package com.ruizurraca.logrobus.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * `logrobus.kmp.library`: every shared Kotlin Multiplatform module (ADR-0007).
 * Targets Android (host tests enabled), iOS device and iOS simulator on Apple silicon;
 * derives the namespace from the module path; enforces [ModuleRules]; registers `ciUnitTest`.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        extensions.configure<KotlinMultiplatformExtension> {
            (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
                namespace = moduleNamespace(path)
                compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
                minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()
                withHostTest { }
                compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
            }
            iosArm64()
            iosSimulatorArm64()

            sourceSets.getByName("commonTest").dependencies {
                implementation(kotlin("test"))
            }
        }

        tasks.register("ciUnitTest") {
            group = "verification"
            description = "Runs the module's unit tests on the JVM (Android host tests), as CI does."
            dependsOn("testAndroidHostTest")
        }

        afterEvaluate { verifyModuleRules() }
    }
}

/** `:shared:core:common` becomes `com.ruizurraca.logrobus.core.common`. */
internal fun moduleNamespace(path: String): String =
    (listOf("com", "ruizurraca", "logrobus") + path.removePrefix(":").split(':').drop(1)).joinToString(".")

private fun Project.verifyModuleRules() {
    val productionDependencies = configurations
        .filter { configuration ->
            val name = configuration.name
            (name.endsWith("Api") || name.endsWith("Implementation") || name.endsWith("CompileOnly")) &&
                !name.contains("Test")
        }
        .flatMap { it.dependencies }
    val projectPaths = productionDependencies
        .filterIsInstance<ProjectDependency>()
        .map { it.path }
        .filter { it != path }
    val external = productionDependencies
        .filterIsInstance<ExternalModuleDependency>()
        .map { "${it.group}:${it.name}" }
    val violations = ModuleRules.violations(path, projectPaths.distinct(), external.distinct())
    if (violations.isNotEmpty()) {
        throw GradleException("Module dependency rules violated:\n" + violations.joinToString("\n") { " - $it" })
    }
}
