import DesignSystem
@preconcurrency import Shared
import SwiftUI
import UIKit

/// Settings (LB-403, FR-SET-01), opened from More (LB-012), in system controls. Appearance holds the language, which
/// the system's Settings changes (ADR-0020), and the theme; Arrivals the ETA format; Data the data status and Clear
/// cache, whose outcome is an alert.
public struct SettingsView: View {
    @State private var viewModel: SettingsViewModel
    @Environment(\.locale) private var locale
    @Environment(\.openURL) private var openURL

    public init(viewModel: SettingsViewModel = SettingsViewModel()) {
        _viewModel = State(initialValue: viewModel)
    }

    public var body: some View {
        content
            .navigationTitle(Text("settings.title", bundle: .module))
            // Cancelled when the screen leaves, which stops the use cases' collections.
            .task { await viewModel.observe() }
            .cacheMessageAlert(viewModel.cacheMessage) { viewModel.onEvent(SettingsEventCacheMessageShown.shared) }
    }

    @ViewBuilder private var content: some View {
        switch onEnum(of: viewModel.state) {
        case .loading:
            // Spec §7.3: the final layout with placeholders, never a lone spinner. VoiceOver hears that Settings is
            // loading rather than the sample values.
            form(Self.placeholder)
                .redacted(reason: .placeholder)
                .disabled(true)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(Text("settings.loading", bundle: .module))
        case .content(let content):
            form(content)
        }
    }

    private func form(_ content: SettingsUiStateContent) -> some View {
        Form {
            Section {
                languageRow(content.language)
                Picker(selection: Binding(get: { content.theme }, set: { theme in
                    viewModel.onEvent(SettingsEventSetTheme(theme: theme))
                })) {
                    ForEach(ThemePreference.allCases, id: \.self) { theme in
                        Text(theme.label, bundle: .module).tag(theme)
                    }
                } label: {
                    Text("settings.theme", bundle: .module)
                }
            } header: {
                Text("settings.appearance", bundle: .module)
                    .foregroundStyle(Tokens.Colors.secondaryText)
            } footer: {
                Text("settings.language.footer", bundle: .module)
                    .foregroundStyle(Tokens.Colors.secondaryText)
            }
            Section {
                Picker(selection: Binding(get: { content.etaFormat }, set: { format in
                    viewModel.onEvent(SettingsEventSetEtaFormat(format: format))
                })) {
                    ForEach(EtaFormat.allCases, id: \.self) { format in
                        Text(format.label, bundle: .module).tag(format)
                    }
                } label: {
                    Text("settings.eta_format", bundle: .module)
                }
            } header: {
                Text("settings.arrivals", bundle: .module)
                    .foregroundStyle(Tokens.Colors.secondaryText)
            } footer: {
                Text("settings.eta_format.footer", bundle: .module)
                    .foregroundStyle(Tokens.Colors.secondaryText)
            }
            Section {
                LabeledContent {
                    dataStatus(content.dataStatus)
                        .foregroundStyle(Tokens.Colors.secondaryText)
                } label: {
                    Text("settings.data_status", bundle: .module)
                }
                Button {
                    viewModel.onEvent(SettingsEventClearCache.shared)
                } label: {
                    Text("settings.clear_cache", bundle: .module)
                }
            } header: {
                Text("settings.data", bundle: .module)
                    .foregroundStyle(Tokens.Colors.secondaryText)
            } footer: {
                Text("settings.clear_cache.detail", bundle: .module)
                    .foregroundStyle(Tokens.Colors.secondaryText)
            }
        }
    }

    /// The language the app runs in. iOS keeps it in the app's page of the system's Settings, which the row opens.
    private func languageRow(_ language: AppLanguage) -> some View {
        Button {
            if let settings = URL(string: UIApplication.openSettingsURLString) { openURL(settings) }
        } label: {
            LabeledContent {
                HStack {
                    language.name
                        .foregroundStyle(Tokens.Colors.secondaryText)
                    // The row opens the iPhone's Settings, outside the app.
                    Image(systemName: "arrow.up.forward.app")
                        .foregroundStyle(.tertiary)
                        .accessibilityHidden(true)
                }
            } label: {
                Text("settings.language", bundle: .module)
                    .foregroundStyle(Color.primary)
            }
        }
        .accessibilityHint(Text("settings.language.hint", bundle: .module))
    }

