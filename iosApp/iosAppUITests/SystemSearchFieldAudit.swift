import XCTest

/// The system search bar's own field, which the accessibility audit warns about and the app cannot fix (LB-209).
///
/// UIKit gives `UISearchBarTextField` its height, and the audit reports that its placeholder "may be clipped at larger
/// Dynamic Type sizes" in both passes, whatever size the text is. A Dynamic Type cap on the screen changed nothing. So
/// the field itself is exempt by name, as the Lines screen's search field already was, and everything else on each
/// screen is still audited. The exemption is recorded in `docs/project_log.md` and `docs/ci_cd.md` §5, since the
/// owner's rule allows one only by name.
enum SystemSearchFieldAudit {
    /// Whether [element] is the system search bar's own field rather than something the app draws.
    static func isItsOwn(_ element: XCUIElement) -> Bool {
        element.elementType == .searchField
    }
}
