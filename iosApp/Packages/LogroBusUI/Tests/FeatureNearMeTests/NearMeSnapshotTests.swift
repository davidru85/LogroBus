import DesignSystem
@testable import FeatureNearMe
import Shared
import SnapshotTesting
import SwiftUI
import Testing

/// AGENTS.md §9: Near Me in light, dark and large text, in Spanish. Without location, "Near you" is one row that turns
/// it on; with location, nothing shows until the nearby stops (LB-203). References live in __Snapshots__; a missing one
/// is recorded, and that run fails until it is committed.
@MainActor
struct NearMeSnapshotTests {
    @Test(arguments: Appearance.allCases)
    func lb201_withoutLocation(appearance: Appearance) async throws {
        let screen = try await screen(SharedDependencies.shared.preview(), settlesOn: NearYouUiTurnOnLocation.self)
        assertSnapshot(of: appearance.apply(to: screen), as: appearance.strategy, named: appearance.name)
    }

    @Test(arguments: Appearance.allCases)
    func lb201_withLocation(appearance: Appearance) async throws {
        let useCases = SharedDependencies.shared.preview().locationOn()
        let screen = try await screen(useCases, settlesOn: NearYouUiLocationOn.self)
        assertSnapshot(of: appearance.apply(to: screen), as: appearance.strategy, named: appearance.name)
    }

    /// The screen in its navigation stack, once its view model shows `NearYou`, so that the capture never races the
    /// first read. The view's own `.task` then finds the same state.
    private func screen<NearYou>(
        _ useCases: PreviewUseCases,
        settlesOn _: NearYou.Type
    ) async throws -> some View {
        let viewModel = NearMeViewModel(useCases: useCases) { FakeLocationAuthorization() }
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.state.nearYou is NearYou }
        return NavigationStack { NearMeView(viewModel: viewModel) }
    }
}

/// Light, dark, and 200 % text or more (`accessibility3`), on a whole iPhone screen, as the app's ShellSnapshotTests.
enum Appearance: CaseIterable, Sendable {
    case light, dark, largeText

    var name: String {
        switch self {
        case .light: "light"
        case .dark: "dark"
        case .largeText: "largeText"
        }
    }

    /// An iPhone 13 screen, with its safe area.
    @MainActor var strategy: Snapshotting<AnyView, UIImage> {
        let phone = ViewImageConfig.iPhone13
        return .image(
            precision: 0.9995,
            perceptualPrecision: 0.98,
            layout: .device(config: phone),
            traits: UITraitCollection(traitsFrom: [
                UITraitCollection(userInterfaceStyle: self == .dark ? .dark : .light),
                UITraitCollection(displayScale: 3)
            ])
        )
    }

    @MainActor func apply(to view: some View) -> AnyView {
        AnyView(
            view
                .environment(\.dynamicTypeSize, self == .largeText ? .accessibility3 : .large)
                .environment(\.locale, Locale(identifier: "es"))
                // The app's tint, as its root sets it (LB-406).
                .appTint()
        )
    }
}
