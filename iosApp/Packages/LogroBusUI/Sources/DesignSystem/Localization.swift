import Foundation

extension Bundle {
    /// The design system's strings in `locale`'s language, so a view follows SwiftUI's environment locale, as in
    /// previews and snapshots. Without a catalogue for that language, the module's own choice.
    static func designSystem(for locale: Locale) -> Bundle {
        guard let language = locale.language.languageCode?.identifier,
              let path = Bundle.module.path(forResource: language, ofType: "lproj"),
              let bundle = Bundle(path: path)
        else { return .module }
        return bundle
    }
}
