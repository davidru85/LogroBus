import DesignSystem
import Shared
import SwiftUI

/// How to Get There, the root of its tab (LB-301). Stubbed until the green phase.
public struct PlannerView: View {
    @State private var viewModel: PlannerViewModel

    public init(viewModel: PlannerViewModel = PlannerViewModel()) {
        _viewModel = State(initialValue: viewModel)
    }

    public var body: some View {
        List {}
            .navigationTitle(Text("plan.title", bundle: .module))
    }
}
