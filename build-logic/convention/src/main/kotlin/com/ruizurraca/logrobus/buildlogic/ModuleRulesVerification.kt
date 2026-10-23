package com.ruizurraca.logrobus.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency

/**
 * Checks every production dependency against [ModuleRules] when it is declared, so dependencies
 * added late (for example in `afterEvaluate`) or in any source set or bucket are covered too.
 */
internal fun Project.verifyModuleRules() {
    val modulePath = path
    configurations.matching { isProductionDependencyBucket(it.name) }.configureEach {
        val bucket = name
        dependencies.configureEach {
            val violations = when (this) {
                is ProjectDependency ->
                    if (path == modulePath) emptyList() else ModuleRules.violations(modulePath, listOf(path))
                is ExternalModuleDependency ->
                    ModuleRules.violations(modulePath, emptyList(), listOf("$group:$name"))
                else -> emptyList()
            }
            if (violations.isNotEmpty()) {
                throw GradleException(
                    "Module dependency rules violated in '$bucket':\n" + violations.joinToString("\n") { " - $it" },
                )
            }
        }
    }
}
