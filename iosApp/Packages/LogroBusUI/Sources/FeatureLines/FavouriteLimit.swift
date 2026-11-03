@preconcurrency import Shared

/// FR-FAV-05: the limit a favourite toggle reached, if it did. A toggle that saved, removed, or failed reached none.
func limitReached(by toggle: AppResult<FavouriteToggle>?) -> Int? {
    guard let saved = toggle as? AppResultSuccess<FavouriteToggle>,
          let reached = saved.value as? FavouriteToggleLimitReached else { return nil }
    return Int(reached.limit)
}
