import DesignSystem
import Shared
import SwiftUI

/// How to Get There, the root of its tab (LB-301). Until place search lands (LB-301 part 2), its origin and
/// destination fields only, which open nothing yet (owner decision, 2026-11-06).
public struct PlannerView: View {
    @State private var viewModel: PlannerViewModel
    @Environment(\.scenePhase) private var scenePhase

    public init(viewModel: PlannerViewModel = PlannerViewModel()) {
        _viewModel = State(initialValue: viewModel)
    }

    public var body: some View {
        List {
            Section {
                PlaceField(kind: .origin, field: viewModel.state.origin)
                PlaceField(kind: .destination, field: viewModel.state.destination)
            }
        }
        // The list's background, as the other roots'.
        .scrollContentBackground(.hidden)
        .background(Tokens.Colors.groupedBackground)
        .navigationTitle(Text("plan.title", bundle: .module))
        // FR-PLAN-11: each time the root shows, and back from the background, where Settings may have changed the
        // location permission.
        .onAppear { viewModel.onEvent(PlannerEventShown.shared) }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { viewModel.onEvent(PlannerEventShown.shared) }
        }
    }
}

/// One field: what it shows, or its hint when empty. A screen reader hears the field's name, then what it shows.
private struct PlaceField: View {
    enum Kind {
        case origin, destination
    }

    let kind: Kind
    let field: any PlaceFieldUi

    var body: some View {
        Label {
            shown
                // A hint in the secondary colour, as an empty text field's.
                .foregroundStyle(field is PlaceFieldUiEmpty ? Tokens.Colors.secondaryText : Color.primary)
        } icon: {
            // Decoration: the field's name says which it is.
            Image(systemName: kind == .origin ? "smallcircle.filled.circle" : "mappin.and.ellipse")
                .foregroundStyle(Tokens.Colors.secondaryText)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(name)
        .accessibilityValue(shown)
    }

    private var name: Text {
        switch kind {
        case .origin: Text("plan.origin", bundle: .module)
        case .destination: Text("plan.destination", bundle: .module)
        }
    }

    private var shown: Text {
        switch onEnum(of: field) {
        case .myLocation: Text("plan.origin.current", bundle: .module)
        case .empty:
            switch kind {
            case .origin: Text("plan.origin.hint", bundle: .module)
            case .destination: Text("plan.destination.hint", bundle: .module)
            }
        }
    }
}

#if DEBUG
#Preview("With location") {
    NavigationStack {
        PlannerView(viewModel: PlannerViewModel(useCases: SharedDependencies.shared.preview().locationOn()))
    }
}

#Preview("Without location, dark") {
    NavigationStack {
        PlannerView(viewModel: PlannerViewModel(useCases: SharedDependencies.shared.preview()))
    }
    .preferredColorScheme(.dark)
}
#endif
