import Foundation
import Shared
import Testing

/// LB-017, ADR-0009: a Swift class can be the app's own telemetry, as LB-409's vendor implementations will be, and the
/// shared consent gates stand in front of it: nothing reaches it until the user consents (NFR-PRIV-03).
struct TelemetryTests {

    @Test
    func lb017_aSwiftTrackerReceivesEventsOnlyOnceTheUserConsents() {
        let native = RecordingTracker()
        let consent = MemoryConsentStore(initial: TelemetryConsent(analytics: .undecided, crashReports: .undecided))
        let tracker = ConsentGatedAnalyticsTracker(delegate: native, consentStore: consent)

        tracker.track(event: AnalyticsEventAppOpen.shared)
        #expect(native.events.isEmpty)

        consent.update(consent: TelemetryConsent(analytics: .granted, crashReports: .undecided))
        tracker.track(event: AnalyticsEventStopView(stopId: 152))

        #expect(native.events.map { String(describing: $0) } == ["StopView(stopId=152)"])
    }

    @Test
    func lb017_aSwiftReporterReceivesReportsOnlyOnceTheUserConsents() {
        let native = RecordingReporter()
        let consent = MemoryConsentStore(initial: TelemetryConsent(analytics: .undecided, crashReports: .undecided))
        let reporter = ConsentGatedCrashReporter(delegate: native, consentStore: consent)
        let crumb = BreadcrumbScreenView(screen: .nearMe)

        reporter.breadcrumb(crumb: crumb)
        #expect(native.reports.isEmpty)

        consent.update(consent: TelemetryConsent(analytics: .undecided, crashReports: .granted))
        reporter.breadcrumb(crumb: crumb)
        reporter.recordNonFatal(error: KotlinIllegalStateException(), area: .network)

        #expect(native.reports == ["breadcrumb ScreenView(screen=NEAR_ME)", "non-fatal in NETWORK"])
    }
}

/// Stand-ins for a vendor's telemetry. Kotlin interfaces export as Objective-C protocols, so they subclass NSObject.
private final class RecordingTracker: NSObject, AnalyticsTracker {
    private(set) var events: [any AnalyticsEvent] = []

    func track(event: any AnalyticsEvent) {
        events.append(event)
    }
}

private final class RecordingReporter: NSObject, CrashReporter {
    private(set) var reports: [String] = []

    func recordNonFatal(error: KotlinThrowable, area: CrashArea) {
        reports.append("non-fatal in \(area.name)")
    }

    func breadcrumb(crumb: any Breadcrumb) {
        reports.append("breadcrumb \(crumb)")
    }
}
