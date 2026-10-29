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

    @Test
    func lb011_anEtaInANarrowRowIsNeverTruncated() {
        // design_guidelines.md §8: a row narrower than the ETA lets it overflow; it never shows "…".
        let narrow = EtaText(eta: EtaCase.eta(EtaDisplayMinutes(minutes: 4), delay: EtaDelayLate(minutes: 3)))
            .frame(width: 24)
            .environment(\.locale, Locale(identifier: "es"))
            .padding(.horizontal, 64)
        assertSnapshot(of: Appearance.light.apply(to: narrow), as: Appearance.light.strategy)
    }
}

/// Light, dark, and 200 % text or more (`accessibility3`: 40 pt body text, against 17 pt).
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
        // Every pixel must match to the eye: the perceptual precision absorbs anti-aliasing between Xcode versions,
        // while a lost icon or strikethrough still fails. A fixed scale keeps sizes the same on any simulator.
        .image(
            precision: 0.9995,
            perceptualPrecision: 0.98,
            layout: .sizeThatFits,
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
        .fixedSize()
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
        // Spanish, the app's default language, whatever the simulator's; no pulse, which could catch "Now" mid-fade.
        .environment(\.locale, Locale(identifier: "es"))
        .environment(\.etaPulse, false)
    }
}
