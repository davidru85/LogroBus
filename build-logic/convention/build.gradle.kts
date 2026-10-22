plugins {
    `kotlin-dsl`
}

group = "com.ruizurraca.logrobus.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "logrobus.kmp.library"
            implementationClass = "com.ruizurraca.logrobus.buildlogic.KmpLibraryConventionPlugin"
        }
    }
}
