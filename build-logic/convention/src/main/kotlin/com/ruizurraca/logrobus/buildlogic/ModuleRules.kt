package com.ruizurraca.logrobus.buildlogic

/**
 * Allowed dependencies between Gradle modules (ADR-0007, docs/architecture.md §3.2).
 * Callers pass production dependencies only; test configurations are not checked.
 */
object ModuleRules {

    private const val REFERENCE = "ADR-0007, docs/architecture.md §3.2"
    private val forbiddenInDomain = setOf("io.ktor", "app.cash.sqldelight")

    /**
     * Returns one message per forbidden dependency of the module at [from].
     * [projectDependencies] are Gradle project paths; [externalDependencies] are "group:name" coordinates.
     */
    fun violations(
        from: String,
        projectDependencies: Collection<String>,
        externalDependencies: Collection<String> = emptyList(),
    ): List<String> {
        val source = ModuleKind.of(from)
        if (source is ModuleKind.Unknown) {
            return listOf("$from is not a classified module; add it to ModuleRules and the architecture ($REFERENCE)")
        }
        val projectViolations = projectDependencies.mapNotNull { to ->
            reason(source, ModuleKind.of(to))?.let { "$from -> $to is not allowed: $it ($REFERENCE)" }
        }
        val externalViolations = if (source is ModuleKind.Domain) {
            externalDependencies
                .filter { coordinate -> coordinate.substringBefore(':') in forbiddenInDomain }
                .map { "$from -> $it is not allowed: domain modules must not use networking or persistence libraries ($REFERENCE)" }
        } else {
            emptyList()
        }
        return projectViolations + externalViolations
    }

    private fun reason(from: ModuleKind, to: ModuleKind): String? {
        if (to is ModuleKind.Unknown) return "the target is not a classified module"
        if (to == ModuleKind.Core("testing") && from != ModuleKind.Core("testing")) {
            return "production code must not depend on the testing module"
        }
        val allowed = when (from) {
            is ModuleKind.Core -> to is ModuleKind.Core
            is ModuleKind.Domain -> to == ModuleKind.Core("common")
            is ModuleKind.Data -> to is ModuleKind.Core || to == ModuleKind.Domain(from.capability)
            is ModuleKind.Feature -> to is ModuleKind.Core || to is ModuleKind.Domain
            ModuleKind.Umbrella -> to is ModuleKind.Core || to is ModuleKind.Domain || to is ModuleKind.Data || to is ModuleKind.Feature
            is ModuleKind.AndroidCore -> to is ModuleKind.Core || to is ModuleKind.AndroidCore
            is ModuleKind.AndroidFeature ->
                to is ModuleKind.Core || to is ModuleKind.Domain || to is ModuleKind.AndroidCore || to == ModuleKind.Feature(from.name)
            ModuleKind.AndroidApp -> to !is ModuleKind.Tool
            is ModuleKind.Tool -> true
            ModuleKind.Unknown -> false
        }
        return if (allowed) null else "a ${from.label} module may not depend on a ${to.label} module"
    }
}

/** Classification of a Gradle project path into an architectural role. */
internal sealed interface ModuleKind {
    val label: String

    data class Core(val name: String) : ModuleKind { override val label = "shared core" }
    data class Domain(val capability: String) : ModuleKind { override val label = "domain ($capability)" }
    data class Data(val capability: String) : ModuleKind { override val label = "data ($capability)" }
    data class Feature(val name: String) : ModuleKind { override val label = "shared feature ($name)" }
    data object Umbrella : ModuleKind { override val label = "umbrella" }
    data class AndroidCore(val name: String) : ModuleKind { override val label = "Android core" }
    data class AndroidFeature(val name: String) : ModuleKind { override val label = "Android feature ($name)" }
    data object AndroidApp : ModuleKind { override val label = "Android app" }
    data class Tool(val name: String) : ModuleKind { override val label = "tool" }
    data object Unknown : ModuleKind { override val label = "unclassified" }

    companion object {
        private val capabilities = setOf("transit", "favourites")

        fun of(path: String): ModuleKind {
            val segments = path.removePrefix(":").split(':')
            return when {
                segments.size == 3 && segments[0] == "shared" && segments[1] == "core" -> Core(segments[2])
                segments.size == 3 && segments[0] == "shared" && segments[1] == "feature" -> Feature(segments[2])
                segments.size == 3 && segments[0] == "shared" && segments[1] in capabilities && segments[2] == "domain" -> Domain(segments[1])
                segments.size == 3 && segments[0] == "shared" && segments[1] in capabilities && segments[2] == "data" -> Data(segments[1])
                segments == listOf("shared", "umbrella") -> Umbrella
                segments == listOf("android", "app") -> AndroidApp
                segments.size == 3 && segments[0] == "android" && segments[1] == "core" -> AndroidCore(segments[2])
                segments.size == 3 && segments[0] == "android" && segments[1] == "feature" -> AndroidFeature(segments[2])
                segments.size == 2 && segments[0] == "tools" -> Tool(segments[1])
                else -> Unknown
            }
        }
    }
}
