package com.ruizurraca.logrobus.buildlogic

import org.gradle.api.Project

private const val SWIFT_EXPORT_CLASSPATH = "swiftExportClasspath"

/**
 * The configurations that get the security floors (docs/ci_cd.md §6):
 * - AGP's tools: the runner for instrumented tests (`unified-test-platform-*`) and Android lint (`androidLintTool`);
 * - KGP's Swift export (`swiftExportClasspath`), which no build of ours runs but the dependency graph resolves;
 * - the unit and host test dependencies (`testImplementation`, `androidHostTestImplementation`), where Robolectric
 *   brings Guava.
 *
 * The app's own classpaths never get them: what ships has the catalog's versions.
 */
internal fun getsSecurityFloors(name: String): Boolean =
    name.startsWith("unified-test-platform-") || name in FLOORED_CONFIGURATIONS

/**
 * Whether a floored configuration also gets the BOMs. A BOM is a dependency, and KGP adds the Swift export tools as
 * default dependencies, which apply only while the configuration declares none: a BOM there would remove the tools
 * instead of raising them. The Swift export classpath gets the plain constraints alone.
 */
internal fun getsSecurityFloorBoms(name: String): Boolean = getsSecurityFloors(name) && name != SWIFT_EXPORT_CLASSPATH

private val FLOORED_CONFIGURATIONS =
    setOf("androidLintTool", SWIFT_EXPORT_CLASSPATH, "testImplementation", "androidHostTestImplementation")

/** Libraries with a BOM, which keeps all their modules on one version. */
private val SECURITY_FLOOR_BOMS = listOf("netty-bom", "bouncycastle-bom", "guava-bom")

/** Libraries raised by a plain constraint. */
private val SECURITY_FLOOR_LIBRARIES = listOf("opentelemetry-api", "commons-lang3", "httpclient")

/**
 * Security floors for libraries with known advisories that the build's tools and test dependencies bring: Netty,
 * Bouncy Castle, Guava, OpenTelemetry, Commons Lang and HttpClient. None of them reaches the app. Every floor acts as
 * a constraint: it raises a library only where something else brings it, and never lowers a newer version. The root
 * build script sets the floors of the plugin classpath.
 */
internal fun Project.raiseSecurityFloors() {
    val handler = dependencies
    val boms = SECURITY_FLOOR_BOMS.map { handler.platform(libs.findLibrary(it).get()) }
    val libraries = SECURITY_FLOOR_LIBRARIES.map { libs.findLibrary(it).get() }
    configurations.matching { getsSecurityFloors(it.name) }.configureEach {
        libraries.forEach { handler.constraints.addProvider(name, it) }
        // Through the DependencyHandler: added straight to the configuration, a BOM loses its platform attribute and
        // resolves as a plain library, so its constraints do not apply.
        if (getsSecurityFloorBoms(name)) boms.forEach { handler.addProvider(name, it) }
    }
}
