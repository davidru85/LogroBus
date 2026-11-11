import DesignSystem
import Shared
import SwiftUI

/// Near Me, the root of the Near Me tab (LB-201). Until the map (LB-202) and the sheet (LB-203) land, the "Near you"
/// section only, and only without location (owner decision, 2026-11-05).
public struct NearMeView: View {
    @State private var viewModel: NearMeViewModel
    @State private var isShowing = false
    @Environment(\.scenePhase) private var scenePhase

    private let onOpenStop: (Int) -> Void

    public init(viewModel: NearMeViewModel = NearMeViewModel(), onOpenStop: @escaping (Int) -> Void = { _ in }) {
        _viewModel = State(initialValue: viewModel)
        self.onOpenStop = onOpenStop
    }

    public var body: some View {
        List {
            if let section = viewModel.state.nearYou as? NearYouUiLocationOn, !section.stops.isEmpty {
                // LB-203 (FR-HOME-03, NFR-A11Y-04): the list is the map's equivalent, so every stop is reachable and
                // openable here without the map. The heading shows only with rows, so an empty section says nothing.
                Section {
                    ForEach(section.stops, id: \.stopId) { stop in
                        NearbyStopRow(stop: stop, isRefined: section.isRefined) { stopId in
                            onOpenStop(Int(stopId))
                        }
                    }
                } header: {
                    Text("nearme.nearby.title", bundle: .module)
                        .foregroundStyle(Tokens.Colors.secondaryText)
                }
            }
            if viewModel.state.nearYou is NearYouUiTurnOnLocation {
                Section {
                    // FR-NEAR-06: the one row of "Near you" without location, which opens the permission flow.
                    Button {
                        viewModel.turnOnLocation()
                    } label: {
                        HStack(spacing: Tokens.Spacing.medium) {
                            Image(systemName: "location")
                                .accessibilityHidden(true)
                            // The row's whole width, so the text wraps rather than ending at its own edge.
                            Text("nearme.nearby.turn_on_location", bundle: .module)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        .contentShape(Rectangle())
                    }
                } header: {
                    Text("nearme.nearby.title", bundle: .module)
                        .foregroundStyle(Tokens.Colors.secondaryText)
                }
            }
        }
        // The list's background, so the screen keeps its colour with the section and without it.
        .scrollContentBackground(.hidden)
        .background(Tokens.Colors.groupedBackground)
        .navigationTitle(Text("nearme.title", bundle: .module))
        .onAppear { isShowing = true }
        .onDisappear { isShowing = false }
        // FR-LOC-03: location runs while Near Me shows and the app is active. Another tab, a pushed screen or the
        // background cancels the task, which stops the updates; a check reads location access again.
        .task(id: LocationReading(checks: viewModel.checks, reading: isShowing && scenePhase == .active)) {
            guard isShowing && scenePhase == .active else { return }
            await viewModel.observe()
        }
        .task { await viewModel.followAuthorization() }
    }

    /// What starts a new reading of location access.
    private struct LocationReading: Equatable {
        let checks: Int
        let reading: Bool
    }
}

#if DEBUG
#Preview("Without location") {
    NavigationStack {
        NearMeView(
            viewModel: NearMeViewModel(useCases: SharedDependencies.shared.preview()) {
                PreviewLocationAuthorization()
            }
        )
    }
}

#Preview("Without location, dark") {
    NavigationStack {
        NearMeView(
            viewModel: NearMeViewModel(useCases: SharedDependencies.shared.preview()) {
                PreviewLocationAuthorization()
            }
        )
    }
    .preferredColorScheme(.dark)
}

#Preview("With location") {
    NavigationStack {
        NearMeView(
            viewModel: NearMeViewModel(useCases: SharedDependencies.shared.preview().locationOn()) {
                PreviewLocationAuthorization()
            }
        )
    }
}

/// A preview's authorization: never asked, and it neither prompts nor opens Settings.
@MainActor
private final class PreviewLocationAuthorization: LocationAuthorizing {
    let status = LocationAuthorizationStatus.notDetermined

    func requestWhenInUse() {}

    func openSettings() {}

    func changes() -> AsyncStream<Void> {
        AsyncStream { _ in }
    }
}
#endif

/// FR-HOME-03: one stop of "Near you", read as one utterance and opened with a tap (NFR-A11Y-02). A walked
/// distance shows the minutes API-08 gave; a straight line says the metres, because a walk it does not know must
/// not be invented.
private struct NearbyStopRow: View {
    let stop: NearbyStopRowUi
    let isRefined: Bool
    let onOpen: (Int32) -> Void
    @Environment(\.locale) private var locale

    var body: some View {
        Button {
            onOpen(stop.stopId)
        } label: {
            VStack(alignment: .leading, spacing: Tokens.Spacing.xSmall) {
                // Names are shown as delivered, never translated (AGENTS.md §7).
                Text(verbatim: stop.name)
                    .foregroundStyle(Tokens.Colors.onSurface)
                Text(verbatim: spokenDistance)
                    .font(.subheadline)
                    .foregroundStyle(Tokens.Colors.secondaryText)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        // One element, so VoiceOver reads the name and how far it is rather than two nodes in a row.
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(verbatim: "\(stop.name), \(spokenDistance)"))
        .accessibilityAddTraits(.isButton)
    }

    /// The walk's own minutes once API-08 answered, and the straight-line metres until then, resolved from the
    /// feature's own table: a `LocalizedStringKey` would look in the app's bundle, which holds no such key.
    ///
    /// The numbers reach the format as Swift `Int`, which is the `%lld` the keys carry; Kotlin's `Int32` would
    /// interpolate as `%d` and find no key at all.
    private var spokenDistance: String {
        if isRefined, let walk = stop.walkingMinutes {
            return String(localized: "nearme.nearby.walk_minutes \(Int(walk))", bundle: .module, locale: locale)
        }
        let metres = Int(stop.distanceMeters)
        return String(localized: "nearme.nearby.distance_meters \(metres)", bundle: .module, locale: locale)
    }
}
