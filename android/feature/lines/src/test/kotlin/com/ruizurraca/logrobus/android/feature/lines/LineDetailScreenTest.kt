package com.ruizurraca.logrobus.android.feature.lines

import android.content.Context
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailEvent
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUi
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import com.ruizurraca.logrobus.android.core.designsystem.R as DS

/**
 * LB-103 (FR-LINE-09): the line detail, until LB-104 adds its map, directions and stops. Its title names the line, its
 * body shows the badge and the name, and its toolbar has the favourite toggle.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LineDetailScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val line3 = LineDetailUi(3, "3", "Campillo - El Cubo", 0xFFF9A825, 0xFF000000, isFavourite = false)

    @Test
    fun lb103_theDetailIsTitledByItsLine_andShowsItsBadgeAndName() {
        compose.setContent { LineDetailScreen(LineDetailUiState.Content(line3), onEvent = {}, onBack = {}) }

        compose.onNode(isHeading() and hasText(context.getString(R.string.line_detail_title, "3"))).assertExists()
        compose.onNode(hasContentDescription(context.getString(DS.string.line_badge, "3"))).assertExists()
        compose.onNodeWithText("Campillo - El Cubo").assertExists()
    }

    @Test
    fun lb103_theToolbarsToggleIsLabelledByTheLine_andSendsItsNewValue() {
        val events = mutableListOf<LineDetailEvent>()
        compose.setContent {
            LineDetailScreen(LineDetailUiState.Content(line3), onEvent = { events += it }, onBack = {})
        }

        compose.onNodeWithContentDescription(context.getString(R.string.lines_favourite_add, "3")).performClick()

        assertEquals(listOf(LineDetailEvent.SetFavourite(isFavourite = true)), events)
    }

    @Test
    fun lb103_aFavouriteLinesToggleRemovesIt() {
        val events = mutableListOf<LineDetailEvent>()
        val favourite = LineDetailUiState.Content(line3.copy(isFavourite = true))
        compose.setContent { LineDetailScreen(favourite, onEvent = { events += it }, onBack = {}) }

        compose.onNodeWithContentDescription(context.getString(R.string.lines_favourite_remove, "3")).performClick()

        assertEquals(listOf(LineDetailEvent.SetFavourite(isFavourite = false)), events)
    }

    @Test
    fun lb103_upGoesBack() {
        var back = 0
        compose.setContent { LineDetailScreen(LineDetailUiState.Content(line3), onEvent = {}, onBack = { back++ }) }

        compose.onNodeWithContentDescription(context.getString(DS.string.action_navigate_up)).performClick()

        assertEquals(1, back)
    }

    @Test
    fun lb103_theFirstLoadIsAnnouncedToTalkBack() {
        compose.setContent { LineDetailScreen(LineDetailUiState.Loading, onEvent = {}, onBack = {}) }

        compose.onNode(hasContentDescription(context.getString(R.string.line_detail_loading))).assertExists()
    }

    @Test
    fun lb103_anErrorShowsItsReason_andRetrySendsTheEvent() {
        val events = mutableListOf<LineDetailEvent>()
        compose.setContent {
            LineDetailScreen(LineDetailUiState.Error(ErrorReason.NETWORK), onEvent = { events += it }, onBack = {})
        }

        compose.onNodeWithText(context.getString(DS.string.error_network)).assertExists()
        compose.onNodeWithText(context.getString(DS.string.error_retry)).performClick()

        assertEquals(listOf(LineDetailEvent.Retry), events)
    }
}
