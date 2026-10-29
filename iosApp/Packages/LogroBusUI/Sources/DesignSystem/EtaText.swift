import Shared
import SwiftUI

/// An arrival's ETA, drawn and spoken as design_guidelines.md §6.3 says. Its spoken label leaves out the line, which
/// the row's ``LineBadge`` says, so a row reads "Line 2, in 4 minutes".
public struct EtaText: View {
    private let eta: EtaUi
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.locale) private var locale
    @State private var dimmed = false

    public init(eta: EtaUi) {
        self.eta = eta
    }

    public var body: some View {
        let bundle = Bundle.designSystem(for: locale)
        let lines = EtaStrings.lines(eta, bundle: bundle, locale: locale)
        VStack(alignment: .trailing, spacing: 2) {
            HStack(spacing: Tokens.Spacing.xSmall) {
                // Never truncated (design_guidelines.md §8): the row gives way, not the ETA.
                Text(lines.value)
                    .fixedSize(horizontal: true, vertical: false)
                    .font(.headline.monospacedDigit())
                    .strikethrough(eta.cancelled)
                    .foregroundStyle(eta.scheduled || eta.cancelled ? Tokens.Colors.secondaryText : Color.primary)
                    .opacity(isNow ? EtaStrings.nowOpacity(dimmed: dimmed, reduceMotion: reduceMotion) : 1)
                // DD-09: a low-confidence ETA is marked, never hidden. The symbol scales with the text.
                if eta.lowConfidence {
                    Image(systemName: "info.circle")
                        .font(.subheadline)
                        .foregroundStyle(Tokens.Colors.secondaryText)
                }
            }
            if let secondary = lines.secondary {
                Text(secondary)
                    .fixedSize(horizontal: true, vertical: false)
                    .font(.caption)
                    .foregroundStyle(Tokens.Colors.secondaryText)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(EtaStrings.spoken(eta, bundle: bundle, locale: locale))
        .onAppear {
            guard isNow else { return }
            withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) { dimmed = true }
        }
    }

    private var isNow: Bool {
        eta.display is EtaDisplayNow && !eta.cancelled
    }
}

/// What an ETA shows: its value, and the secondary line under it, if any.
struct EtaLines: Equatable {
    let value: String
    let secondary: String?
}

/// The words of an ETA, from the target's string catalogue, in the language of `bundle`. The same rules as Android's
/// EtaText, so both platforms show and say the same.
enum EtaStrings {
    /// The faintest opacity of the "Now" pulse: 4.5:1 or more on every surface, as on Android.
    static let pulseMinOpacity = 0.7

    static func lines(_ eta: EtaUi, bundle: Bundle = .module, locale: Locale = .current) -> EtaLines {
        let value = switch onEnum(of: eta.display) {
        case .now:
            String(localized: "eta.now", bundle: bundle, locale: locale)
        case .minutes(let display):
            String(localized: "eta.minutes \(Int(display.minutes))", bundle: bundle, locale: locale)
        case .clockTime(let display):
            clockText(display)
        }
        let secondary: String? = if eta.cancelled {
            String(localized: "eta.cancelled", bundle: bundle, locale: locale)
        } else if eta.scheduled {
            String(localized: "eta.scheduled", bundle: bundle, locale: locale)
        } else {
            eta.delay.map { delayText($0, bundle: bundle, locale: locale) }
        }
        guard eta.lowConfidence else { return EtaLines(value: value, secondary: secondary) }
        return EtaLines(
            value: String(localized: "eta.approximate \(value)", bundle: bundle, locale: locale),
            secondary: secondary
        )
    }

    /// What VoiceOver says, with the same precedence as ``lines(_:bundle:locale:)``: cancelled, then scheduled (which
    /// hides a delay), then the delay.
    static func spoken(_ eta: EtaUi, bundle: Bundle = .module, locale: Locale = .current) -> String {
        if eta.cancelled { return String(localized: "eta.cancelled_spoken", bundle: bundle, locale: locale) }
        let display = onEnum(of: eta.display)
        let time = timeSpoken(display, eta: eta, bundle: bundle, locale: locale)
        let approximate = if eta.lowConfidence, !isNow(display) {
            String(localized: "eta.approximately_spoken \(time)", bundle: bundle, locale: locale)
        } else {
            time
        }
        let qualifier: String? = if eta.scheduled {
            // A scheduled clock time already says "scheduled at".
            isClock(display) ? nil : String(localized: "eta.scheduled_short_spoken", bundle: bundle, locale: locale)
        } else {
            eta.delay.map { delaySpoken($0, bundle: bundle, locale: locale) }
        }
        guard let qualifier else { return approximate }
        return String(localized: "eta.spoken_join \(approximate) \(qualifier)", bundle: bundle, locale: locale)
    }

    /// The opacity of "Now" while it pulses: still at 1 when Reduce Motion is on.
    static func nowOpacity(dimmed: Bool, reduceMotion: Bool) -> Double {
        dimmed && !reduceMotion ? pulseMinOpacity : 1
    }

    /// Whether an ETA pulses: a live "Now", whenever it becomes one, unless Reduce Motion is on.
    static func pulses(_ eta: EtaUi, reduceMotion: Bool) -> Bool {
        false
    }

    private static func timeSpoken(
        _ display: Skie.Presentation.EtaDisplay.__Sealed, eta: EtaUi, bundle: Bundle, locale: Locale
    ) -> String {
        switch display {
        case .now:
            if eta.lowConfidence {
                return String(localized: "eta.now_approximately_spoken", bundle: bundle, locale: locale)
            }
            return String(localized: "eta.now_spoken", bundle: bundle, locale: locale)
        case .minutes(let minutes):
            return String(localized: "eta.minutes_spoken \(Int(minutes.minutes))", bundle: bundle, locale: locale)
        case .clockTime(let clock):
            // The hour picks Spanish's article: "a la 01:05", "a las 09:05".
            let hour = Int(clock.hour)
            let text = clockText(clock)
            if eta.scheduled {
                return String(localized: "eta.scheduled_spoken \(hour) \(text)", bundle: bundle, locale: locale)
            }
            return String(localized: "eta.clock_spoken \(hour) \(text)", bundle: bundle, locale: locale)
        }
    }

    private static func delayText(_ delay: any EtaDelay, bundle: Bundle, locale: Locale) -> String {
        switch onEnum(of: delay) {
        case .late(let late): String(localized: "eta.late \(Int(late.minutes))", bundle: bundle, locale: locale)
        case .early(let early): String(localized: "eta.early \(Int(early.minutes))", bundle: bundle, locale: locale)
        }
    }

    private static func delaySpoken(_ delay: any EtaDelay, bundle: Bundle, locale: Locale) -> String {
        switch onEnum(of: delay) {
        case .late(let late): String(localized: "eta.late_spoken \(Int(late.minutes))", bundle: bundle, locale: locale)
        case .early(let early):
            String(localized: "eta.early_spoken \(Int(early.minutes))", bundle: bundle, locale: locale)
        }
    }

    // Spec §5.7: HH:mm on the Madrid clock, the same digits in every locale.
    private static func clockText(_ clock: EtaDisplayClockTime) -> String {
        String(format: "%02d:%02d", clock.hour, clock.minute)
    }

    private static func isNow(_ display: Skie.Presentation.EtaDisplay.__Sealed) -> Bool {
        if case .now = display { true } else { false }
    }

    private static func isClock(_ display: Skie.Presentation.EtaDisplay.__Sealed) -> Bool {
        if case .clockTime = display { true } else { false }
    }
}
