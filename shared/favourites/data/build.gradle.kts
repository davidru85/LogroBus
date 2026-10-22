plugins {
    id("logrobus.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.favourites.domain)
            implementation(projects.shared.core.database)
        }
    }
}
