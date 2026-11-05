import FeatureNearMe

/// Stands in for Core Location's authorization: it counts the prompts and the trips to Settings, and the test answers.
@MainActor
final class FakeLocationAuthorization: LocationAuthorizing {
    private(set) var status: LocationAuthorizationStatus
    private(set) var prompts = 0
    private(set) var settingsOpened = 0
    private var continuations: [AsyncStream<Void>.Continuation] = []

    init(status: LocationAuthorizationStatus = .notDetermined) {
        self.status = status
    }

    func requestWhenInUse() {
        prompts += 1
    }

    func openSettings() {
        settingsOpened += 1
    }

    func changes() -> AsyncStream<Void> {
        let (changes, continuation) = AsyncStream<Void>.makeStream()
        continuations.append(continuation)
        return changes
    }

    /// The user answers the prompt, or changes the authorization in Settings.
    func change(to status: LocationAuthorizationStatus) {
        self.status = status
        continuations.forEach { $0.yield() }
    }
}
