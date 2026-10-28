package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.presentation.TopLevelDestination
import com.ruizurraca.logrobus.feature.settings.domain.ObserveLastSyncs
import org.koin.mp.KoinPlatform

/**
 * The use cases a Swift ViewModel can ask for (architecture.md §5.2). [SharedDependencies] resolves them from the
 * graph; [SharedDependencies.preview] scripts them for previews and tests.
 */
interface UseCases {
    fun observeLastSyncs(): ObserveLastSyncs
}

/**
 * The only entry point Swift uses to reach shared code (architecture.md §5.2, ADR-0004). It hands out shared values
 * and the use cases; Swift never reaches Koin directly.
 */
object SharedDependencies : UseCases {
    /** FR-NAV-02: the tab the app opens on. */
    val startDestination: TopLevelDestination
        get() = TopLevelDestination.start

    /** Whether the app has started the dependency graph, which the use cases come from (ADR-0014). */
    val isReady: Boolean
        get() = KoinPlatform.getKoinOrNull() != null

    override fun observeLastSyncs(): ObserveLastSyncs = graph().get()

    private fun graph() =
        checkNotNull(KoinPlatform.getKoinOrNull()) {
            "The dependency graph has not started: the app starts it at launch, and previews and tests use preview()"
        }

    /** A new set of scripted use cases, for SwiftUI previews and Swift ViewModel tests. They need no graph. */
    fun preview(): PreviewUseCases = PreviewUseCases()
}
