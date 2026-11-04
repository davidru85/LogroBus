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
    /// What a "Clear cache" leaves to say, until the screen says it has shown it: state rather than an effect, as the
    /// favourite limit's message (LB-110).
    public private(set) var cacheMessage: CacheMessage?
    // The use cases' last values, which a change of either maps again; none while they have not answered yet.
    private var preferences: UserPreferences?
    private var syncs: AppResult<NSArray>?
    // Kept rather than resolved here: SwiftUI may build a view's ViewModel on every parent update and keep only the
    // first, so the init stays cheap.
    private let useCases: any UseCases
    private let language: AppLanguage
    // The last change, a setting or Clear cache. Each waits for the one before: SKIE may start two Kotlin calls in
    // either order, so two quick taps could otherwise land the wrong way round.
    private var change: Task<Void, Never>?

    /// Takes the use cases from the graph, or from `SharedDependencies.shared.preview()` in previews and tests, and
    /// the `language` the app runs in.
    public init(useCases: any UseCases = SharedDependencies.shared, language: AppLanguage = .current) {
        self.useCases = useCases
        self.language = language
    }

    public func onEvent(_ event: any SettingsEvent) {
        switch onEnum(of: event) {
        case .setTheme(let setTheme):
            // The setting shows through the store's flow. One that cannot be saved, which the store has logged,
            // leaves Settings as it was.
            queue { [useCases] in _ = try? await useCases.setTheme().invoke(theme: setTheme.theme) }
        case .setEtaFormat(let setEtaFormat):
            queue { [useCases] in _ = try? await useCases.setEtaFormat().invoke(format: setEtaFormat.format) }
        case .clearCache:
            queue { [useCases] in
                if let result = try? await useCases.clearCache().invoke() {
                    self.cacheMessage = SettingsUiMapper.shared.cacheMessage(result: result)
                } else {
                    // A clear that could not even be asked for says what a failure the platform reports says.
                    self.cacheMessage = .failed
                }
            }
        case .cacheMessageShown:
            cacheMessage = nil
        case .setLanguage:
            // ADR-0020: iOS keeps the app's language in the system's Settings, which the screen opens instead.
            break
        }
    }

    /// Runs while the screen shows: the view calls it from `.task {}`, which cancels it when the screen leaves.
    public func observe() async {
        await withTaskGroup(of: Void.self) { group in
            group.addTask { await self.observePreferences() }
            group.addTask { await self.observeLastSyncs() }
        }
    }

    private func observePreferences() async {
        for await preferences in useCases.observePreferences().invoke() {
            self.preferences = preferences
            show()
        }
    }

    private func observeLastSyncs() async {
        for await syncs in useCases.observeLastSyncs().invoke() {
            self.syncs = syncs
            show()
        }
    }

    private func show() {
        guard let preferences, let syncs else { return }
        state = SettingsUiMapper.shared.state(preferences: preferences, syncs: syncs, language: language)
    }

    private func queue(_ work: @escaping @MainActor () async -> Void) {
        let previous = change
        change = Task {
            await previous?.value
            await work()
        }
    }
}

extension AppLanguage {
    /// The language the app runs in, which iOS picks from the device's languages or the app's own (ADR-0020).
    public static var current: AppLanguage { of(localization: Bundle.main.preferredLocalizations.first) }

    /// The app's language for a bundle `localization`, whatever its region. Spanish is the development language,
    /// which iOS falls back to.
    static func of(localization: String?) -> AppLanguage {
        guard let localization else { return .spanish }
        return Locale.Language(identifier: localization).languageCode == .english ? .english : .spanish
    }
}

extension Locale {
    /// LB-405 (NFR-I18N-01): the locale SwiftUI looks the app's words up in, and formats with: the language iOS gave
    /// the app, Spanish, its development language, on a device whose languages it does not speak, with the device's
    /// region and preferences. With the device's own locale, a lookup that takes a locale, such as a
    /// LocalizedStringResource's, matches English for it instead, so the screens mixed the two.
    public static var app: Locale {
        app(localization: Bundle.main.preferredLocalizations.first, device: .autoupdatingCurrent)
    }

    /// The `device` locale in the language of the app's bundle `localization`, or the device's own when they agree.
    static func app(localization: String?, device: Locale) -> Locale {
        guard let localization else { return device }
        let language = Locale.Language(identifier: localization).languageCode
        guard device.language.languageCode != language else { return device }
        // Only the language changes: the region, and the device's preferences, stay.
        var components = Locale.Components(locale: device)
        components.languageComponents.languageCode = language
        components.languageComponents.script = nil
        return Locale(components: components)
    }
}
