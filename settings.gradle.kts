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
include(":core:core-network")
include(":core:core-database")

// Feature modules
include(":feature:feature-auth")
include(":feature:feature-feed")
