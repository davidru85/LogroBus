package com.ruizurraca.logrobus.android.app

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.android.feature.settings.AboutDataViewModel
import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.umbrella.initKoin
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.error.KoinApplicationAlreadyStartedException
import kotlin.reflect.KClass

/** ADR-0014: the app starts the dependency graph at launch, exactly once per process. */
@RunWith(AndroidJUnit4::class)
class AppGraphTest {
    @Test
    fun lb010_theAppStartsTheGraphOnceAtLaunch() {
        val application = ApplicationProvider.getApplicationContext<Application>()

        assertNotNull("The graph should be started at launch", GlobalContext.getOrNull()?.get<AppClock>())
        assertThrows(KoinApplicationAlreadyStartedException::class.java) {
            initKoin(application, appVersion = "1.2.0", debug = true)
        }
    }

    @Test
    fun lb010_theAppGraphGivesEachScreenItsViewModel() {
        // Created in a store and cleared, as a screen's would be, so no ViewModel outlives the test.
        val store = ViewModelStore()
        val fromTheGraph =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(
                    modelClass: KClass<T>,
                    extras: CreationExtras,
                ): T = GlobalContext.get().get(modelClass)
            }

        assertNotNull(ViewModelProvider.create(store, fromTheGraph)[AboutDataViewModel::class])
        store.clear()
    }
}
