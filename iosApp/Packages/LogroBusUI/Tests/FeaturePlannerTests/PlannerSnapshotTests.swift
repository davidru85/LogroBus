import DesignSystem
@testable import FeaturePlanner
import Shared
import SnapshotTesting
import SwiftUI
import Testing

/// AGENTS.md §9: How to Get There's root in light, dark and large text, in Spanish, with and without the location
/// permission, swapped, and departing at a picked time. References live in __Snapshots__; a missing one is recorded,
/// and that run fails until it is committed.
@MainActor
struct PlannerSnapshotTests {
    @Test(arguments: Appearance.allCases)
    func lb301_withLocation(appearance: Appearance) {
        let screen = screen(SharedDependencies.shared.preview().locationOn())
        assertSnapshot(of: appearance.apply(to: screen), as: appearance.strategy, named: appearance.name)
    }

    @Test(arguments: Appearance.allCases)
    func lb301_withoutLocation(appearance: Appearance) {
        let screen = screen(SharedDependencies.shared.preview())
        assertSnapshot(of: appearance.apply(to: screen), as: appearance.strategy, named: appearance.name)
    }

    @Test(arguments: Appearance.allCases)
    func lb304_swapped(appearance: Appearance) {
        // FR-PLAN-08: one swap from "My location": the trip ends where the user is.
        let screen = screen(SharedDependencies.shared.preview().locationOn(), then: [PlannerEventSwap.shared])
        assertSnapshot(of: appearance.apply(to: screen), as: appearance.strategy, named: appearance.name)
    }

    @Test(arguments: Appearance.allCases)
    func lb303_departAt(appearance: Appearance) {
        // FR-PLAN-03: a time on another day than the preview clock's, the departure row's longest text.
        let departAt = PlannerEventDepartAt(year: 2027, month: 1, day: 15, hour: 8, minute: 19)
        let screen = screen(SharedDependencies.shared.preview().locationOn(), then: [departAt])
        assertSnapshot(of: appearance.apply(to: screen), as: appearance.strategy, named: appearance.name)
    }

    @Test(arguments: ["es", "en"])
    func lb303_departureTexts(language: String) {
        // FR-PLAN-03: every text the departure row can say. The hour 1 takes Spanish's singular article, "a la".
        let locale = Locale(identifier: language)
        func departAt(hour: Int32, day: Int32, _ relativeDay: DepartureDay) -> DepartureUiAt {
            DepartureUiAt(
                time: MadridDateTime(year: 2026, month: 10, day: day, hour: hour, minute: 5), relativeDay: relativeDay
            )
        }
        let texts = VStack(alignment: .leading, spacing: 8) {
            departureText(DepartureUiNow.shared, locale: locale)
            departureText(departAt(hour: 1, day: 28, .today), locale: locale)
            departureText(departAt(hour: 8, day: 28, .today), locale: locale)
            departureText(departAt(hour: 1, day: 29, .tomorrow), locale: locale)
            departureText(departAt(hour: 8, day: 29, .tomorrow), locale: locale)
            departureText(departAt(hour: 8, day: 15, .other), locale: locale)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .environment(\.locale, locale)
        let strategy = Snapshotting<AnyView, UIImage>.image(
            precision: 0.9995, perceptualPrecision: 0.98, layout: .fixed(width: 390, height: 260),
            traits: UITraitCollection { $0.displayScale = 2; $0.userInterfaceStyle = .light }
        )
        assertSnapshot(of: AnyView(texts), as: strategy, named: language)
    }

    /// The screen in its navigation stack, its fields already read, as the view's own `onAppear` reads them, then
    /// after `events`.
    private func screen(_ useCases: PreviewUseCases, then events: [any PlannerEvent] = []) -> some View {
        let viewModel = PlannerViewModel(useCases: useCases)
        viewModel.onEvent(PlannerEventShown.shared)
        events.forEach(viewModel.onEvent)
        return NavigationStack { PlannerView(viewModel: viewModel) }
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
            traits: UITraitCollection { traits in
                traits.userInterfaceStyle = self == .dark ? .dark : .light
                traits.displayScale = 3
            }
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
