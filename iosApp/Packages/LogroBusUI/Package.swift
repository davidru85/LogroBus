// swift-tools-version: 6.0
import PackageDescription

// LogroBusUI: the SwiftUI layer of the iOS app, one target per area (architecture.md §3.1 and §5.2).
let package = Package(
    name: "LogroBusUI",
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
        .target(name: "FeatureLines", dependencies: ["DesignSystem", "MapUI", "Shared"]),
        .target(name: "FeatureStops", dependencies: ["DesignSystem", "MapUI", "Shared"]),
        .target(name: "FeatureNearMe", dependencies: ["DesignSystem", "MapUI", "Shared"]),
        .target(name: "FeaturePlanner", dependencies: ["DesignSystem", "MapUI", "Shared"]),
        .target(name: "FeatureSettings", dependencies: ["DesignSystem", "MapUI", "Shared"])
    ]
)
