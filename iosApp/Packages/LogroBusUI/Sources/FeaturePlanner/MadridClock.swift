import Foundation
import Shared

// FR-PLAN-03 on iOS: the departure picker shows and reads the Madrid clock, whatever the device's zone and calendar
// (AGENTS.md §5.5); shared code reads the picked fields on that clock (`PlanDeparture.atMadrid`).

extension Calendar {
    /// Logroño's calendar, on which the departure picker shows and reads its times (AGENTS.md §5.5).
    static let madrid: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        // The identifier is the IANA database's own: it always resolves.
        calendar.timeZone = TimeZone(identifier: "Europe/Madrid") ?? .gmt
        return calendar
    }()
}

extension MadridDateTime {
    /// The day and its month in `locale`: "15 ene", "15 Jan". The fields are already on the Madrid clock, so one fixed
    /// zone carries them through the formatter unchanged.
    func dayAndMonth(locale: Locale) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .gmt
        guard let date = calendar.date(from: DateComponents(year: Int(year), month: Int(month), day: Int(day))) else {
            return ""
        }
        // Gregorian, as the fields and the picker are, whatever calendar the device is set to.
        let style = Date.FormatStyle(locale: locale, calendar: calendar, timeZone: .gmt)
        return date.formatted(style.day().month(.abbreviated))
    }

    /// This time on the Madrid calendar, for the system's picker. The fields come from an instant, so they always
    /// resolve.
    var madridDate: Date {
        let components = DateComponents(
            year: Int(year), month: Int(month), day: Int(day), hour: Int(hour), minute: Int(minute)
        )
        return Calendar.madrid.date(from: components) ?? .distantPast
    }
}

extension Date {
    /// "Depart at" this moment, read on the Madrid clock (FR-PLAN-03).
    var departAt: PlannerEventDepartAt {
        let calendar = Calendar.madrid
        func field(_ component: Calendar.Component) -> Int32 { Int32(calendar.component(component, from: self)) }
        return PlannerEventDepartAt(
            year: field(.year), month: field(.month), day: field(.day), hour: field(.hour), minute: field(.minute)
        )
    }
}
