package com.ruizurraca.logrobus.android.core.navigation

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** LB-012: a tab root that is asked to scroll to its top does so (FR-NAV-03). */
@RunWith(AndroidJUnit4::class)
class ScrollToTopOnReselectTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lb012_aRequestScrollsTheRootsListToItsTop() {
        val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        lateinit var list: LazyListState
        compose.setContent {
            list = rememberLazyListState(initialFirstVisibleItemIndex = ROWS / 2)
            CompositionLocalProvider(LocalScrollToTopRequests provides requests) {
                ScrollToTopOnReselect(list)
                LazyColumn(state = list) { items(ROWS) { BasicText("Row $it") } }
            }
        }

        compose.runOnIdle { requests.tryEmit(Unit) }

        compose.runOnIdle { assertEquals(0, list.firstVisibleItemIndex) }
    }

    private companion object {
        const val ROWS = 100
    }
}
