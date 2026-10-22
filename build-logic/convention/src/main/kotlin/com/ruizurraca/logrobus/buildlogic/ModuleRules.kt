package com.ruizurraca.logrobus.buildlogic

/**
 * Allowed dependencies between Gradle modules (ADR-0007, docs/architecture.md §3.2).
 * Callers pass production dependencies only; test configurations are not checked.
 */
object ModuleRules {

    private const val REFERENCE = "ADR-0007, docs/architecture.md §3.2"
    private val platformGroups = listOf("androidx", "com.android", "com.google.android")

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
        val externalViolations = externalDependencies.mapNotNull { coordinate ->
            externalReason(source, coordinate.substringBefore(':'))
                ?.let { "$from -> $coordinate is not allowed: $it ($REFERENCE)" }
        }
        return projectViolations + externalViolations
    }

    private fun reason(from: ModuleKind, to: ModuleKind): String? {
        if (to is ModuleKind.Unknown) return "the target is not a classified module"
        if (to == ModuleKind.Testing) return "production code must not depend on the testing module"
        val allowed = when (from) {
            is ModuleKind.Core -> to is ModuleKind.Core
            ModuleKind.Testing -> to is ModuleKind.Core || to is ModuleKind.Domain
            is ModuleKind.Domain -> to == ModuleKind.Core("common")
            is ModuleKind.Data -> to is ModuleKind.Core || to == ModuleKind.Domain(from.capability)
            is ModuleKind.Feature -> to is ModuleKind.Core || to is ModuleKind.Domain
            ModuleKind.Umbrella -> to.isShared
            is ModuleKind.AndroidCore -> to is ModuleKind.Core || to is ModuleKind.AndroidCore
            is ModuleKind.AndroidFeature -> to.isInwardFrom(from)
            ModuleKind.AndroidApp -> to !is ModuleKind.Tool
            is ModuleKind.Tool -> true
            ModuleKind.Unknown -> false
        }
        return if (allowed) null else "a ${from.label} module may not depend on a ${to.label} module"
    }

    private fun externalReason(from: ModuleKind, group: String): String? = when {
        from is ModuleKind.Domain && platformGroups.any { group.isInGroup(it) } ->
            "domain modules must not use platform libraries"
        group.isInGroup("io.ktor") && !(from == ModuleKind.Core("network") || from.mayUseInfrastructure) ->
            "only the network core, data and feature modules may use Ktor"
        group.isInGroup("app.cash.sqldelight") && !(from == ModuleKind.Core("database") || from.mayUseInfrastructure) ->
            "only the database core, data and feature modules may use SQLDelight"
        else -> null
    }

    private fun String.isInGroup(group: String): Boolean = this == group || startsWith("$group.")

    /** Data modules and the data packages of feature modules talk to the network and the database. */
    private val ModuleKind.mayUseInfrastructure: Boolean
        get() = this is ModuleKind.Data || this is ModuleKind.Feature

    private val ModuleKind.isShared: Boolean
        get() = this is ModuleKind.Core ||
            this is ModuleKind.Domain ||
            this is ModuleKind.Data ||
            this is ModuleKind.Feature

    /** An Android feature may use shared cores, domains, Android cores and only its own shared feature. */
    private fun ModuleKind.isInwardFrom(feature: ModuleKind.AndroidFeature): Boolean =
        this is ModuleKind.Core ||
            this is ModuleKind.Domain ||
            this is ModuleKind.AndroidCore ||
            this == ModuleKind.Feature(feature.name)
}

/** Classification of a Gradle project path into an architectural role. */
internal sealed interface ModuleKind {
    val label: String

    data class Core(val name: String) : ModuleKind { override val label = "shared core" }
    data object Testing : ModuleKind { override val label = "shared testing" }
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
            return when (segments.size) {
                2 -> ofTwo(segments[0], segments[1])
                3 -> ofThree(segments[0], segments[1], segments[2])
                else -> Unknown
            }
        }

        private fun ofTwo(root: String, name: String): ModuleKind = when {
            root == "shared" && name == "umbrella" -> Umbrella
            root == "android" && name == "app" -> AndroidApp
            root == "tools" -> Tool(name)
            else -> Unknown
        }

        private fun ofThree(root: String, group: String, name: String): ModuleKind = when {
            root == "shared" && group == "core" && name == "testing" -> Testing
            root == "shared" && group == "core" -> Core(name)
            root == "shared" && group == "feature" -> Feature(name)
            root == "shared" && group in capabilities && name == "domain" -> Domain(group)
            root == "shared" && group in capabilities && name == "data" -> Data(group)
            root == "android" && group == "core" -> AndroidCore(name)
            root == "android" && group == "feature" -> AndroidFeature(name)
            else -> Unknown
        }
    }
}
