@preconcurrency import Shared
import SwiftUI

/// FR-FAV-05: the limit a favourite toggle reached, if it did. A toggle that saved, removed, or failed reached none.
func limitReached(by toggle: AppResult<FavouriteToggle>?) -> Int? {
    guard let saved = toggle as? AppResultSuccess<FavouriteToggle>,
          let reached = saved.value as? FavouriteToggleLimitReached else { return nil }
    return Int(reached.limit)
}

extension View {
    /// FR-FAV-05: an alert that says why a star saved nothing while `limit` is set; dismissing it calls `onShown`, so
    /// it shows once. VoiceOver reads an alert as it appears.
    func favouriteLimitAlert(_ limit: Int?, onShown: @escaping () -> Void) -> some View {
        alert(
            Text("lines.favourites.limit \(limit ?? 0)", bundle: .module),
            isPresented: Binding(get: { limit != nil }, set: { if !$0 { onShown() } }),
            actions: {},
            message: { Text("lines.favourites.limit_hint", bundle: .module) }
        )
    }
}
