import Foundation

extension Bundle {
    /// The design system's strings in `locale`'s language, so a view follows SwiftUI's environment locale, as in
    /// previews and snapshots. Without a catalogue for that language, the module's own choice. Cached per language,
    /// because every ETA looks it up on each redraw.
    static func designSystem(for locale: Locale) -> Bundle {
        guard let language = locale.language.languageCode?.identifier else { return .module }
        if let cached = languageBundles.object(forKey: language as NSString) { return cached }
        guard let path = Bundle.module.path(forResource: language, ofType: "lproj"),
              let bundle = Bundle(path: path)
        else { return .module }
        languageBundles.setObject(bundle, forKey: language as NSString)
        return bundle
    }
}

// NSCache is thread-safe, so views on any actor can share it.
nonisolated(unsafe) private let languageBundles = NSCache<NSString, Bundle>()
