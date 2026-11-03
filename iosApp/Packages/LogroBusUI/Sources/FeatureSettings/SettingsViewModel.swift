import Foundation
import Observation
// Kotlin types declare no Sendable conformance, so Swift 6 refuses to send a use case to the async function SKIE makes
// of a `suspend` one. Kotlin/Native's memory model lets any thread call them (ADR-0004), so Shared's types are
// imported as pre-concurrency here.
@preconcurrency import Shared

/// Settings (LB-403). It orchestrates only: the shared use cases and mapper build the state (ADR-0007).
@MainActor
@Observable
public final class SettingsViewModel {
    public private(set) var state: any SettingsUiState = SettingsUiStateLoading.shared
    /// What a "Clear cache" leaves to say, until the screen says it has shown it.
    public private(set) var cacheMessage: CacheMessage?
    private let useCases: any UseCases
    private let language: AppLanguage

    /// Takes the use cases from the graph, or from `SharedDependencies.shared.preview()` in previews and tests, and
    /// the [language] the app runs in.
    public init(useCases: any UseCases = SharedDependencies.shared, language: AppLanguage = .current) {
        self.useCases = useCases
        self.language = language
    }

    public func onEvent(_ event: any SettingsEvent) {}

    /// Runs while the screen shows: the view calls it from `.task {}`, which cancels it when the screen leaves.
    public func observe() async {}
}

extension AppLanguage {
    /// The language the app runs in, which iOS picks from the device's languages or the app's own (ADR-0020).
    public static var current: AppLanguage { of(localization: Bundle.main.preferredLocalizations.first) }

    /// The app's language for a bundle [localization].
    static func of(localization: String?) -> AppLanguage { .system }
}
