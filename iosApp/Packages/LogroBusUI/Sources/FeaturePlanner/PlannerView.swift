import DesignSystem
import Shared
import SwiftUI

/// How to Get There, the root of its tab (LB-301). Until place search lands (LB-301 part 2), its origin and
/// destination fields, which open nothing yet (owner decision, 2026-11-06), the swap button (LB-304), and the
/// departure row with its date and time picker (LB-303).
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
            // FR-PLAN-03: when the trip starts, under where it goes.
            Section {
                DepartureMenu(departure: viewModel.state.departure) { viewModel.onEvent($0) }
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
        .sheet(item: picking) { shown in
            DepartureTimeSheet(picker: shown.picker) { viewModel.onEvent($0) }
        }
    }

    /// The date and time picker, while the state has it. An item, so the sheet keeps its content while it slides
    /// away after Done or Cancel. Swiped down, it closes without a time.
    private var picking: Binding<ShownPicker?> {
        Binding(
            get: { viewModel.state.departurePicker.map(ShownPicker.init) },
            set: { shown in
                if shown == nil { viewModel.onEvent(PlannerEventCancelDepartureTime.shared) }
            }
        )
    }
}

/// The picker as the sheet's item. One identity, whatever its start: "Leave now"'s moves with the clock, and a new
/// identity would present the sheet again.
private struct ShownPicker: Identifiable {
    let picker: DeparturePickerUi
    var id: Int { 0 }
}

/// FR-PLAN-03: the departure, "Leave now" or the picked time (design_guidelines.md §3.3). Its menu leaves now, or opens
/// the date and time picker.
private struct DepartureMenu: View {
    let departure: any DepartureUi
    let onEvent: (any PlannerEvent) -> Void
    @Environment(\.locale) private var locale

    var body: some View {
        let text = departureText(departure, locale: locale)
        Menu {
            Button { onEvent(PlannerEventLeaveNow.shared) } label: { Text("plan.leave_now", bundle: .module) }
            Button { onEvent(PlannerEventChooseDepartureTime.shared) } label: {
                Text("plan.depart_at_choose", bundle: .module)
            }
        } label: {
            HStack {
                // A menu centres its label's lines: wrapped at large sizes, they start at the row's edge instead.
                text
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                // Decoration, as the system's menu rows show: a menu opens.
                Image(systemName: "chevron.up.chevron.down")
                    .imageScale(.small)
                    .accessibilityHidden(true)
            }
            // The whole label opens the menu, the space before the chevrons included. The row is the list's, and so at
            // least 44 pt tall, which Xcode's audit checks on the root.
            .contentShape(Rectangle())
        }
        .accessibilityLabel(text)
    }
}

/// What the departure row says: "Leave now", or the picked time, "Depart at 08:19", "Depart tomorrow at 08:19" or
/// "Depart on 15 Jan at 08:19". The hour picks Spanish's article: "a la 01:05", "a las 08:19".
func departureText(_ departure: any DepartureUi, locale: Locale) -> Text {
    switch onEnum(of: departure) {
    case .now:
        return Text("plan.leave_now", bundle: .module)
    case .at(let departAt):
        let time = departAt.time
        let hour = Int(time.hour)
        // HH:mm on the Madrid clock, as the ETAs' clock times (spec §5.7).
        let clock = clockText(hour: hour, minute: Int(time.minute))
        switch departAt.relativeDay {
        case .today:
            return Text("plan.depart_today \(hour) \(clock)", bundle: .module)
        case .tomorrow:
            return Text("plan.depart_tomorrow \(hour) \(clock)", bundle: .module)
        case .other:
            return Text("plan.depart_on \(hour) \(time.dayAndMonth(locale: locale)) \(clock)", bundle: .module)
        }
    }
}

/// FR-PLAN-03: the system's date and time picker, open at the picker's start, on the Madrid clock whatever the device's
/// zone; no day before today can be picked, and any time can. Done departs at the picked time; Cancel, or a swipe down,
/// keeps the departure.
private struct DepartureTimeSheet: View {
    let onEvent: (any PlannerEvent) -> Void
    private let today: Date
    @State private var time: Date

    init(picker: DeparturePickerUi, onEvent: @escaping (any PlannerEvent) -> Void) {
        self.onEvent = onEvent
        let today = Calendar.madrid.startOfDay(for: picker.now.madridDate)
        self.today = today
        _time = State(initialValue: picker.start.madridDate)
    }

    var body: some View {
        NavigationStack {
            Form {
                DatePicker(selection: $time, in: today..., displayedComponents: [.date, .hourAndMinute]) {
                    Text("plan.departure_time", bundle: .module)
                }
                .datePickerStyle(.graphical)
            }
            .navigationTitle(Text("plan.departure_time", bundle: .module))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button { onEvent(PlannerEventCancelDepartureTime.shared) } label: {
                        Text("plan.departure_cancel", bundle: .module)
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button { onEvent(time.departAt) } label: { Text("plan.departure_done", bundle: .module) }
                }
            }
        }
        // The picker shows and reads the Madrid clock (AGENTS.md §5.5).
        .environment(\.timeZone, Calendar.madrid.timeZone)
        .environment(\.calendar, Calendar.madrid)
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

#Preview("Departing at a time") {
    let viewModel = PlannerViewModel(useCases: SharedDependencies.shared.preview().locationOn())
    viewModel.onEvent(PlannerEventShown.shared)
    viewModel.onEvent(PlannerEventDepartAt(year: 2027, month: 1, day: 15, hour: 8, minute: 19))
    return NavigationStack { PlannerView(viewModel: viewModel) }
}

#Preview("Choosing a time") {
    let viewModel = PlannerViewModel(useCases: SharedDependencies.shared.preview().locationOn())
    viewModel.onEvent(PlannerEventShown.shared)
    viewModel.onEvent(PlannerEventChooseDepartureTime.shared)
    return NavigationStack { PlannerView(viewModel: viewModel) }
}

#Preview("Without location, dark") {
    NavigationStack {
        PlannerView(viewModel: PlannerViewModel(useCases: SharedDependencies.shared.preview()))
    }
    .preferredColorScheme(.dark)
}
#endif
