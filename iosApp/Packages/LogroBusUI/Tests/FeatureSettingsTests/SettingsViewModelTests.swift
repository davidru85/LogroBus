@testable import FeatureSettings
import Foundation
@preconcurrency import Shared
import SwiftUI
import Testing

/// ADR-0007 and FR-SET-01: Settings' Swift ViewModel builds its state with the shared use cases and mapper, sends each
/// change through its use case, and says once that the cache was cleared (LB-403).
@MainActor
struct SettingsViewModelTests {
    @Test
    func lb403_theViewModelShowsWhatTheSharedMapperMakesOfTheSettings() async throws {
        let useCases = SharedDependencies.shared.preview().settings(theme: .dark, etaFormat: .clockTime)
        let viewModel = SettingsViewModel(useCases: useCases, language: .english)
        #expect(viewModel.state is SettingsUiStateLoading)

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.state is SettingsUiStateContent }

        let content = try #require(viewModel.state as? SettingsUiStateContent)
        #expect(content.language == .english)
        #expect(content.theme == .dark)
        #expect(content.etaFormat == .clockTime)
        // The preview's syncs happened at 09:30 UTC, which is 10:30 in Madrid (AGENTS.md §5.5).
        #expect((content.dataStatus as? DataStatusUiSynced)?.at.hour == 10)
    }

    @Test
    func lb403_aNewThemeAndEtaFormat_goThroughTheirUseCases_andShow() async throws {
        let viewModel = SettingsViewModel(useCases: SharedDependencies.shared.preview(), language: .spanish)
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.state is SettingsUiStateContent }

        viewModel.onEvent(SettingsEventSetTheme(theme: .light))
        viewModel.onEvent(SettingsEventSetEtaFormat(format: .clockTime))

        try await waitUntil {
            let content = viewModel.state as? SettingsUiStateContent
            return content?.theme == .light && content?.etaFormat == .clockTime
        }
    }

    @Test
    func lb403_clearCache_clearsIt_andSaysSoOnce() async throws {
        let useCases = SharedDependencies.shared.preview()
        let viewModel = SettingsViewModel(useCases: useCases, language: .spanish)

        viewModel.onEvent(SettingsEventClearCache.shared)
        try await waitUntil { viewModel.cacheMessage == .cleared }
        #expect(useCases.cacheClears == 1)

        viewModel.onEvent(SettingsEventCacheMessageShown.shared)
        #expect(viewModel.cacheMessage == nil)
    }

    @Test
    func lb403_aCacheThatCannotBeCleared_saysSo() async throws {
        let viewModel = SettingsViewModel(useCases: SharedDependencies.shared.preview().failing(), language: .spanish)

        viewModel.onEvent(SettingsEventClearCache.shared)

        try await waitUntil { viewModel.cacheMessage == .failed }
    }

    @Test
    func lb403_leavingTheScreenStopsReadingTheDataStatus() async throws {
        let useCases = SharedDependencies.shared.preview()
        let viewModel = SettingsViewModel(useCases: useCases, language: .spanish)

        let observation = Task { await viewModel.observe() }
        try await waitUntil { useCases.activeCollections == 1 }
        // What `.task {}` does when the screen leaves.
        observation.cancel()
        await observation.value

        try await waitUntil { useCases.activeCollections == 0 }
    }

    @Test
    func lb403_theLanguageIsTheOneTheAppRunsIn() {
        #expect(AppLanguage.of(localization: "es") == .spanish)
        #expect(AppLanguage.of(localization: "en") == .english)
        // Spanish is the development language, which iOS falls back to (NFR-I18N-01).
        #expect(AppLanguage.of(localization: nil) == .spanish)
    }
}

/// FR-SET-01 on iOS: the app's root follows the theme chosen in Settings.
@MainActor
struct AppAppearanceTests {
    @Test
    func lb403_eachThemeIsItsColorScheme() {
        #expect(AppAppearance.colorScheme(of: .system) == nil)
        #expect(AppAppearance.colorScheme(of: .light) == .light)
        #expect(AppAppearance.colorScheme(of: .dark) == .dark)
    }

    @Test
    func lb403_theAppFollowsTheThemeChosenInSettings() async throws {
        let useCases = SharedDependencies.shared.preview()
        let appearance = AppAppearance(useCases: useCases)
        let observation = Task { await appearance.observe() }
        defer { observation.cancel() }

        _ = try await useCases.setTheme().invoke(theme: .dark)

        try await waitUntil { appearance.colorScheme == .dark }
    }
}
