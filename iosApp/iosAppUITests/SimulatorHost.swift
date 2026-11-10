import XCTest

/// What every UI test in this bundle needs of the simulator itself, apart from what the screens show.
///
/// The app decides nothing about its own test host: a system notification can cover a screen, and a busy simulator can
/// refuse to deliver a synthesised touch. Both are conditions of CI's runners, met in PRs #58 and #59, and both are
/// handled here rather than at each call site.
extension XCTestCase {
    /// Taps [element], trying again when XCTest cannot deliver the event.
    ///
    /// `XCUIElement.tap()` raises "Failed to tap …: Timed out while synthesizing event" when the simulator is too busy
    /// to process the touch, and a retry then succeeds (PR #59, on a CI runner). The element is still there and still
    /// hittable; what failed is the delivery, so retrying is what the situation calls for, not a longer wait.
    @MainActor
    func tap(_ element: XCUIElement, attempts: Int = 3) {
        for attempt in 1...attempts {
            do {
                element.tap()
                return
            } catch {
                if attempt == attempts { XCTFail("Could not tap \(element): \(error)") }
                Thread.sleep(forTimeInterval: 1)
            }
        }
    }

    /// Clears a notification banner that the system draws over the app, such as the simulator's "Ready for Apple
    /// Intelligence" notice on a fresh device. The audit reads whatever covers the app, and on CI that banner produced
    /// "Potentially inaccessible text, no element" on About data: a text the audit cannot attribute because it belongs
    /// to SpringBoard, not to the screen (PR #58). The app decides nothing about the system's banners, so the test
    /// dismisses them before auditing, and swipes one away when it lingers.
    @MainActor
    func dismissSystemBanners() {
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        for _ in 0..<2 {
            let banner = springboard.otherElements.matching(identifier: "NotificationShortLookView").firstMatch
            guard banner.waitForExistence(timeout: 2) else { return }
            // A swipe up dismisses a banner; a tap would open its app, which is not what this test wants.
            banner.swipeUp(velocity: .fast)
            Thread.sleep(forTimeInterval: 0.5)
        }
    }
}
