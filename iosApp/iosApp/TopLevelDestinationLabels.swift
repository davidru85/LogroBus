import Foundation
import Shared

extension TopLevelDestination {
    /// The tab label (design_guidelines.md §2.1, keys `tab.*` in §9). Shared code carries the destination;
    /// iOS supplies the localised words (AGENTS.md §5, rule 4).
    var label: LocalizedStringResource {
        switch self {
        case .lines: "tab.lines"
        case .nearMe: "tab.near_me"
        case .planner: "tab.directions"
        }
    }

    /// The tab icon (design_guidelines.md §2.1), an SF Symbol.
    var systemImage: String {
        switch self {
        case .lines: "bus"
        case .nearMe: "location.circle"
        case .planner: "arrow.triangle.turn.up.right.diamond"
        }
    }
}
