import Shared
import SwiftUI

/// An arrival's ETA, drawn and spoken as design_guidelines.md §6.3 says. Its spoken label leaves out the line, which
/// the row's ``LineBadge`` says, so a row reads "Line 2, in 4 minutes".
public struct EtaText: View {
    private let eta: EtaUi

    public init(eta: EtaUi) {
        self.eta = eta
    }

    public var body: some View {
        EmptyView()
    }
}

/// What an ETA shows: its value, and the secondary line under it, if any.
struct EtaLines: Equatable {
    let value: String
    let secondary: String?
}

/// The words of an ETA, from the target's string catalogue, in the language of `bundle`.
enum EtaStrings {
    /// The faintest opacity of the "Now" pulse: 4.5:1 or more on every surface, as on Android.
    static let pulseMinOpacity = 0.55

    static func lines(_ eta: EtaUi, bundle: Bundle = .module, locale: Locale = .current) -> EtaLines {
        EtaLines(value: "", secondary: nil)
    }

    static func spoken(_ eta: EtaUi, bundle: Bundle = .module, locale: Locale = .current) -> String {
        ""
    }

    /// The opacity of "Now" while it pulses: still at 1 when Reduce Motion is on.
    static func nowOpacity(dimmed: Bool, reduceMotion: Bool) -> Double {
        dimmed ? pulseMinOpacity : 1
    }
}
