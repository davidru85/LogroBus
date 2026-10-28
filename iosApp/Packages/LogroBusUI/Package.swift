// swift-tools-version: 6.0
import PackageDescription

// Shared is a static framework, so the app links the system libraries it calls: SQLite, for SQLDelight's native driver
// (ADR-0013). Every target that uses Shared carries the setting.
let linksShared: [LinkerSetting] = [.linkedLibrary("sqlite3")]

// LogroBusUI: the SwiftUI layer of the iOS app, one target per area (architecture.md §3.1 and §5.2).
let package = Package(
    name: "LogroBusUI",
    // Spanish (es-ES) is the source language; English ships too (AGENTS.md §7).
    defaultLocalization: "es",
    platforms: [.iOS(.v18)],
    products: [
        .library(
            name: "LogroBusUI",
            targets: [
                "DesignSystem",
                "MapUI",
                "FeatureLines",
                "FeatureStops",
                "FeatureNearMe",
                "FeaturePlanner",
                "FeatureSettings"
            ]
        ),
        // The Kotlin framework. The app links it once, through this package.
        .library(name: "Shared", targets: ["Shared"])
    ],
    targets: [
        // Built by Gradle and copied here by scripts/ios/generate-project.sh; never committed.
        .binaryTarget(name: "Shared", path: "Frameworks/Shared.xcframework"),
        .target(name: "DesignSystem"),
        .target(name: "MapUI", dependencies: ["DesignSystem"]),
        .target(name: "FeatureLines", dependencies: ["DesignSystem", "MapUI", "Shared"], linkerSettings: linksShared),
        .target(name: "FeatureStops", dependencies: ["DesignSystem", "MapUI", "Shared"], linkerSettings: linksShared),
        .target(name: "FeatureNearMe", dependencies: ["DesignSystem", "MapUI", "Shared"], linkerSettings: linksShared),
        .target(name: "FeaturePlanner", dependencies: ["DesignSystem", "MapUI", "Shared"], linkerSettings: linksShared),
        .target(name: "FeatureSettings", dependencies: ["DesignSystem", "MapUI", "Shared"], linkerSettings: linksShared)
    ]
)

// Swift ViewModel tests, on SharedDependencies.shared.preview() (architecture.md §5.2). The iosApp scheme runs them, so
// scripts/ios/test.sh does too. Kept apart from the list above, which holds one target per line.
package.targets.append(
    .testTarget(name: "FeatureSettingsTests", dependencies: ["FeatureSettings", "Shared"], linkerSettings: linksShared)
)
