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
        modifier(FavouriteLimitAlert(limit: limit, onShown: onShown))
    }
}

private struct FavouriteLimitAlert: ViewModifier {
    let limit: Int?
    let onShown: () -> Void
    // The limit the alert shows: kept while it closes, when `limit` is already nil, so its title never reads 0.
    @State private var shownLimit = 0

    func body(content: Content) -> some View {
        content
            .onChange(of: limit, initial: true) { _, reached in if let reached { shownLimit = reached } }
            .alert(
                Text("lines.favourites.limit \(limit ?? shownLimit)", bundle: .module),
                isPresented: Binding(get: { limit != nil }, set: { if !$0 { onShown() } }),
                actions: {},
                message: { Text("lines.favourites.limit_hint", bundle: .module) }
            )
    }
}
