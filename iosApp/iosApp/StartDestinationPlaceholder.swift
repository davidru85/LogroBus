import Shared
import SwiftUI

/// Shows the label of the shared start destination (FR-NAV-02). LB-012 replaces it with the tab shell.
struct StartDestinationPlaceholder: View {
    var body: some View {
        Text(SharedDependencies.shared.startDestination.label)
    }
}

#Preview {
    StartDestinationPlaceholder()
}
