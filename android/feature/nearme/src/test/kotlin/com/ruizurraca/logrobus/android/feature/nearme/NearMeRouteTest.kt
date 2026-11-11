package com.ruizurraca.logrobus.android.feature.nearme

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.feature.nearme.domain.LocationAccess
import com.ruizurraca.logrobus.feature.nearme.domain.MyStops
import com.ruizurraca.logrobus.feature.nearme.domain.NearYouState
import com.ruizurraca.logrobus.feature.nearme.domain.NearbyStops
import com.ruizurraca.logrobus.feature.nearme.domain.ObserveLocationAccess
import com.ruizurraca.logrobus.feature.nearme.domain.ObserveMyStops
import com.ruizurraca.logrobus.feature.nearme.domain.ObserveNearYou
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/**
 * FR-LOC-01 on Android: Near Me asks for nothing until the user taps "Turn on location", then for foreground location
 * only, approximate or precise. Once the system no longer asks, the row opens the app's settings.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NearMeRouteTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val prompts = RecordingRegistry()

    @After
    fun stopGraph() = stopKoin()

    @Test
    fun lb201_openingNearMeAsksForNoPermission() {
        show(Permission())

        compose.onNodeWithText(rowText()).assertExists()
        assertEquals(emptyList<List<String>>(), prompts.requests)
    }

    @Test
    fun lb201_theRowAsksForForegroundLocationOnly_approximateOrPrecise() {
        show(Permission())

        compose.onNodeWithText(rowText()).performClick()

        assertEquals(
            listOf(listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)),
            prompts.requests,
        )
    }

    @Test
    fun lb201_afterTheSystemsAnswer_accessIsReadAgain() {
        val permission = Permission()
        show(permission)

        compose.onNodeWithText(rowText()).performClick()
        // The user grants approximate location only.
        permission.access = LocationAccess.On
        prompts.answer(coarse = true, fine = false)

        compose.onNodeWithText(rowText()).assertDoesNotExist()
    }

    @Test
    fun lb201_whenTheSystemNoLongerAsks_theRowOpensTheAppsSettings() {
        // Denied twice before: the system denies at once, and the app may no longer explain.
        show(Permission())

        compose.onNodeWithText(rowText()).performClick()
        prompts.answer(coarse = false, fine = false)

        val settings = shadowOf(compose.activity).nextStartedActivity
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, settings?.action)
        assertEquals(Uri.fromParts("package", compose.activity.packageName, null), settings?.data)
    }

    @Test
    fun lb201_aFirstDenial_leavesTheRowAskingAgain() {
        show(Permission())

        compose.onNodeWithText(rowText()).performClick()
        // After a first denial the app may explain, and the system will ask once more.
        explains(true)
        prompts.answer(coarse = false, fine = false)

        assertNull(shadowOf(compose.activity).nextStartedActivity)
    }

    @Test
    fun lb201_aSecondDenial_doesNotOpenTheSettingsAtOnce() {
        show(Permission())
        explains(true)

        compose.onNodeWithText(rowText()).performClick()
        // The user just said no again: the next tap opens the settings, not this one.
        explains(false)
        prompts.answer(coarse = false, fine = false)

        assertNull(shadowOf(compose.activity).nextStartedActivity)
    }

    @Test
    fun lb201_anInterruptedPrompt_opensNothing() {
        show(Permission())

        compose.onNodeWithText(rowText()).performClick()
        // Android answers an interrupted request with no grants at all: a cancellation, not a denial.
        prompts.cancel()

        assertNull(shadowOf(compose.activity).nextStartedActivity)
    }

    @Test
    fun lb201_aSecondTapWhileThePromptShows_asksOnce() {
        show(Permission())

        compose.onNodeWithText(rowText()).performClick()
        compose.onNodeWithText(rowText()).performClick()

        assertEquals(1, prompts.requests.size)
    }

    @Test
    fun lb201_aSecondDenialAcrossARotation_doesNotOpenTheSettingsAtOnce() {
        val restoration = StateRestorationTester(compose)
        show(Permission(), restoration)
        explains(true)

        compose.onNodeWithText(rowText()).performClick()
        // The activity is recreated while the prompt shows.
        restoration.emulateSavedInstanceStateRestore()
        explains(false)
        prompts.answer(coarse = false, fine = false)

        assertNull(shadowOf(compose.activity).nextStartedActivity)
    }

    @Test
    fun lb201_onADeviceWithoutTheAppsSettingsScreen_theRowDoesNotCrash() {
        // Some managed devices have none: Android then throws, as Robolectric does when it checks activities.
        shadowOf(compose.activity.application).checkActivities(true)
        show(Permission())

        compose.onNodeWithText(rowText()).performClick()
        prompts.answer(coarse = false, fine = false)

        compose.onNodeWithText(rowText()).assertExists()
    }

    private fun show(
        observe: ObserveLocationAccess,
        restoration: StateRestorationTester? = null,
    ) {
        // LB-203: the screen now reads its section from `ObserveNearYou`, so the scripted access is wrapped in the
        // section the screen shows: the same access, and the stops only once it is on.
        val section =
            ObserveNearYou {
                observe().map { access ->
                    NearYouState(access, nearby = if (access == LocationAccess.On) NearbyStops(emptyList()) else null)
                }
            }
        startKoin {
            modules(
                module {
                    factory { section }
                    // LB-208: the screen reads "My stops" too, which these tests are not about.
                    factory<ObserveMyStops> {
                        ObserveMyStops { flowOf(AppResult.Success(MyStops(emptyList(), emptyMap()))) }
                    }
                },
                nearMeUiModule,
            )
        }
        val owner =
            object : ActivityResultRegistryOwner {
                override val activityResultRegistry: ActivityResultRegistry = prompts
            }
        val content: @Composable () -> Unit = {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) { NearMeRoute(onOpenMore = {}) }
        }
        if (restoration != null) restoration.setContent(content) else compose.setContent(content)
    }

    private fun rowText() = compose.activity.getString(R.string.nearme_nearby_turn_on_location)

    /** Whether Android lets the app explain why it asks for location, as it does after a first denial. */
    private fun explains(explains: Boolean) {
        val packageManager = shadowOf(compose.activity.packageManager)
        packageManager.setShouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION, explains)
        packageManager.setShouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION, explains)
    }

    /** Stands in for the system's prompt: it records each request, and the test answers it. */
    private inner class RecordingRegistry : ActivityResultRegistry() {
        val requests = mutableListOf<List<String>>()
        private var requestCode = -1

        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            this.requestCode = requestCode
            requests += (input as Array<*>).map { it as String }
        }

        fun answer(
            coarse: Boolean,
            fine: Boolean,
        ) {
            val grants =
                mapOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION to coarse,
                    Manifest.permission.ACCESS_FINE_LOCATION to fine,
                )
            // As Android does, the answer comes as the intent the contract parses.
            val result =
                Intent()
                    .putExtra(RequestMultiplePermissions.EXTRA_PERMISSIONS, grants.keys.toTypedArray())
                    .putExtra(
                        RequestMultiplePermissions.EXTRA_PERMISSION_GRANT_RESULTS,
                        grants.values.map { if (it) PERMISSION_GRANTED else PERMISSION_DENIED }.toIntArray(),
                    )
            compose.runOnUiThread { dispatchResult(requestCode, Activity.RESULT_OK, result) }
            compose.waitForIdle()
        }

        /** An interrupted request, which Android answers with no permissions and no grants. */
        fun cancel() {
            compose.runOnUiThread { dispatchResult(requestCode, Activity.RESULT_CANCELED, null) }
            compose.waitForIdle()
        }
    }

    /** Location access as the system reports it at each read: off until a test grants it. */
    private class Permission : ObserveLocationAccess {
        var access = LocationAccess.Off

        override fun invoke(): Flow<LocationAccess> = flowOf(access)
    }
}
