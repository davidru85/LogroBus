package com.ruizurraca.logrobus.android.app

/**
 * The SDK that screenshot tests run on. On SDK 37, the target, Robolectric 4.17 draws only the first Roborazzi capture
 * of a test run, and every later one comes out blank; SDK 36 draws them all. Other tests keep the target SDK.
 */
internal const val SCREENSHOT_SDK = 36
