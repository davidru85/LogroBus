package com.ruizurraca.logrobus.buildlogic

import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** NFR-MNT-01: the minimum line coverage that blocks a pull request (docs/testing_strategy.md §3). */
private const val MIN_LINE_COVERAGE = 80

/** What the coverage gate of one module covers. */
internal sealed interface CoverageGate {
    /** Every class of the module. */
    data object WholeModule : CoverageGate

    /** Only the classes in these packages and their subpackages. */
    data class Packages(
        val packages: List<String>,
    ) : CoverageGate
}

/**
 * The coverage gate for the module at [path], or null when it is measured but not gated
 * (docs/testing_strategy.md §3): domain and data modules and the shared presentation mappers whole, and feature
 * modules on their `domain`, `data` and `presentation` packages.
 */
internal fun coverageGate(path: String): CoverageGate? =
    when (ModuleKind.of(path)) {
        is ModuleKind.Domain, is ModuleKind.Data -> {
            CoverageGate.WholeModule
        }

        ModuleKind.Core("presentation") -> {
            CoverageGate.WholeModule
        }

        is ModuleKind.Feature -> {
            val namespace = moduleNamespace(path)
            CoverageGate.Packages(listOf("domain", "data", "presentation").map { "$namespace.$it" })
        }

        else -> {
            null
        }
    }

/** Kover on a shared module, with the [coverageGate] of its path as a [MIN_LINE_COVERAGE] % line rule. */
internal fun Project.configureKover() {
    pluginManager.apply("org.jetbrains.kotlinx.kover")
    val gate = coverageGate(path) ?: return

    extensions.configure<KoverProjectExtension> {
        reports {
            if (gate is CoverageGate.Packages) {
                filters.includes.packages(gate.packages)
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
