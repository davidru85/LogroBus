package com.ruizurraca.logrobus.architecture

import java.io.File

/** The repository root: the nearest ancestor of the working directory with `settings.gradle.kts` and `gradlew`. */
internal val repositoryRoot: File =
    generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").isFile && File(it, "gradlew").isFile }
