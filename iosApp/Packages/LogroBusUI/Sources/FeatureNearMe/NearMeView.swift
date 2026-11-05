import DesignSystem
import Shared
import SwiftUI

/// Near Me, the root of the Near Me tab (LB-201). Until the map (LB-202) and the sheet (LB-203) land, the "Near you"
/// section only, and only without location (owner decision, 2026-11-05).
public struct NearMeView: View {
    @State private var viewModel: NearMeViewModel
    @State private var isShowing = false
    @Environment(\.scenePhase) private var scenePhase

    public init(viewModel: NearMeViewModel = NearMeViewModel()) {
        _viewModel = State(initialValue: viewModel)
    }

    public var body: some View {
        List {
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
