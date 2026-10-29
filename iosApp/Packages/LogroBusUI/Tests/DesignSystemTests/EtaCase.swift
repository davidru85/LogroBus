@testable import DesignSystem
import Shared

/// An ETA with what it shows and says in English and Spanish: Android's EtaTextTest cases.
struct EtaCase {
    let eta: EtaUi
    let english: EtaLines
    let englishSpoken: String
    let spanish: EtaLines
    let spanishSpoken: String

    static func eta(
        _ display: any EtaDisplay,
        lowConfidence: Bool = false,
        scheduled: Bool = false,
        delay: (any EtaDelay)? = nil,
        cancelled: Bool = false
    ) -> EtaUi {
        EtaUi(display: display, lowConfidence: lowConfidence, scheduled: scheduled, delay: delay, cancelled: cancelled)
    }

    // Computed, because Kotlin types are not Sendable and Swift 6 forbids them in stored statics.
    private static var now: EtaDisplayNow { .shared }
    private static var minutes: EtaDisplayMinutes { EtaDisplayMinutes(minutes: 4) }
    private static var clock: EtaDisplayClockTime { EtaDisplayClockTime(hour: 9, minute: 5) }

    // Every case of spec §5.7 that shows, and the combinations EtaUi allows. The screen and VoiceOver always agree.
    static var all: [EtaCase] { [
        EtaCase(eta: eta(now), english: lines("Now"), englishSpoken: "arriving now",
                spanish: lines("Ahora"), spanishSpoken: "llega ahora"),
        EtaCase(eta: eta(EtaDisplayMinutes(minutes: 1)), english: lines("1 min"), englishSpoken: "in 1 minute",
                spanish: lines("1 min"), spanishSpoken: "en 1 minuto"),
        EtaCase(eta: eta(minutes), english: lines("4 min"), englishSpoken: "in 4 minutes",
                spanish: lines("4 min"), spanishSpoken: "en 4 minutos"),
        EtaCase(eta: eta(clock), english: lines("09:05"), englishSpoken: "at 09:05",
                spanish: lines("09:05"), spanishSpoken: "a las 09:05"),
        // A Búho time: "a la una", in the singular.
        EtaCase(eta: eta(EtaDisplayClockTime(hour: 1, minute: 5)), english: lines("01:05"), englishSpoken: "at 01:05",
                spanish: lines("01:05"), spanishSpoken: "a la 01:05"),
        EtaCase(eta: eta(minutes, lowConfidence: true), english: lines("~4 min"),
                englishSpoken: "approximately in 4 minutes",
                spanish: lines("~4 min"), spanishSpoken: "aproximadamente en 4 minutos"),
        EtaCase(eta: eta(now, lowConfidence: true), english: lines("~Now"),
                englishSpoken: "arriving now, approximately",
                spanish: lines("~Ahora"), spanishSpoken: "llega ahora, aproximadamente"),
        EtaCase(eta: eta(clock, scheduled: true), english: lines("09:05", "Scheduled"),
                englishSpoken: "scheduled at 09:05",
                spanish: lines("09:05", "Programado"), spanishSpoken: "programado a las 09:05"),
        EtaCase(eta: eta(minutes, scheduled: true), english: lines("4 min", "Scheduled"),
                englishSpoken: "in 4 minutes, scheduled",
                spanish: lines("4 min", "Programado"), spanishSpoken: "en 4 minutos, programado"),
        // Scheduled hides the delay, on screen and in speech.
        EtaCase(eta: eta(clock, scheduled: true, delay: EtaDelayLate(minutes: 3)), english: lines("09:05", "Scheduled"),
                englishSpoken: "scheduled at 09:05",
                spanish: lines("09:05", "Programado"), spanishSpoken: "programado a las 09:05"),
        EtaCase(eta: eta(minutes, delay: EtaDelayLate(minutes: 3)), english: lines("4 min", "+3 min late"),
                englishSpoken: "in 4 minutes, 3 minutes late",
                spanish: lines("4 min", "+3 min tarde"), spanishSpoken: "en 4 minutos, 3 minutos tarde"),
        EtaCase(eta: eta(minutes, delay: EtaDelayEarly(minutes: 1)), english: lines("4 min", "1 min early"),
                englishSpoken: "in 4 minutes, 1 minute early",
                spanish: lines("4 min", "1 min antes"), spanishSpoken: "en 4 minutos, 1 minuto antes"),
        EtaCase(eta: eta(minutes, cancelled: true), english: lines("4 min", "Cancelled"), englishSpoken: "cancelled",
                spanish: lines("4 min", "Cancelado"), spanishSpoken: "cancelado")
    ] }

    private static func lines(_ value: String, _ secondary: String? = nil) -> EtaLines {
        EtaLines(value: value, secondary: secondary)
    }
}
