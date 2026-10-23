package com.ruizurraca.logrobus.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import java.util.Properties

/**
 * `logrobus.android.application`: the Android app shell (`:android:app`). Adds the target SDK and the
 * marketing version from `version.properties` (docs/release_process.md §1) to [configureAndroidModule].
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        val versionFile = isolated.rootProject.projectDirectory.file("version.properties")
        val marketingVersion = providers.fileContents(versionFile).asText.map { text ->
            Properties().apply { load(text.reader()) }.getProperty("VERSION_NAME")
                ?: error("version.properties must define VERSION_NAME (docs/release_process.md §1)")
        }

        extensions.configure<ApplicationExtension> {
            configureAndroidModule(this)
            defaultConfig {
                targetSdk = libs.versionOf("android-targetSdk").toInt()
                // Every build uses 1 until the release automation (M5) supplies GITHUB_RUN_NUMBER plus an offset
                // (docs/release_process.md §1).
                versionCode = 1
                versionName = marketingVersion.get()
            }
        }
    }
}
