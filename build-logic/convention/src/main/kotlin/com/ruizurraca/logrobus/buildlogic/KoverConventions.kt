package com.ruizurraca.logrobus.buildlogic

import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** NFR-MNT-01: the minimum line coverage that blocks a pull request (docs/testing_strategy.md §3). */
private const val MIN_LINE_COVERAGE = 80

/**
 * Kover on a shared module (docs/testing_strategy.md §3, coverage gates). Domain and data modules must cover
 * [MIN_LINE_COVERAGE] % of their lines; feature modules must cover that much of their `domain`, `data` and
 * `presentation` packages. Other shared modules are measured but not gated.
 */
internal fun Project.configureKover() {
    pluginManager.apply("org.jetbrains.kotlinx.kover")
    val kind = ModuleKind.of(path)
    val gated = kind is ModuleKind.Domain || kind is ModuleKind.Data || kind is ModuleKind.Feature
    if (!gated) return

    extensions.configure<KoverProjectExtension> {
        reports {
            if (kind is ModuleKind.Feature) {
                val namespace = moduleNamespace(path)
                filters.includes.packages("$namespace.domain", "$namespace.data", "$namespace.presentation")
            }
            verify.rule("NFR-MNT-01: $MIN_LINE_COVERAGE % line coverage") {
                bound {
                    minValue.set(MIN_LINE_COVERAGE)
                    coverageUnits.set(CoverageUnit.LINE)
                }
            }
        }
    }
}
