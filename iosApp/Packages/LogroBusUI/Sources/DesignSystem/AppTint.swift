import SwiftUI

public extension View {
    /// LB-406 (NFR-A11Y-01): the app's controls, picker values, alert buttons and selected tab in the accent, which
    /// reaches 4.5:1 on the system's light and dark backgrounds (`TokensTests`), where the system's blue reaches about
    /// 4:1 on white. The app's root sets it, and so do the snapshots, so they show what ships.
    func appTint() -> some View {
        tint(Tokens.Colors.accent)
    }
}
