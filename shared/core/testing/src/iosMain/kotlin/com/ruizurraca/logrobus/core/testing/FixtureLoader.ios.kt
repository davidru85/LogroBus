package com.ruizurraca.logrobus.core.testing

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.getenv
import platform.posix.memcpy

/**
 * The iOS side of [readFixtureFile] and [fixturesDirectory] (docs/testing_strategy.md §4.1).
 *
 * `iosMain` and not `iosTest`, although only the simulator tests read fixtures: the `expect` lives in `commonMain`, so
 * Kotlin requires its `actual` in every main compilation of the target, and a test source set is compiled after that
 * check. Nothing ships it anyway — `ModuleRules` lets only test configurations depend on this module.
 *
 * The simulator's binary and the test bundle share the host's file system, so a path from the build reads directly;
 * `NSData` decodes nothing and the bytes become text here, which keeps the loader free to hand a body to any decoder.
 */

@OptIn(ExperimentalForeignApi::class)
internal actual fun readFixtureFile(name: String): String {
    val directory = fixturesDirectory()
    val data =
        NSData.dataWithContentsOfFile("$directory/$name")
            ?: error(missingFixtureMessage(name, directory))
    if (data.length == 0UL) return ""
    val text =
        ByteArray(data.length.toInt())
            .usePinned { pinned ->
                memcpy(pinned.addressOf(0), data.bytes, data.length)
                pinned.get()
            }.decodeToString()
    return text
}

/**
 * [FIXTURES_DIRECTORY_VARIABLE] only: there is no `user.dir` to walk up from on iOS, and the test bundle carries no
 * copy of the repository. The build sets it on every `KotlinNativeTest`, so a test run by Gradle always has it; the
 * failure below is what an Xcode run without the scheme's variable gets, and it says which variable to set.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun fixturesDirectory(): String =
    getenv(FIXTURES_DIRECTORY_VARIABLE)?.toKString()
        ?: error(
            "$FIXTURES_DIRECTORY_VARIABLE is not set, so the fixtures directory is unknown " +
                "(docs/testing_strategy.md §4.1).",
        )
