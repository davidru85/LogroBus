package com.ruizurraca.logrobus.buildlogic

private val productionSuffixes = listOf("Api", "Implementation", "CompileOnly", "RuntimeOnly")
private val productionNames = setOf("api", "implementation", "compileOnly", "runtimeOnly")

/**
 * `:shared:core:common` becomes `com.ruizurraca.logrobus.core.common`. Android modules keep their platform
 * segment (`:android:feature:lines` becomes `com.ruizurraca.logrobus.android.feature.lines`), so they never
 * clash with the Android target of the shared module of the same name.
 */
internal fun moduleNamespace(path: String): String {
    val segments = path.removePrefix(":").split(':')
    val relevant = if (segments.first() == "shared") segments.drop(1) else segments
    return (listOf("com", "ruizurraca", "logrobus") + relevant).joinToString(".")
}

/**
 * True for configurations where production dependencies are declared (any source set, any
 * declaration kind), false for test, tooling and resolvable metadata configurations.
 */
internal fun isProductionDependencyBucket(name: String): Boolean {
    if (name.contains("Test") || name.startsWith("test")) return false
    return name in productionNames || productionSuffixes.any { name.endsWith(it) }
}
