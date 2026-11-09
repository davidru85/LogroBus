package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.color.onColorFor
import com.ruizurraca.logrobus.transit.data.SyncStatusRepositoryImpl
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind
import com.ruizurraca.logrobus.transit.domain.LinesRepository
import com.ruizurraca.logrobus.transit.domain.StaticSyncEngine
import com.ruizurraca.logrobus.transit.domain.SyncStatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Sample lines for previews, Swift tests and debug builds, until LB-008 part 2 downloads the real catalogue (owner
 * decision, 2026-11-02). They have the network's shape: its codes, no line 8, and lines 4 and B1, and 9 and B2, sharing
 * a colour (DD-08). Their ids, names and colours are placeholders, not the source's (OQ-11). Line 11 is not
 * operational, so its status chip shows (FR-LINE-02). Each heads for each end of its name, outbound first, so a
 * favourite card shows both headsigns (FR-LINE-13).
 */
internal object SampleLines {
    private const val RED = 0xFFE30613
    private const val BLUE = 0xFF1565C0

    val all: List<Line> =
        listOf(
            line(1, "1", "Lardero - Hospital San Pedro", 0xFF0FDCFC),
            line(2, "2", "Yagüe - Varea", 0xFF2E7D32),
            line(3, "3", "Campillo - El Cubo", 0xFFF9A825),
            line(4, "4", "Casa Blanca - Yagüe", RED),
            line(5, "5", "Los Lirios - Cascajos", 0xFF6A1B9A),
            line(6, "6", "Valdegastea - Polígono Cantabria", 0xFFEF6C00),
            line(7, "7", "Ayuntamiento - La Estrella", 0xFF8D6E63),
            line(9, "9", "Puente Madre - IES", BLUE),
            line(10, "10", "El Arco - Hospital San Pedro", 0xFFAD1457),
            line(11, "11", "Artesanos - Manuel de Falla", 0xFF00897B, isOperational = false),
            line(101, "B1", "Varea - La Cava", RED),
            line(102, "B2", "Yagüe - Lardero", BLUE),
            line(103, "B3", "Valdegastea - El Cubo", 0xFF37474F),
        )

    private fun line(
        id: Int,
        code: String,
        displayName: String,
        color: Long,
        isOperational: Boolean = true,
    ) = Line(
        id = id,
        code = code,
        displayName = displayName,
        color = color,
        onColor = onColorFor(color),
        kind = if (code.startsWith("B")) LineKind.NIGHT else LineKind.DAY,
        isOperational = isOperational,
        // "Lardero - Hospital San Pedro" runs towards Hospital San Pedro, then back towards Lardero.
        headsigns = displayName.split(" - ").reversed(),
    )
}

/** The sample lines as a repository, for a preview's or a screen test's scripted graph (LB-008 part 2). */
private object SampleLinesRepository : LinesRepository {
    override fun observeLines(): Flow<AppResult<List<Line>>> = flowOf(AppResult.Success(SampleLines.all))
}

/**
 * The scripted static data of a preview or a screen test, beside `SharedDependencies.preview()` (ADR-0007): the sample
 * lines, and a sync that succeeds without a source. A test starts its graph over this module instead of the transit
 * data one, so a screen shows the lines it scripted rather than the empty cache an unreachable source leaves (LB-008
 * part 2). No production graph builds it: `LogroBusApplication` and `LogroBusApp` call `initKoin`.
 */
fun previewDataModule(): Module =
    module {
        // The rest of what the real transit data module binds: a screen test reaches About data's last syncs too, and
        // a missing binding fails at the first ViewModel that asks (LB-008 part 2).
        single<SyncStatusRepository> { SyncStatusRepositoryImpl(get(), get(), get()) }
        single<StaticSyncEngine> {
            object : StaticSyncEngine {
                override suspend fun syncIfStale() = AppResult.Success(Unit)
            }
        }
        single<LinesRepository> { SampleLinesRepository }
    }
