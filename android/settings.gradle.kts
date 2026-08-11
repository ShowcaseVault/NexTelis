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
        // ("SIP/RTP client choice") for why this library was chosen. We use
        // the "no-video" build (group org.linphone.no-video) since NexTelis
        // only makes audio calls — meaningfully smaller than org.linphone's
        // full artifact, which bundles VP8/H264 video codecs we never use.
        maven {
            url = uri("https://download.linphone.org/maven_repository")
            content {
                includeGroup("org.linphone")
                includeGroup("org.linphone.no-video")
            }
        }
    }
}

rootProject.name = "NexTelis"
include(":app")
