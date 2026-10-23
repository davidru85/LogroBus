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
    private val swiftTarget = Regex("""\.target\(\s*name:\s*"(\w+)"(?:\s*,\s*dependencies:\s*\[([^\]]*)])?""")
    private val jobName = Regex("""^ {4}name:\s*(.*\$\{\{.*)$""", RegexOption.MULTILINE)
    private val matrixModuleItem = Regex("""^\s*-\s*"?(:[\w-]+(?::[\w-]+)+)"?\s*$""", RegexOption.MULTILINE)
    private val jobsHeader = Regex("""^jobs:\s*$""", RegexOption.MULTILINE)
    private val topLevelKey = Regex("""^\S""", RegexOption.MULTILINE)
    private val jobKey = Regex("""^ {2}([\w-]+):\s*$""", RegexOption.MULTILINE)
    private val gateNeeds = Regex("""^ {4}needs:\s*\[?([^\]\n]*)]?\s*$""", RegexOption.MULTILINE)
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

    /** Feature targets of a Swift package manifest that depend on another feature target (ADR-0007, §3.2). */
    fun crossFeatureSwiftDependencies(manifest: String): List<String> =
        swiftTarget.findAll(manifest).flatMap { target ->
            val name = target.groupValues[1]
            quoted.findAll(target.groupValues[2]).map { it.groupValues[1] }
                .filter { name.isSwiftFeature() && it.isSwiftFeature() && it != name }
                .map { "$name -> $it" }
        }.toList()

    /** Job names (not step names) that use `${{ }}` expressions, which GitHub shows unexpanded on skipped jobs. */
    fun jobNamesWithExpressions(workflow: String): List<String> =
        jobName.findAll(workflow).map { it.groupValues[1].trim() }.toList()

    /** Gradle module paths listed as items of a workflow matrix, such as `- ":shared:core:common"`. */
    fun workflowMatrixModules(workflow: String): Set<String> =
        matrixModuleItem.findAll(workflow).map { it.groupValues[1] }.toSet()

    /**
     * Jobs of a GitHub Actions workflow that its [gate] job does not list in `needs`. The gate is the one required
     * check (docs/ci_cd.md §4), so it could pass while one of these jobs fails.
     */
    fun jobsMissingFromGate(workflow: String, gate: String): List<String> {
        val afterHeader = jobsHeader.find(workflow)?.let { workflow.substring(it.range.last + 1) }.orEmpty()
        val jobs = topLevelKey.find(afterHeader)?.let { afterHeader.substring(0, it.range.first) } ?: afterHeader
        val keys = jobKey.findAll(jobs).toList()
        val gateIndex = keys.indexOfFirst { it.groupValues[1] == gate }
        require(gateIndex >= 0) { "The workflow has no '$gate' job" }
        val gateEnd = keys.getOrNull(gateIndex + 1)?.range?.first ?: jobs.length
        val needs = gateNeeds.find(jobs.substring(keys[gateIndex].range.last + 1, gateEnd))?.groupValues?.get(1)
            .orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
        return keys.map { it.groupValues[1] }.filter { it != gate && it !in needs }
    }

    /**
     * `shared/feature/lines/src/...` must use `com.ruizurraca.logrobus.feature.lines`. Android modules keep their
     * platform segment, as in build-logic: `android/feature/lines/src/...` uses
     * `com.ruizurraca.logrobus.android.feature.lines`.
     */
    fun expectedNamespace(relativePath: String): String {
        val pathSegments = relativePath.split('/').takeWhile { it != "src" }
        val moduleSegments = if (pathSegments.first() == "shared") pathSegments.drop(1) else pathSegments
        return (listOf(ROOT_PACKAGE) + moduleSegments).joinToString(".")
    }

    private fun String.isSwiftFeature(): Boolean = startsWith("Feature")

    private fun featureOf(name: String): String? = featurePackage.find(name)?.groupValues?.get(1)
}
