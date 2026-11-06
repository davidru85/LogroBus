@testable import FeaturePlanner
import Shared
import SwiftUI
import Testing
import UIKit

/// FR-PLAN-11 on iOS: the root reads the origin by itself when it shows, as the tab is opened.
@MainActor
struct PlannerViewTests {
    @Test
    func lb301_theRootReadsTheOriginWhenItAppears() async throws {
        let viewModel = PlannerViewModel(useCases: SharedDependencies.shared.preview().locationOn())
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 390, height: 844))
        window.rootViewController = UIHostingController(rootView: NavigationStack { PlannerView(viewModel: viewModel) })
        window.makeKeyAndVisible()
        defer { window.isHidden = true }

        // Nothing but the view asks: no test calls onEvent.
        try await waitUntil { viewModel.state.origin is PlaceFieldUiMyLocation }
    }
}
