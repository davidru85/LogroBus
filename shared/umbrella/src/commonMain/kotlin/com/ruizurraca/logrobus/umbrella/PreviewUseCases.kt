package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.feature.settings.domain.ObserveLastSyncs
import kotlinx.coroutines.flow.emptyFlow

/**
 * The real use cases over scripted repositories (architecture.md §5.2), so previews show what the shared mappers make
 * of the script. By default every dataset synced at [SAMPLE_SYNC], 10:30 in Madrid.
 */
class PreviewUseCases internal constructor() : UseCases {
    /** How many collections of these use cases are running: a test sees that leaving a screen stops its own. */
    val activeCollections: Int
        get() = -1

    /** Scripts a first load that never ends: the use cases emit nothing. */
    fun loading(): PreviewUseCases = this

    /** Scripts an app that has never synced. */
    fun neverSynced(): PreviewUseCases = this

    override fun observeLastSyncs(): ObserveLastSyncs = ObserveLastSyncs { emptyFlow() }
}
