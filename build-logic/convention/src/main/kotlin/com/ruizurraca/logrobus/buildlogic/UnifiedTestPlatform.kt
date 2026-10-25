package com.ruizurraca.logrobus.buildlogic

import org.gradle.api.Project

/**
 * AGP resolves its Unified Test Platform, the runner for instrumented tests, in configurations named
 * `unified-test-platform-*`. They pull Netty through gRPC.
 */
internal fun isUnifiedTestPlatformConfiguration(name: String): Boolean = name.startsWith("unified-test-platform-")

/**
 * Raises the Netty of AGP's Unified Test Platform to the catalog's `netty` version: AGP 9.3 brings 4.1.93 and 4.1.110,
 * which have GHSA-c4c3-7fpv-j4q5 (critical), and the dependency review fails on it (docs/ci_cd.md §6). Netty never
 * reaches the app. The BOM keeps every Netty module on one version, and as a constraint it never lowers a newer
 * version that AGP brings itself.
 */
internal fun Project.patchUnifiedTestPlatformNetty() {
    val handler = dependencies
    val nettyBom = handler.platform(libs.findLibrary("netty-bom").get())
    // Through the DependencyHandler: added straight to the configuration, the BOM loses its platform attribute and
    // resolves as a plain library, so its constraints do not apply.
    configurations.matching { isUnifiedTestPlatformConfiguration(it.name) }.configureEach {
        handler.addProvider(name, nettyBom)
    }
}
