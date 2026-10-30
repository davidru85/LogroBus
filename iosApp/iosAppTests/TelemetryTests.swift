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
        reporter.recordNonFatal(error: KotlinIllegalStateException(message: "q=Calle Portales"), area: .network)

        #expect(native.reports == ["breadcrumb ScreenView(screen=NEAR_ME)", "non-fatal in NETWORK"])
        // NFR-PRIV-02: a Swift vendor gets the error's type and frames, never its message.
        let error = native.errors.first as? RedactedThrowable
        #expect(error?.type.hasSuffix("IllegalStateException") == true)
        #expect(error?.frames.isEmpty == false)
        #expect(error?.message?.contains("Calle") == false)
    }

    @Test
    func lb017_swiftTelemetryGoesToInitKoinInPlatformTelemetry() {
        // The shape LogroBusApp passes to doInitKoin, with LB-409's vendor implementations in place of nil.
        let tracker = RecordingTracker()
        let reporter = RecordingReporter()

        let telemetry = PlatformTelemetry(analytics: tracker, crashReporter: reporter)

        #expect(telemetry.analytics === tracker)
        #expect(telemetry.crashReporter === reporter)
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
    private(set) var errors: [KotlinThrowable] = []

    func recordNonFatal(error: KotlinThrowable, area: CrashArea) {
        reports.append("non-fatal in \(area.name)")
        errors.append(error)
    }

    func breadcrumb(crumb: any Breadcrumb) {
        reports.append("breadcrumb \(crumb)")
    }
}
