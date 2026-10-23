plugins {
    id("logrobus.android.application")
}

android {
    defaultConfig {
        // OQ-14: the store identifier, shared with iOS. It cannot change after the first release.
        applicationId = "com.ruizurraca.logrobus"
    }
}
