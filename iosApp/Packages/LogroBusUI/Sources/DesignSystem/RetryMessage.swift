import Shared
import SwiftUI

/// FR-ERR-05: Retry, the way out of an Error or Empty state. A bordered, large button, so it is a full touch target
/// (NFR-A11Y-04), not a line of tinted text.
public struct RetryButton: View {
    private let action: () -> Void

    public init(action: @escaping () -> Void) {
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            Text("error.retry", bundle: .module)
        }
        .buttonStyle(.bordered)
        .controlSize(.large)
    }
}

extension ErrorReason {
    /// FR-ERR-05: the short reason an Error state gives (design_guidelines.md §9, keys `error.*`).
    public var message: Text {
        switch self {
        case .network: Text("error.network", bundle: .module)
        case .server: Text("error.server", bundle: .module)
        case .data: Text("error.data", bundle: .module)
        }
    }
}
