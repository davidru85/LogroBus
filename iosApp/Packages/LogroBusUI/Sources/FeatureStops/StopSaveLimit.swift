@preconcurrency import Shared
import SwiftUI

extension View {
    /// FR-FAV-05: an alert that says why the star saved nothing while `limit` is set; dismissing it calls `onShown`, so
    /// it shows once. VoiceOver reads an alert as it appears.
    func stopSaveLimitAlert(_ limit: Int?, onShown: @escaping () -> Void) -> some View {
        modifier(StopSaveLimitAlert(limit: limit, onShown: onShown))
    }
}

private struct StopSaveLimitAlert: ViewModifier {
    // Swift `Int`, which is the `%lld` the key carries; Kotlin's `Int32` would interpolate as `%d` and find no key.
    let limit: Int?
    let onShown: () -> Void
    // The limit the alert shows: kept while it closes, when `limit` is already nil, so its title never reads 0.
    @State private var shownLimit = 0

    func body(content: Content) -> some View {
        content
            .onChange(of: limit, initial: true) { _, reached in if let reached { shownLimit = reached } }
            .alert(
                Text("stop.save.limit \(limit ?? shownLimit)", bundle: .module),
                isPresented: Binding(get: { limit != nil }, set: { if !$0 { onShown() } }),
                actions: {},
                message: { Text("stop.save.limit_hint", bundle: .module) }
            )
    }
}
