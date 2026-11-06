import CoreLocation
import UIKit

/// What the system says of the app's location authorization (FR-LOC-01).
public enum LocationAuthorizationStatus: Sendable {
    /// The system has not asked yet: its prompt shows.
    case notDetermined
    /// Denied, or restricted by Screen Time or a device profile. The row opens the app's page in Settings, where a
    /// denial can be undone; a restriction only its owner can lift.
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

/// Core Location's authorization, for the app. Create it on the main thread: Core Location calls its delegate on the
/// run loop of the thread that created the manager.
@MainActor
public final class CoreLocationAuthorization: NSObject, LocationAuthorizing, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private var continuations: [UUID: AsyncStream<Void>.Continuation] = [:]

    override public init() {
        super.init()
        manager.delegate = self
    }

    public var status: LocationAuthorizationStatus {
        switch manager.authorizationStatus {
        case .notDetermined: .notDetermined
        case .authorizedWhenInUse, .authorizedAlways: .granted
        case .denied, .restricted: .denied
        @unknown default: .denied
        }
    }

    public func requestWhenInUse() {
        manager.requestWhenInUseAuthorization()
    }

    public func openSettings() {
        guard let settings = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(settings)
    }

    public func changes() -> AsyncStream<Void> {
        let (changes, continuation) = AsyncStream<Void>.makeStream()
        let id = UUID()
        continuations[id] = continuation
        continuation.onTermination = { [weak self] _ in
            Task { @MainActor in self?.continuations[id] = nil }
        }
        return changes
    }

    // Also called once when the manager is created; a check then finds nothing new.
    nonisolated public func locationManagerDidChangeAuthorization(_: CLLocationManager) {
        MainActor.assumeIsolated {
            for continuation in continuations.values {
                continuation.yield()
            }
        }
    }
}
