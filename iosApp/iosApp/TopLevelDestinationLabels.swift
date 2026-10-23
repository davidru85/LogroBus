import Shared

extension TopLevelDestination {
    /// The tab label (design_guidelines.md §2.1, keys `tab.*` in §9). Shared code carries the destination;
    /// iOS supplies the localised words (AGENTS.md §5, rule 4).
    var label: LocalizedStringResource {
        switch self {
        case .lines: "tab.lines"
        case .nearMe: "tab.near_me"
        case .planner: "tab.directions"
        // Kotlin enums reach Swift as classes until SKIE maps them to Swift enums. A new destination fails the
        // Kotlin test lb012_thereAreThreeTopLevelDestinationsInTabOrder first, before this line can be reached.
        // TODO(LB-010): remove the default once SKIE exports TopLevelDestination as a Swift enum.
        default: fatalError("No tab label for \(name)")
        }
    }
}
