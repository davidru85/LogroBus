import DesignSystem
import Shared
import SwiftUI

/// Near Me, the root of the Near Me tab (LB-201).
public struct NearMeView: View {
    @State private var viewModel: NearMeViewModel

    public init(viewModel: NearMeViewModel = NearMeViewModel()) {
        _viewModel = State(initialValue: viewModel)
    }

    public var body: some View {
        List {}
            .navigationTitle(Text("nearme.title", bundle: .module))
    }
}
