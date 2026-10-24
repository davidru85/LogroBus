package com.ruizurraca.logrobus.buildlogic

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/**
 * detekt (docs/code_style.md §2.1, ADR-0011) with `config/detekt/detekt.yml` on top of the defaults. The
 * `ciDetekt` alias runs the type-resolved [analysisTasks], which rules such as `ForbiddenMethodCall` need, plus
 * every other detekt task whose name [alsoRun] accepts (for source sets detekt can only analyse without types).
 * The module's `check` runs it too.
 */
internal fun Project.configureDetekt(
    analysisTasks: List<String>,
    alsoRun: (taskName: String) -> Boolean = { false },
) {
    pluginManager.apply("dev.detekt")
    extensions.configure<DetektExtension> {
        buildUponDefaultConfig.set(true)
        config.setFrom(isolated.rootProject.projectDirectory.file("config/detekt/detekt.yml"))
    }
    tasks.register("ciDetekt") {
        group = "verification"
        description = "Runs detekt as CI does."
        dependsOn(analysisTasks)
        dependsOn(project.tasks.withType<Detekt>().matching { alsoRun(it.name) })
    }
    tasks.named("check") { dependsOn("ciDetekt") }
}

/**
 * KMP source sets that detekt analyses without type resolution: every one except the common and Android source
 * sets, which the type-resolved Android compilation tasks already cover.
 */
internal fun isUntypedKmpSourceSetTask(taskName: String): Boolean =
    taskName.endsWith("SourceSet") &&
        !taskName.startsWith("detektCommon") &&
        !taskName.startsWith("detektAndroid")