    @ViewBuilder private func dataStatus(_ status: any DataStatusUi) -> some View {
        switch onEnum(of: status) {
        case .synced(let synced):
            Text("settings.data_status.synced \(synced.at.formatted(locale: locale))", bundle: .module)
        case .neverSynced:
            Text("settings.data_status.never", bundle: .module)
        case .unknown:
            Text("settings.data_status.unknown", bundle: .module)
        }
    }

    /// Sample content for the redacted first load.
    private static let placeholder = SettingsUiStateContent(
        language: .spanish,
        theme: .system,
        etaFormat: .minutes,
        dataStatus: DataStatusUiSynced(at: MadridDateTime(year: 2026, month: 1, day: 1, hour: 0, minute: 0))
    )
}

extension AppLanguage {
    /// The language's name. Spanish and English are named in themselves, in every language (the catalogue marks them
    /// as not translated).
    var name: Text {
        switch self {
        case .system: Text("settings.language.system", bundle: .module)
        case .spanish: Text("settings.language.spanish", bundle: .module)
        case .english: Text("settings.language.english", bundle: .module)
        }
    }
}

extension ThemePreference {
    var label: LocalizedStringKey {
        switch self {
        case .system: "settings.theme.system"
        case .light: "settings.theme.light"
        case .dark: "settings.theme.dark"
        }
    }
}

extension EtaFormat {
    var label: LocalizedStringKey {
        switch self {
        case .minutes: "settings.eta_format.minutes"
        case .clockTime: "settings.eta_format.clock_time"
        }
    }
}

extension View {
    /// FR-SET-01: an alert with what Clear cache did while `message` is set; dismissing it calls `onShown`, so it shows
    /// once. VoiceOver reads an alert as it appears.
    func cacheMessageAlert(_ message: CacheMessage?, onShown: @escaping () -> Void) -> some View {
        modifier(CacheMessageAlert(message: message, onShown: onShown))
    }
}

private struct CacheMessageAlert: ViewModifier {
    let message: CacheMessage?
    let onShown: () -> Void
    // The message the alert shows: kept while it closes, when `message` is already nil, so its title never changes.
    @State private var shown = CacheMessage.cleared

    func body(content: Content) -> some View {
        content
            .onChange(of: message, initial: true) { _, said in if let said { shown = said } }
            .alert(
                Text((message ?? shown).title, bundle: .module),
                isPresented: Binding(get: { message != nil }, set: { if !$0 { onShown() } }),
                actions: {}
            )
    }
}

extension CacheMessage {
    var title: LocalizedStringKey {
        switch self {
        case .cleared: "settings.cache_cleared"
        case .failed: "settings.cache_failed"
        }
    }
}

// Inside a NavigationStack, as More pushes it, so the previews show its title.
@MainActor
private func settings(_ useCases: PreviewUseCases) -> some View {
    NavigationStack {
        SettingsView(viewModel: SettingsViewModel(useCases: useCases, language: .spanish))
    }
}

#Preview("Content") {
    settings(SharedDependencies.shared.preview().settings(theme: .dark, etaFormat: .clockTime))
}

#Preview("Content, dark, large text") {
    settings(SharedDependencies.shared.preview())
        .preferredColorScheme(.dark)
        .dynamicTypeSize(.accessibility3)
}

#Preview("Never synced") {
    settings(SharedDependencies.shared.preview().neverSynced())
}

#Preview("Sync status unknown") {
    settings(SharedDependencies.shared.preview().failing())
}

#Preview("Loading") {
    settings(SharedDependencies.shared.preview().loading())
}
