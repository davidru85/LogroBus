import Testing

/// Waits on the main actor until `condition` holds, failing after `timeout`.
@MainActor
func waitUntil(
    timeout: Duration = .seconds(5),
    _ condition: () -> Bool,
    sourceLocation: SourceLocation = #_sourceLocation
) async throws {
    let deadline = ContinuousClock.now + timeout
    while !condition() {
        guard ContinuousClock.now < deadline else {
            Issue.record("The condition did not hold within \(timeout)", sourceLocation: sourceLocation)
            return
        }
        try await Task.sleep(for: .milliseconds(10))
    }
}
