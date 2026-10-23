package com.ruizurraca.logrobus.architecture

/** Pure rules behind ArchitectureTest (ADR-0007, docs/architecture.md §3), unit-tested in ArchitectureRulesTest. */
object ArchitectureRules {

    private const val ROOT_PACKAGE = "com.ruizurraca.logrobus"
    private const val UMBRELLA_PACKAGE = "$ROOT_PACKAGE.umbrella"

    private val includeCall = Regex("""\binclude\s*\(([^)]*)\)""")
    private val quoted = Regex(""""([^"]+)"""")
    private val versionPatterns = listOf(
        Regex(""""[\w.-]+:[\w.-]+:[^"\s]+""""),
        Regex("""\bversion\s*[=(]?\s*""""),
        Regex("""\b(strictly|require|prefer)\s*\(\s*""""),
    )
    private val matrixModuleItem = Regex("""^\s*-\s*"?(:[\w-]+(?::[\w-]+)+)"?\s*$""", RegexOption.MULTILINE)
    private val featurePackage = Regex("""^${Regex.escape(ROOT_PACKAGE)}\.feature\.(\w+)""")
    private val dataPackage = Regex("""^${Regex.escape(ROOT_PACKAGE)}\..*\.data(\.|$)""")
    private val infrastructure = listOf("io.ktor.", "app.cash.sqldelight.")
    private val platform = listOf(
        "java.",
        "javax.",
        "dalvik.",
        "android.",
        "androidx.",
        "platform.",
        "kotlinx.cinterop.",
    )

    /** Every module path passed to `include(...)`, including multi-argument and multi-line calls. */
    fun includedModules(settings: String): Set<String> =
        includeCall.findAll(settings)
            .flatMap { call -> quoted.findAll(call.groupValues[1]).map { it.groupValues[1] } }
            .toSet()

    /** Version literals in a build script or build-logic source: coordinates, `version`, rich versions. */
    fun versionLiterals(source: String): List<String> =
        versionPatterns.flatMap { pattern -> pattern.findAll(source).map { it.value }.toList() }

    /** Imports of another feature. Feature code may import itself; only the umbrella may import any feature. */
    fun crossFeatureImports(packageName: String?, imports: List<String>): List<String> {
        val own = packageName.orEmpty()
        if (own == UMBRELLA_PACKAGE || own.startsWith("$UMBRELLA_PACKAGE.")) return emptyList()
        val ownFeature = featureOf(own)
        return imports.filter { imported -> featureOf(imported)?.let { it != ownFeature } == true }
    }

    /** Domain code may not import JVM or platform APIs, interop, networking, persistence or data packages. */
    fun forbiddenDomainImports(imports: List<String>): List<String> =
        imports.filter { imported ->
            (platform + infrastructure).any(imported::startsWith) || dataPackage.containsMatchIn(imported)
        }

    /** Presentation code may not import networking, persistence or data packages. */
    fun forbiddenPresentationImports(imports: List<String>): List<String> =
        imports.filter { imported -> infrastructure.any(imported::startsWith) || dataPackage.containsMatchIn(imported) }

    /** Gradle module paths listed as items of a workflow matrix, such as `- ":shared:core:common"`. */
    fun workflowMatrixModules(workflow: String): Set<String> =
        matrixModuleItem.findAll(workflow).map { it.groupValues[1] }.toSet()

    /** `shared/feature/lines/src/...` must use the `com.ruizurraca.logrobus.feature.lines` namespace. */
    fun expectedNamespace(relativePath: String): String {
        val moduleSegments = relativePath.split('/').drop(1).takeWhile { it != "src" }
        return (listOf(ROOT_PACKAGE) + moduleSegments).joinToString(".")
    }

    private fun featureOf(name: String): String? = featurePackage.find(name)?.groupValues?.get(1)
}
