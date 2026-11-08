package com.ruizurraca.logrobus.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * `logrobus.kmp.library`: every shared Kotlin Multiplatform module (ADR-0007).
 * Targets Android (host tests enabled), iOS device and iOS simulator on Apple silicon;
 * derives the namespace from the module path; lets host tests run Robolectric; raises the security floors of the
 * build's tools and tests; enforces [ModuleRules]; registers `ciUnitTest`.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")

            extensions.configure<KotlinMultiplatformExtension> {
                jvmToolchain(libs.versionOf("jvm-toolchain").toInt())
                (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
                    namespace = moduleNamespace(path)
                    compileSdk = libs.versionOf("android-compileSdk").toInt()
                    minSdk = libs.versionOf("android-minSdk").toInt()
                    withHostTest { }
                    compilerOptions {
                        jvmTarget.set(JvmTarget.fromTarget(libs.versionOf("android-jvmTarget")))
                    }
                }
                iosArm64()
                iosSimulatorArm64()

                sourceSets.getByName("commonTest").dependencies {
                    implementation(kotlin("test"))
                }
            }

            // Host tests may use Robolectric, as :shared:core:location does.
            tasks.withType(Test::class.java).configureEach { jvmArgs(ROBOLECTRIC_JDK_ACCESS) }

            // Robolectric reads its framework jars from the directory Gradle fills, so it never downloads them: with
            // the guard below installed, a download would fail (LB-003).
            configureRobolectricOffline()

            // LB-003: a host test never resolves the live source (docs/testing_strategy.md §1 rule 2).
            configureTestNetworkGuard { guard ->
                extensions.configure<KotlinMultiplatformExtension> {
                    sourceSets.getByName("androidHostTest").dependencies { implementation(guard) }
                }
            }

            tasks.register("ciUnitTest") {
                group = "verification"
                description = "Runs the module's unit tests on the JVM (Android host tests), as CI does."
                dependsOn("testAndroidHostTest")
            }

            // Common and Android code with type resolution, through the Android compilations; the iOS, Apple and
            // native source sets without it, because detekt has no classpath for them.
            configureDetekt(
                analysisTasks = listOf("detektMainAndroid", "detektHostTestAndroid"),
                alsoRun = ::isUntypedKmpSourceSetTask,
            )
            configureKover()
            raiseSecurityFloors()
            verifyModuleRules()
        }
}
