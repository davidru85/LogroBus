plugins {
    `kotlin-dsl`
}

group = "com.ruizurraca.logrobus.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.kover.gradlePlugin)

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
        register("androidApplication") {
            id = "logrobus.android.application"
            implementationClass = "com.ruizurraca.logrobus.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "logrobus.android.library"
            implementationClass = "com.ruizurraca.logrobus.buildlogic.AndroidLibraryConventionPlugin"
        }
    }
}
