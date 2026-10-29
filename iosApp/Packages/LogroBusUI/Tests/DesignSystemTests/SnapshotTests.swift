@testable import DesignSystem
import Shared
import SnapshotTesting
import SwiftUI
import Testing

/// AGENTS.md §9 and LB-011: every component in light, dark and large text. The glass helpers' snapshots run hosted by
/// the app (iosAppTests/GlassSnapshotTests), because glass needs a key window. References live in __Snapshots__; a
/// missing one is recorded, and that run fails until it is committed.
@MainActor
struct SnapshotTests {
    @Test(arguments: Appearance.allCases)
    func lb011_theCurrentLineBadges(appearance: Appearance) {
        assertSnapshot(of: appearance.apply(to: CurrentLineBadges()), as: appearance.strategy, named: appearance.name)
    }

    @Test(arguments: Appearance.allCases)
    func lb011_everyEtaCase(appearance: Appearance) {
        assertSnapshot(of: appearance.apply(to: EveryEta()), as: appearance.strategy, named: appearance.name)
    }
}

/// Light, dark, and about 200 % text (`accessibility2`, 33 pt body text).
enum Appearance: CaseIterable, Sendable {
    case light, dark, largeText

    var name: String {
        switch self {
        case .light: "light"
        case .dark: "dark"
        case .largeText: "largeText"
        }
    }

    @MainActor var strategy: Snapshotting<AnyView, UIImage> {
        // A little tolerance: the reference machine and CI may run different Xcode versions.
        .image(
            precision: 0.99,
            perceptualPrecision: 0.98,
            layout: .sizeThatFits,
            traits: UITraitCollection(userInterfaceStyle: self == .dark ? .dark : .light)
        )
    }

    @MainActor func apply(to view: some View) -> AnyView {
        AnyView(
            view
                .environment(\.dynamicTypeSize, self == .largeText ? .accessibility2 : .large)
                .padding(Tokens.Spacing.small)
                .background(Color(uiColor: .systemBackground))
        )
    }
}

/// Spec §3's line catalogue as of 2026-10, with the onColor the shared rule gives each.
private struct CurrentLineBadges: View {
    private let lines: [(String, Int64)] = [
        ("1", 0xFF26_9905), ("2", 0xFFFF_FF00), ("3", 0xFFC1_00FB), ("4", 0xFFFD_0200), ("5", 0xFFFD_82BB),
        ("6", 0xFF92_4C48), ("7", 0xFFBA_BABA), ("9", 0xFF00_02FD), ("10", 0xFF0F_DCFC), ("11", 0xFFFF_E07D),
        ("B1", 0xFFFD_0200), ("B2", 0xFF00_02FD), ("B3", 0xFF01_FF01)
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: Tokens.Spacing.small) {
            ForEach(LineBadgeSize.allCases, id: \.self) { size in
                // Two rows per size, so 200 % text still fits the width.
                ForEach([Array(lines.prefix(7)), Array(lines.dropFirst(7))], id: \.first?.0) { row in
                    HStack(spacing: Tokens.Spacing.xSmall) {
                        ForEach(row, id: \.0) { code, color in
                            LineBadge(code: code, color: color, onColor: onColorFor(background: color), size: size)
                        }
                    }
                }
            }
        }
    }
}

private struct EveryEta: View {
    var body: some View {
        VStack(alignment: .trailing, spacing: Tokens.Spacing.small) {
            ForEach(Array(EtaCase.all.enumerated()), id: \.offset) { _, etaCase in
                EtaText(eta: etaCase.eta)
            }
        }
        .fixedSize()
        // Spanish, the app's default language, whatever the simulator's.
        .environment(\.locale, Locale(identifier: "es"))
    }
}
