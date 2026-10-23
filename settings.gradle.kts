pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "LogroBus"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":shared:core:common")
include(":shared:core:config")
include(":shared:core:telemetry")
include(":shared:core:network")
include(":shared:core:database")
include(":shared:core:location")
include(":shared:core:polling")
include(":shared:core:presentation")
include(":shared:core:testing")
include(":shared:transit:domain")
include(":shared:transit:data")
include(":shared:favourites:domain")
include(":shared:favourites:data")
include(":shared:feature:lines")
include(":shared:feature:stops")
include(":shared:feature:nearme")
include(":shared:feature:planner")
include(":shared:feature:settings")
include(":shared:umbrella")

include(":android:app")
include(":android:core:designsystem")
include(":android:core:map")
include(":android:core:navigation")
include(":android:feature:lines")
include(":android:feature:stops")
include(":android:feature:nearme")
include(":android:feature:planner")
include(":android:feature:settings")

include(":tools:architecture-tests")
