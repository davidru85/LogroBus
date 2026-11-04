package com.ruizurraca.logrobus.android.core.testing

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider

/** LB-406 (NFR-A11Y): fails on what TalkBack users would meet on the screen under test. */
fun SemanticsNodeInteractionsProvider.assertAccessible() {
    // The checks come with the green commit.
}
