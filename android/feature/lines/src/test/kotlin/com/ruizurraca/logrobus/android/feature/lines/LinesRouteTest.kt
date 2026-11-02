package com.ruizurraca.logrobus.android.feature.lines

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.feature.lines.domain.LinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.SetFavouriteLine
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.annotation.GraphicsMode

/** ADR-0014 on Android: the Lines root gets its ViewModel from the graph with koinViewModel(). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LinesRouteTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun startGraph() {
        val line = Line(4, "4", "Casa Blanca - Yagüe", 0xFFE30613, 0xFFFFFFFF, LineKind.DAY, isOperational = true)
        val lines = AppResult.Success(LinesWithFavourites(listOf(line), favouriteIds = emptyList()))
        startKoin {
            modules(
                module {
                    factory { ObserveLinesWithFavourites { flowOf(lines) } }
                    factory { SetFavouriteLine { _, _ -> AppResult.Success(Unit) } }
                },
                linesUiModule,
            )
        }
    }

    @After
    fun stopGraph() = stopKoin()

    @Test
    fun lb101_theRouteShowsTheLinesOfItsViewModelFromTheGraph() {
        compose.setContent { LinesRoute(onOpenMore = {}, onOpenLine = {}) }

        compose
            .onNodeWithContentDescription(
                compose.activity.getString(com.ruizurraca.logrobus.android.core.designsystem.R.string.line_badge, "4"),
            ).assertExists()
    }
}
