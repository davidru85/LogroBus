package com.ruizurraca.logrobus.buildlogic

import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * detekt (docs/code_style.md §2.1, ADR-0011) with `config/detekt/detekt.yml` on top of the defaults. The
 * `ciDetekt` alias runs the given [tasks]: the type-resolved ones where detekt offers them, which rules such as
 * `ForbiddenMethodCall` need. The module's `check` runs it too.
 */
internal fun Project.configureDetekt(tasks: List<String>) {
    pluginManager.apply("dev.detekt")
    extensions.configure<DetektExtension> {
        buildUponDefaultConfig.set(true)
        config.setFrom(isolated.rootProject.projectDirectory.file("config/detekt/detekt.yml"))
    }
    this.tasks.register("ciDetekt") {
        group = "verification"
        description = "Runs detekt as CI does."
        dependsOn(tasks)
    }
    this.tasks.named("check") { dependsOn("ciDetekt") }
}
