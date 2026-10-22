package com.ruizurraca.logrobus.buildlogic

private val productionSuffixes = listOf("Api", "Implementation", "CompileOnly", "RuntimeOnly")
private val productionNames = setOf("api", "implementation", "compileOnly", "runtimeOnly")

/** `:shared:core:common` becomes `com.ruizurraca.logrobus.core.common`. */
internal fun moduleNamespace(path: String): String =
    (listOf("com", "ruizurraca", "logrobus") + path.removePrefix(":").split(':').drop(1)).joinToString(".")

/**
 * True for configurations where production dependencies are declared (any source set, any
 * declaration kind), false for test, tooling and resolvable metadata configurations.
 */
internal fun isProductionDependencyBucket(name: String): Boolean {
    if (name.contains("Test") || name.startsWith("test")) return false
    return name in productionNames || productionSuffixes.any { name.endsWith(it) }
}
