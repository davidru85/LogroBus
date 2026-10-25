package com.ruizurraca.logrobus.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Settings shared by the Android application and library conventions: SDK levels and JVM target from the
 * catalog, Compose with its BOM, Robolectric-ready unit tests, the `ciUnitTest` alias, the security floors of AGP's
 * tools and [ModuleRules].
 */
internal fun Project.configureAndroidModule(android: CommonExtension) {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    val jvmTarget = libs.versionOf("android-jvmTarget")

    android.apply {
        namespace = moduleNamespace(path)
        compileSdk = libs.versionOf("android-compileSdk").toInt()
        defaultConfig.minSdk = libs.versionOf("android-minSdk").toInt()
        compileOptions.sourceCompatibility = JavaVersion.toVersion(jvmTarget)
        compileOptions.targetCompatibility = JavaVersion.toVersion(jvmTarget)
        buildFeatures.compose = true
        // Robolectric reads the merged resources and manifest.
        testOptions.unitTests.isIncludeAndroidResources = true
        // Robolectric's Android 17 (API 37) sandbox reaches jdk.internal.access, which JDK 21 does not export.
        testOptions.unitTests.all { it.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED") }
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        jvmToolchain(libs.versionOf("jvm-toolchain").toInt())
        compilerOptions.jvmTarget.set(JvmTarget.fromTarget(jvmTarget))
    }

    dependencies {
        val composeBom = platform(libs.findLibrary("compose-bom").get())
        add("implementation", composeBom)
        add("testImplementation", composeBom)
    }

    tasks.register("ciUnitTest") {
        group = "verification"
        description = "Runs the module's unit tests on the JVM (debug variant, Robolectric), as CI does."
        dependsOn("testDebugUnitTest")
    }

    raiseAgpToolSecurityFloors()
    configureDetekt(listOf("detektMain", "detektTest"))
    verifyModuleRules()
}
