package com.ruizurraca.logrobus.buildlogic

import org.gradle.api.Project

/**
 * The configurations in which AGP resolves its own tools: the runner for instrumented tests (`unified-test-platform-*`)
 * and Android lint (`androidLintTool`).
 */
internal fun isAgpToolConfiguration(name: String): Boolean =
    name.startsWith("unified-test-platform-") || name == "androidLintTool"

/**
 * Security floors for libraries that AGP 9.3's tools bring with critical advisories, which the dependency review
 * fails on (docs/ci_cd.md §6): Netty (GHSA-c4c3-7fpv-j4q5) and Bouncy Castle (GHSA-574f-3g2m-x479,
 * GHSA-9pwp-9qqc-pr26). None of them reaches the app. Each BOM keeps its library's modules on one version and, as a
 * constraint, never lowers a newer version that AGP brings itself. The root build script sets the floors of AGP's
 * plugin classpath.
 */
internal fun Project.raiseAgpToolSecurityFloors() {
    val handler = dependencies
    val boms = listOf("netty-bom", "bouncycastle-bom").map { handler.platform(libs.findLibrary(it).get()) }
    // Through the DependencyHandler: added straight to the configuration, a BOM loses its platform attribute and
    // resolves as a plain library, so its constraints do not apply.
    configurations.matching { isAgpToolConfiguration(it.name) }.configureEach {
        boms.forEach { handler.addProvider(name, it) }
    }
}
