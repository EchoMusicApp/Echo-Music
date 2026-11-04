// Desktop (Windows/Linux/macOS) build for Echo Music.
// Separate from the Android build: reuses the platform-neutral modules' sources as-is.
rootProject.name = "echomusic-desktop"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
        maven { setUrl("https://jitpack.io") }
    }
    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

include(":metadata", ":innertube-jvm", ":lrclib-jvm", ":cli", ":app")
project(":metadata").projectDir = file("../metadata")
