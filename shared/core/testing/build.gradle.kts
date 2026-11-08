plugins {
    id("logrobus.kmp.library")
}

// Test support: only test configurations may use this module (ModuleRules). It ships the test-scope network guard
// (LB-003): `androidMain` holds the JDK `InetAddressResolverProvider` and its `META-INF/services` file, because those
// resources must reach the host test runtime classpath of every module — AGP publishes them as the module's java
// resources, which `computeClasspath` adds to the test classpath. Putting the guard in `androidHostTest` instead would
// keep it out of the JVM that runs the tests. Every module but this one receives it from `configureTestNetworkGuard`.
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.common)
        }
    }
}
