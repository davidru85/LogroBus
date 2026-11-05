package com.ruizurraca.logrobus.android.feature.nearme

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * FR-LOC-01: foreground location only, approximate or precise. Asked together, so that the user may grant approximate
 * location only. Never background location (security_privacy.md §3).
 */
internal val LOCATION_PERMISSIONS =
    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)

/**
 * What "Turn on location" does (FR-LOC-01): the system's prompt for [LOCATION_PERMISSIONS]. Once the system no longer
 * asks, the tap opens the app's settings instead. [onAnswered] follows each answer.
 */
@Composable
internal fun rememberLocationRequest(onAnswered: () -> Unit): () -> Unit {
    val activity = LocalActivity.current
    val answered by rememberUpdatedState(onAnswered)
    // Saved, because the activity may be recreated while the prompt shows.
    var explainedBefore by rememberSaveable { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(RequestMultiplePermissions()) { grants ->
            val granted = grants.values.any { it }
            if (activity != null && systemNoLongerAsks(granted, explainedBefore, activity.explainsLocation())) {
                activity.openAppSettings()
            }
            answered()
        }
    return remember(launcher, activity) {
        {
            explainedBefore = activity?.explainsLocation() ?: false
            launcher.launch(LOCATION_PERMISSIONS)
        }
    }
}

/**
 * Whether the system denied location without asking. Once the user has denied it twice, the system denies at once and
 * the app may no longer explain why it asks, before or after. After a first denial the app may explain, and after a
 * second it could explain before the prompt. A first prompt dismissed without an answer looks the same as no prompt,
 * so it opens the settings too, where location can be turned on.
 */
internal fun systemNoLongerAsks(
    granted: Boolean,
    explainedBefore: Boolean,
    explainsAfter: Boolean,
): Boolean = !granted && !explainedBefore && !explainsAfter

private fun Activity.explainsLocation(): Boolean = shouldShowRequestPermissionRationale(LOCATION_PERMISSIONS.first())

private fun Activity.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
}
