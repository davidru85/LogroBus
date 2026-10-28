plugins {
    id("logrobus.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.common)
            implementation(libs.kotlinx.datetime)
        }
    }
}
