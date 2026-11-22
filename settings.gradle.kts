pluginManagement {
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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Nexus"

include(":app")

// Core modules
include(":core:core-common")
include(":core:core-ui")
include(":core:core-datastore")
include(":core:core-firebase")
include(":core:core-storage")
include(":core:core-giphy")

// Feature modules
include(":feature:feature-auth")
include(":feature:feature-feed")
include(":feature:feature-post")
include(":feature:feature-profile")
include(":feature:feature-circles")
include(":feature:feature-messages")
