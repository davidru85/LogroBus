import CoreLocation
import UIKit

/// What the system says of the app's location authorization (FR-LOC-01).
public enum LocationAuthorizationStatus: Sendable {
    /// The system has not asked yet: its prompt shows.
    case notDetermined
    /// Denied or restricted: only the system's Settings can turn location on.
    case denied
    /// When In Use, approximate or precise.
    case granted
}

/// The system's location authorization, which only the UI asks for (architecture.md §5.2): the shared location
/// provider reads it, and never asks.
@MainActor
public protocol LocationAuthorizing: AnyObject {
    var status: LocationAuthorizationStatus { get }
    /// The system's prompt for When In Use authorization. Never Always (security_privacy.md §3).
    func requestWhenInUse()
    /// The app's page in the system's Settings.
    func openSettings()
    /// Yields each time the authorization changes, until cancelled.
    func changes() -> AsyncStream<Void>
}

/// Core Location's authorization, for the app.
@MainActor
public final class CoreLocationAuthorization: NSObject, LocationAuthorizing {
    public var status: LocationAuthorizationStatus { .notDetermined }

    override public init() {
        super.init()
    }

    public func requestWhenInUse() {}

    public func openSettings() {}

    public func changes() -> AsyncStream<Void> {
        AsyncStream { $0.finish() }
    }
}
