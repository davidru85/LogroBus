plugins {
    id("logrobus.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.common)
        }
        androidMain.dependencies {
            // The Fused Location Provider (architecture.md §5.1).
            implementation(libs.play.services.location)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.robolectric)
        }
    }
}

tasks.withType<Test>().configureEach {
    // Robolectric's Android 17 (API 37) sandbox reaches jdk.internal.access, which JDK 21 does not export.
    jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
}
