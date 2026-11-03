@testable import FeatureLines
import Testing

/// LB-110 (FR-FAV-02, NFR-A11Y): VoiceOver's "Move up" and "Move down" in the edit sheet, which reorder without a drag.
struct FavouriteLinesListTests {
    @Test
    func lb110_moveUpAndMoveDownMoveARowByOne() {
        let rows = ["10", "4", "B1"]

        #expect(FavouriteLinesList.moving(rows, at: 1, by: -1) == ["4", "10", "B1"])
        #expect(FavouriteLinesList.moving(rows, at: 1, by: 1) == ["10", "B1", "4"])
        #expect(FavouriteLinesList.moving(rows, at: 0, by: 1) == ["4", "10", "B1"])
        #expect(FavouriteLinesList.moving(rows, at: 2, by: -1) == ["10", "B1", "4"])
    }

    @Test
    func lb110_aMovePastEitherEndChangesNothing() {
        // The first row has no "Move up" and the last no "Move down", but a stale action must not reorder.
        let rows = ["10", "4", "B1"]

        #expect(FavouriteLinesList.moving(rows, at: 0, by: -1) == rows)
        #expect(FavouriteLinesList.moving(rows, at: 2, by: 1) == rows)
    }
}
