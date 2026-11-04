plugins {
    id("logrobus.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.common)
            // FR-SET-01: the ETA format the user chose.
            api(projects.shared.core.preferences)
            implementation(libs.kotlinx.datetime)
        }
    }
}
