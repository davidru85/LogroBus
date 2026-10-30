package com.ruizurraca.logrobus.architecture

/** Pure rules behind ArchitectureTest (ADR-0007, docs/architecture.md §3), unit-tested in ArchitectureRulesTest. */
object ArchitectureRules {
    private const val ROOT_PACKAGE = "com.ruizurraca.logrobus"
    private const val UMBRELLA_PACKAGE = "$ROOT_PACKAGE.umbrella"
    private const val DESIGN_SYSTEM_PACKAGE = "$ROOT_PACKAGE.android.core.designsystem"
    private const val EXPRESSIVE_API = "ExperimentalMaterial3ExpressiveApi"

    private val databaseExtensions = setOf("db", "sqlite", "sqlite3", "realm")

    private val seedName = Regex("^seed(?![a-z])")

    private val catalogueJson = Regex("^(lines|stops)(?![a-z]).*\\.json$")

    private val includeCall = Regex("""\binclude\s*\(([^)]*)\)""")
    private val quoted = Regex(""""([^"]+)"""")
    private val versionPatterns =
        listOf(
            Regex(""""[\w.-]+:[\w.-]+:[^"\s]+""""),
            Regex("""\bversion\s*[=(]?\s*""""),
            Regex("""\b(strictly|require|prefer)\s*\(\s*""""),
        )
    private val swiftTarget = Regex("""\.target\(\s*name:\s*"(\w+)"(?:\s*,\s*dependencies:\s*\[([^\]]*)])?""")
    private val swiftPackageExact =
        Regex("""\.package\(\s*url:\s*"[^"]*/([\w.-]+?)(?:\.git)?"\s*,\s*exact:\s*"([^"]+)"""")
    private val xcodegenPackageExact = Regex("""url:\s*\S*/([\w.-]+?)(?:\.git)?\s*\n\s*exactVersion:\s*"?([\w.-]+)"?""")
    private val jobName = Regex("""^ {4}name:\s*(.*\$\{\{.*)$""", RegexOption.MULTILINE)
    private val matrixModuleItem = Regex("""^\s*-\s*"?(:[\w-]+(?::[\w-]+)+)"?\s*$""", RegexOption.MULTILINE)
    private val jobsHeader = Regex("""^jobs:\s*$""", RegexOption.MULTILINE)
    private val topLevelKey = Regex("""^\S""", RegexOption.MULTILINE)
    private val jobKey = Regex("""^ {2}([\w-]+):\s*$""", RegexOption.MULTILINE)
    private val jobNeedsLine = Regex("""^ {4}needs:\s*\[?([^\]\n]*)]?\s*$""", RegexOption.MULTILINE)
    private val featurePackage = Regex("""^${Regex.escape(ROOT_PACKAGE)}\.feature\.(\w+)""")
    private val dataPackage = Regex("""^${Regex.escape(ROOT_PACKAGE)}\..*\.data(\.|$)""")
    private val infrastructure = listOf("io.ktor.", "app.cash.sqldelight.")
    private val telemetryValueTypes = setOf("Int", "Long", "Boolean")
    private val swiftPackageUrl = Regex("""\.package\(\s*url:\s*"([^"]+)"""")
    private val vendorSwiftOwners =
        setOf(
            "firebase",
            "getsentry",
            "bugsnag",
            "datadog",
            "amplitude",
            "mixpanel",
            "posthog",
            "segmentio",
            "newrelic",
            "embrace-io",
            "instabug",
            "appsflyersdk",
            "adjust",
            "googleanalytics",
        )
    private val platform =
        listOf(
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
        includeCall
            .findAll(settings)
            .flatMap { call -> quoted.findAll(call.groupValues[1]).map { it.groupValues[1] } }
            .toSet()

    /** Version literals in a build script or build-logic source: coordinates, `version`, rich versions. */
    fun versionLiterals(source: String): List<String> =
        versionPatterns.flatMap { pattern -> pattern.findAll(source).map { it.value }.toList() }

    /** Imports of another feature. Feature code may import itself; only the umbrella may import any feature. */
    fun crossFeatureImports(
        packageName: String?,
        imports: List<String>,
    ): List<String> {
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

    /**
     * Properties of analytics events and crash breadcrumbs that are not ids, counts, flags or telemetry enums, as
     * `Type.property: DeclaredType`. Allowing only those types keeps out free text and coordinates, whatever type they
     * come in (ADR-0009, NFR-OBS-03, NFR-PRIV-02). [telemetryEnums] are the simple names of the telemetry enums.
     */
    fun forbiddenTelemetryProperties(
        properties: Map<String, String>,
        telemetryEnums: Set<String>,
    ): List<String> {
        val allowed = telemetryValueTypes + telemetryEnums
        return properties
            .filterValues { type -> type.trim().removeSuffix("?").substringAfterLast('.') !in allowed }
            .map { (name, type) -> "$name: $type" }
    }

    /** Whether a file outside the Android design system uses Material 3's experimental expressive APIs (ADR-0005). */
    fun usesExpressiveApiOutsideDesignSystem(
        packageName: String?,
        source: String,
    ): Boolean {
        val own = packageName.orEmpty()
        val inDesignSystem = own == DESIGN_SYSTEM_PACKAGE || own.startsWith("$DESIGN_SYSTEM_PACKAGE.")
        return !inDesignSystem && EXPRESSIVE_API in source
    }

    /** The dependencies a Swift package manifest declares for [target], as listed in its `.target(...)` line. */
    fun swiftTargetDependencies(
        manifest: String,
        target: String,
    ): List<String> {
        val dependencies =
            swiftTarget
                .findAll(
                    manifest,
                ).firstOrNull { it.groupValues[1] == target }
                ?.groupValues
                ?.get(2)
        return quoted.findAll(dependencies.orEmpty()).map { it.groupValues[1] }.toList()
    }

    /**
     * Remote Swift packages that the package manifest and the XcodeGen spec pin to different exact versions. Two exact
     * requirements for one package fail resolution, so they must move together.
     */
    fun swiftPackageVersionMismatches(
        manifest: String,
        projectSpec: String,
    ): List<String> {
        val inManifest = swiftPackageExact.findAll(manifest).associate { it.groupValues[1] to it.groupValues[2] }
        val inProject = xcodegenPackageExact.findAll(projectSpec).associate { it.groupValues[1] to it.groupValues[2] }
        return inManifest.keys
            .filter { it in inProject && inManifest[it] != inProject[it] }
            .map { "$it: ${inManifest[it]} in Package.swift, ${inProject[it]} in project.yml" }
    }

    /**
     * The analytics and crash-reporting packages a Swift manifest declares, by URL, recognised by the GitHub owner of
     * their repository (ADR-0009).
     */
    fun vendorTelemetryPackages(manifest: String): List<String> =
        swiftPackageUrl
            .findAll(manifest)
            .map { it.groupValues[1] }
            .filter { url -> url.substringAfter("github.com/").substringBefore('/').lowercase() in vendorSwiftOwners }
            .toList()

    /**
     * Whether the file at [relativePath] looks like bundled seed data: a database file, a file named "seed" as a word
     * ("seed.json", "Seed-lines.json", not "seedling.png"), or a lines or stops JSON file in an assets or resources
     * folder outside the tests. The MVP downloads lines and stops on first use and bundles none (ADR-0008); a seed
     * bundle is v1.0's offline mode.
     */
    fun isSeedData(relativePath: String): Boolean {
        val folders = relativePath.lowercase().split('/').dropLast(1)
        val name = relativePath.substringAfterLast('/').lowercase()
        val bundled = folders.any { it == "assets" || it == "resources" } && folders.none { it.contains("test") }
        return name.substringAfterLast('.', "") in databaseExtensions ||
            seedName.containsMatchIn(name) ||
            (bundled && catalogueJson.matches(name))
    }

    /** Feature targets of a Swift package manifest that depend on another feature target (ADR-0007, §3.2). */
    fun crossFeatureSwiftDependencies(manifest: String): List<String> =
        swiftTarget
            .findAll(manifest)
            .flatMap { target ->
                val name = target.groupValues[1]
                quoted
                    .findAll(target.groupValues[2])
                    .map { it.groupValues[1] }
                    .filter { name.isSwiftFeature() && it.isSwiftFeature() && it != name }
                    .map { "$name -> $it" }
            }.toList()

    /** Job names (not step names) that use `${{ }}` expressions, which GitHub shows unexpanded on skipped jobs. */
    fun jobNamesWithExpressions(workflow: String): List<String> =
        jobName.findAll(workflow).map { it.groupValues[1].trim() }.toList()

    /** Gradle module paths listed as items of a workflow matrix, such as `- ":shared:core:common"`. */
    fun workflowMatrixModules(workflow: String): Set<String> =
        matrixModuleItem.findAll(workflow).map { it.groupValues[1] }.toSet()

    /** Gradle module paths listed in [job]'s matrix only, not in the other jobs of the workflow. */
    fun jobMatrixModules(
        workflow: String,
        job: String,
    ): Set<String> = workflowMatrixModules(jobBodies(workflow)[job].orEmpty())

    /**
     * Jobs of a GitHub Actions workflow that its [gate] job does not list in `needs`. The gate is the one required
     * check (docs/ci_cd.md §4), so it could pass while one of these jobs fails.
     */
    fun jobsMissingFromGate(
        workflow: String,
        gate: String,
    ): List<String> {
        val needs = jobNeeds(workflow, gate)
        return jobBodies(workflow).keys.filter { it != gate && it !in needs }
    }

    /** The jobs that [job] lists in `needs`, written either as `needs: a` or as `needs: [a, b]`. */
    fun jobNeeds(
        workflow: String,
        job: String,
    ): Set<String> {
        val body = jobBodies(workflow)[job]
        require(body != null) { "The workflow has no '$job' job" }
        return jobNeedsLine
            .find(body)
            ?.groupValues
            ?.get(1)
            .orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    /** Each job of a workflow, by key, mapped to the text of its definition. */
    private fun jobBodies(workflow: String): Map<String, String> {
        val afterHeader = jobsHeader.find(workflow)?.let { workflow.substring(it.range.last + 1) }.orEmpty()
        val jobs = topLevelKey.find(afterHeader)?.let { afterHeader.substring(0, it.range.first) } ?: afterHeader
        val keys = jobKey.findAll(jobs).toList()
        return keys
            .mapIndexed { index, key ->
                val end = keys.getOrNull(index + 1)?.range?.first ?: jobs.length
                key.groupValues[1] to jobs.substring(key.range.last + 1, end)
            }.toMap()
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
