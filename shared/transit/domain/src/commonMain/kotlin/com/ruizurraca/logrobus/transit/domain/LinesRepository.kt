package com.ruizurraca.logrobus.transit.domain

import com.ruizurraca.logrobus.core.common.AppResult
import kotlinx.coroutines.flow.Flow

/** The cached lines (ADR-0008). Downloading them is the static sync's job ([StaticSyncEngine]). */
interface LinesRepository {
    /**
     * Every cached line, emitted again whenever a sync replaces them; empty before the first sync. A failure to read is
     * a [AppResult.Failure], never an exception (AGENTS.md §5, rule 11).
     */
    fun observeLines(): Flow<AppResult<List<Line>>>
}
