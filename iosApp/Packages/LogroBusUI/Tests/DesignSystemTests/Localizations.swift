import Foundation

/// The design system's strings in one language, from its compiled catalogue, whatever the simulator's language.
func designSystemBundle(_ language: String) -> Bundle {
    guard let bundle = moduleBundle.path(forResource: language, ofType: "lproj").flatMap(Bundle.init(path:)) else {
        fatalError("No \(language).lproj in the DesignSystem bundle")
    }
    return bundle
}

private let moduleBundle: Bundle = {
    // The test target reaches the design system's resource bundle by its SwiftPM name.
    let name = "LogroBusUI_DesignSystem.bundle"
    let candidates = [Bundle.main.resourceURL, Bundle(for: BundleToken.self).resourceURL]
        + Bundle.allBundles.map(\.resourceURL) + Bundle.allFrameworks.map(\.resourceURL)
    for case let url? in candidates {
        if let bundle = Bundle(url: url.appendingPathComponent(name)) { return bundle }
    }
    fatalError("No \(name) next to the tests")
}()

private final class BundleToken {}
