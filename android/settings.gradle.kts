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
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Linphone SDK — not published to Maven Central. See docs/FINDINGS.md
        // ("SIP/RTP client choice") for why this library was chosen.
        maven {
            url = uri("https://download.linphone.org/maven_repository")
            content { includeGroup("org.linphone") }
        }
    }
}

rootProject.name = "NexTelis"
include(":app")
