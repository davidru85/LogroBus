import Shared
import SwiftUI

struct FavouriteLinesEditor: View {
    let rows: [any EditableFavouriteLineUi]
    let onReorder: ([Int32]) -> Void
    let onRemove: (Int32) -> Void

    var body: some View {
        EmptyView()
    }
}
