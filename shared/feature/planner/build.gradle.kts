plugins {
    id("logrobus.kmp.library")
    // The data package's DTOs are @Serializable (api_spec.md §3, §4.7–4.8).
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.transit.domain)
            api(projects.shared.favourites.domain)
            api(projects.shared.core.presentation)
            implementation(projects.shared.core.network)
            implementation(projects.shared.core.location)
            implementation(projects.shared.core.telemetry)
            implementation(projects.shared.core.config)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            // The contract tests drive SourceClient over a mock engine and never call the source (AGENTS.md §9).
            implementation(libs.ktor.client.mock)
            // Contract tests decode the captures in fixtures/ through FixtureLoader (docs/testing_strategy.md §2).
            implementation(projects.shared.core.testing)
        }
    }
}
