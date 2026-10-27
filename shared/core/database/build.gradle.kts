plugins {
    id("logrobus.kmp.library")
    alias(libs.plugins.sqldelight)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.common)
            implementation(libs.sqldelight.coroutines)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
        getByName("androidHostTest").dependencies {
            // An in-memory JDBC database for tests (ADR-0013).
            implementation(libs.sqldelight.sqlite.driver)
        }
    }
}

sqldelight {
    databases {
        create("LogroBusDatabase") {
            packageName.set("com.ruizurraca.logrobus.core.database")
        }
    }
}
