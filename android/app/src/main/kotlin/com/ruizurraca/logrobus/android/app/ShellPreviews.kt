package com.ruizurraca.logrobus.android.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme

// The shell and More have one state each; ShellScreenshotTest and RailScreenshotTest capture the same views.

@PreviewLightDark
@PreviewFontScale
@PreviewScreenSizes
@Composable
private fun AppShellPreview() {
    LogroBusTheme { AppShell() }
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun MorePreview() {
    LogroBusTheme { MoreScreen(onOpenSettings = {}, onOpenAboutData = {}, onBack = {}) }
}
