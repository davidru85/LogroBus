package com.ruizurraca.logrobus.android.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.android.feature.lines.linesUiModule
import com.ruizurraca.logrobus.umbrella.SharedDependencies
import org.koin.compose.KoinApplicationPreview
import org.koin.dsl.module

// The shell and More have one state each; ShellScreenshotTest and RailScreenshotTest capture the same views.

@PreviewLightDark
@PreviewFontScale
@PreviewScreenSizes
@Composable
private fun AppShellPreview() {
    // The Lines root takes its ViewModel from the graph, here over the scripted use cases (architecture.md §5.2).
    // One scripted set, so that a toggle in the interactive preview reaches the list.
    val useCases = SharedDependencies.preview()
    val lines =
        module {
            factory { useCases.observeLinesWithFavourites() }
            factory { useCases.setFavouriteLine() }
        }
    KoinApplicationPreview(application = { modules(lines, linesUiModule) }) {
        LogroBusTheme { AppShell() }
    }
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun MorePreview() {
    LogroBusTheme { MoreScreen(onOpenSettings = {}, onOpenAboutData = {}, onBack = {}) }
}
