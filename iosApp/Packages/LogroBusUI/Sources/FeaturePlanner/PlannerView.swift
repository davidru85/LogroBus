import DesignSystem
import Shared
import SwiftUI

/// How to Get There, the root of its tab (LB-301). Until place search lands (LB-301 part 2), its origin and
/// destination fields, which open nothing yet (owner decision, 2026-11-06), and the swap button (LB-304).
public struct PlannerView: View {
    @State private var viewModel: PlannerViewModel
    @Environment(\.scenePhase) private var scenePhase

    public init(viewModel: PlannerViewModel = PlannerViewModel()) {
        _viewModel = State(initialValue: viewModel)
    }

    public var body: some View {
        List {
            Section {
                PlaceFields(
                    origin: viewModel.state.origin,
                    destination: viewModel.state.destination,
                    onSwap: { viewModel.onEvent(PlannerEventSwap.shared) }
                )
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

/// The section's one row: both fields with the line between them, and the swap button at its trailing end, centred
/// between them (design_guidelines.md §3.3). A screen reader reads the fields, then the button.
private struct PlaceFields: View {
    let origin: any PlaceFieldUi
    let destination: any PlaceFieldUi
    let onSwap: () -> Void
    // One width for both icons, so that both texts start in line; it grows with the text.
    @ScaledMetric(relativeTo: .body) private var iconWidth: CGFloat = 28

    var body: some View {
        HStack(spacing: Tokens.Spacing.small) {
            VStack(alignment: .leading, spacing: 0) {
                PlaceField(kind: .origin, field: origin, iconWidth: iconWidth)
                // A hairline between the fields, from where their texts start.
                Divider().padding(.leading, iconWidth + Tokens.Spacing.medium)
                PlaceField(kind: .destination, field: destination, iconWidth: iconWidth)
            }
            .accessibilityElement(children: .contain)
            .accessibilitySortPriority(1)
            // FR-PLAN-08: one tap exchanges the origin and the destination. An icon, spoken as what it does.
            Button(action: onSwap) {
                Image(systemName: "arrow.up.arrow.down")
                    .frame(minWidth: Tokens.minTouchTarget, minHeight: Tokens.minTouchTarget)
                    .contentShape(Rectangle())
            }
            // Its own button: the row is not one.
            .buttonStyle(.borderless)
            .accessibilityLabel(Text("plan.swap", bundle: .module))
        }
        .accessibilityElement(children: .contain)
    }
}

/// One field: what it shows, or its hint when empty. A screen reader hears the field's name, then what it shows.
private struct PlaceField: View {
    enum Kind {
        case origin, destination
    }

    let kind: Kind
    let field: any PlaceFieldUi
    let iconWidth: CGFloat

    var body: some View {
        HStack(spacing: Tokens.Spacing.medium) {
            // Decoration: the field's name says which it is.
            Image(systemName: kind == .origin ? "smallcircle.filled.circle" : "mappin.and.ellipse")
                .foregroundStyle(Tokens.Colors.secondaryText)
                .frame(width: iconWidth)
            // The row's whole width, so the text wraps rather than ending at its own edge: the audit took a Label's
            // text for clipped, although it showed whole (as Near Me's row, LB-201).
            shown
                // A hint in the secondary colour, as an empty text field's.
                .foregroundStyle(field is PlaceFieldUiEmpty ? Tokens.Colors.secondaryText : Color.primary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .fixedSize(horizontal: false, vertical: true)
        }
        // A list row's height, as each field had its own row before the swap button joined them (LB-304).
        .padding(.vertical, Tokens.Spacing.small)
        .frame(minHeight: Tokens.minTouchTarget)
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
        case .myLocation: Text("plan.my_location", bundle: .module)
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

#Preview("Swapped") {
    let viewModel = PlannerViewModel(useCases: SharedDependencies.shared.preview().locationOn())
    viewModel.onEvent(PlannerEventShown.shared)
    viewModel.onEvent(PlannerEventSwap.shared)
    return NavigationStack { PlannerView(viewModel: viewModel) }
}

#Preview("Without location, dark") {
    NavigationStack {
        PlannerView(viewModel: PlannerViewModel(useCases: SharedDependencies.shared.preview()))
    }
    .preferredColorScheme(.dark)
}
#endif
