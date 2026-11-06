@testable import FeaturePlanner
import Shared
import SwiftUI
import Testing
import UIKit

/// FR-PLAN-11 on iOS: the root reads the origin by itself when it shows, as the tab is opened. FR-PLAN-08: its swap
/// button, as VoiceOver finds and activates it, exchanges the fields.
@MainActor
struct PlannerViewTests {
    @Test
    func lb301_theRootReadsTheOriginWhenItAppears() async throws {
        let viewModel = PlannerViewModel(useCases: SharedDependencies.shared.preview().locationOn())
        let window = show(viewModel)
        defer { window.isHidden = true }

        // Nothing but the view asks: no test calls onEvent.
        try await waitUntil { viewModel.state.origin is PlaceFieldUiMyLocation }
    }

    @Test
    func lb304_theSwapButton_isSpokenAsSwapOriginAndDestination_andActivatingItSwaps() async throws {
        let viewModel = PlannerViewModel(useCases: SharedDependencies.shared.preview().locationOn())
        let window = show(viewModel)
        defer { window.isHidden = true }
        try await waitUntil { viewModel.state.origin is PlaceFieldUiMyLocation }

        let swap = try #require(element(labelled: "Swap origin and destination", in: window))
        #expect(swap.accessibilityTraits.contains(.button))
        #expect(swap.accessibilityActivate())

        try await waitUntil { viewModel.state.destination is PlaceFieldUiMyLocation }
        #expect(viewModel.state.origin is PlaceFieldUiEmpty)
    }

    /// The root in a window of its own, in English, as the app shows it.
    private func show(_ viewModel: PlannerViewModel) -> UIWindow {
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 390, height: 844))
        let root = NavigationStack { PlannerView(viewModel: viewModel) }.environment(\.locale, Locale(identifier: "en"))
        window.rootViewController = UIHostingController(rootView: root)
        window.makeKeyAndVisible()
        return window
    }

    /// The accessibility element labelled `label` under `object`, as an assistive technology walks the tree.
    private func element(labelled label: String, in object: NSObject) -> NSObject? {
        if object.accessibilityLabel == label { return object }
        var children: [NSObject] = (object as? UIView)?.subviews ?? []
        if let elements = object.accessibilityElements as? [NSObject] {
            children += elements
        } else {
            let count = object.accessibilityElementCount()
            if count != NSNotFound, count > 0 {
                children += (0..<count).compactMap { object.accessibilityElement(at: $0) as? NSObject }
            }
        }
        return children.lazy.compactMap { self.element(labelled: label, in: $0) }.first
    }
}
