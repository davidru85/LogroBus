import DesignSystem
import FeatureLines
import FeatureNearMe
import FeaturePlanner
import FeatureSettings
import FeatureStops
import Shared
import SwiftUI

/// The app shell (LB-012): the three tabs of FR-NAV-01, opening on Lines (FR-NAV-02), each with its own navigation
/// stack (FR-NAV-03). The system tab bar adopts Liquid Glass on iOS 26 and later.
struct AppShell: View {
    @State private var router = TabRouter()

    var body: some View {
        // The setter also runs when the selected tab is tapped again, which is how the router sees re-selections.
        TabView(selection: Binding(get: { router.selection }, set: { router.select($0) })) {
            ForEach(TopLevelDestination.allCases, id: \.self) { destination in
                Tab(value: destination) {
                    TabStack(destination: destination, router: router)
                } label: {
                    Label(destination.label, systemImage: destination.systemImage)
                }
            }
        }
    }
}

/// One tab's navigation stack: its root, with the More action (FR-NAV-04), and the screens it can push.
private struct TabStack: View {
    let destination: TopLevelDestination
    let router: TabRouter

    var body: some View {
        let path = Binding(get: { router.paths[destination, default: []] }, set: { router.paths[destination] = $0 })
        NavigationStack(path: path) {
            root
                .environment(\.scrollToTopRequests, router.scrollToTopRequests[destination, default: 0])
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        NavigationLink(value: Route.more) {
                            Label("more.action", systemImage: "gearshape")
                        }
                    }
                }
                .navigationDestination(for: Route.self) { route in
                    switch route {
                    case .line(let lineId):
                        LineDetailView(lineId: lineId) { stopId in
                            router.open(.stop(stopId), on: destination)
                        }
                    case .stop(let stopId): StopDetailView(stopId: stopId)
                    case .more: MoreView()
                    case .settings: SettingsView()
                    case .aboutData: AboutDataView()
                    }
                }
        }
    }

    /// The tab's own screen: the Lines list (LB-101), which opens a line's detail on this stack (LB-103), Near Me
    /// (LB-201), and How to Get There (LB-301).
    @ViewBuilder private var root: some View {
        switch destination {
        case .lines: LinesView { lineId in router.paths[destination, default: []].append(.line(lineId)) }
        case .nearMe: NearMeView()
        case .planner: PlannerView()
        }
    }
}

#Preview {
    AppShell()
}
