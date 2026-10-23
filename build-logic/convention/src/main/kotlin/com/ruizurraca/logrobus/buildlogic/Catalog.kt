package com.ruizurraca.logrobus.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** The `libs` version catalog (`gradle/libs.versions.toml`), the only place where versions are declared. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** Named `versionOf`, not `version`, so the version-literal rule does not mistake a call for a literal. */
internal fun VersionCatalog.versionOf(alias: String): String = findVersion(alias).get().requiredVersion
