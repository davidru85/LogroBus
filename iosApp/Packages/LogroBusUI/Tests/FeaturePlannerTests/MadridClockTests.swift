@testable import FeaturePlanner
import Foundation
import Shared
import Testing

/// AGENTS.md §5.5 on iOS: the departure picker opens at, and reads, the Madrid clock whatever the device's zone, in
/// summer and in winter time (LB-303). The zone set here is the process's default, so the tests run one at a time; it
/// moves the formatters' default zone, but not `TimeZone.current`, the simulator's, which is UTC on CI. A GMT calendar
/// in place of Madrid's fails the first two.
@Suite(.serialized)
struct MadridClockTests {
    @Test(arguments: ["America/New_York", "Asia/Tokyo"])
    func lb303_aPickedMoment_isReadOnTheMadridClock_whateverTheDeviceZone(zone: String) throws {
        try inDeviceZone(zone) {
            // 06:19 UTC on 22 Oct 2026 is 08:19 in Madrid (CEST); 07:19 UTC on 15 Jan 2027 is 08:19 there (CET).
            let summer = Date(timeIntervalSince1970: 1_792_649_940)
            let winter = Date(timeIntervalSince1970: 1_799_997_540)

            #expect(summer.departAt == PlannerEventDepartAt(year: 2026, month: 10, day: 22, hour: 8, minute: 19))
            #expect(winter.departAt == PlannerEventDepartAt(year: 2027, month: 1, day: 15, hour: 8, minute: 19))
        }
    }

    @Test(arguments: ["America/New_York", "Asia/Tokyo"])
    func lb303_thePickerOpensAtTheMadridTime_whateverTheDeviceZone(zone: String) throws {
        try inDeviceZone(zone) {
            let summer = MadridDateTime(year: 2026, month: 10, day: 22, hour: 8, minute: 19)
            let winter = MadridDateTime(year: 2027, month: 1, day: 15, hour: 8, minute: 19)

            #expect(summer.madridDate == Date(timeIntervalSince1970: 1_792_649_940))
            #expect(winter.madridDate == Date(timeIntervalSince1970: 1_799_997_540))
            // And back: the picked fields are the ones it opened at.
            #expect(summer.madridDate.departAt.hour == 8 && summer.madridDate.departAt.minute == 19)
        }
    }

    @Test(arguments: ["America/New_York", "Asia/Tokyo"])
    func lb303_anotherDaysDate_isItsMadridDay_inTheGivenLocale(zone: String) throws {
        try inDeviceZone(zone) {
            let time = MadridDateTime(year: 2027, month: 1, day: 15, hour: 0, minute: 10)

            #expect(time.dayAndMonth(locale: Locale(identifier: "es_ES")) == "15 ene")
            #expect(time.dayAndMonth(locale: Locale(identifier: "en_GB")) == "15 Jan")
        }
    }

    private func inDeviceZone(_ zone: String, _ body: () throws -> Void) throws {
        let deviceZone = NSTimeZone.default
        NSTimeZone.default = try #require(TimeZone(identifier: zone))
        defer { NSTimeZone.default = deviceZone }
        try body()
    }
}
