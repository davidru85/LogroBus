import Shared
import SwiftUI

/// The root of a tab until its feature lands (LB-101, LB-201, LB-301): its title alone. The shell adds the More action.
struct TabRootPlaceholder: View {
    let destination: TopLevelDestination

    var body: some View {
        Color.clear
            .navigationTitle(Text(destination.title))
    }
}

#Preview {
    NavigationStack {
        TabRootPlaceholder(destination: .lines)
    }
}
