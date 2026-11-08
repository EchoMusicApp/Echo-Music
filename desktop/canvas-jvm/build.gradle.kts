plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin { jvmToolchain(21) }

// The original canvas modules (animated artwork lookups), compiled unchanged as one plain JVM library.
sourceSets {
    main {
        kotlin.srcDir("../../canvas/src/main/kotlin")
        kotlin.srcDir("../../applecanvas/src/main/kotlin")
        kotlin.srcDir("../../echomusiccanvas/src/main/kotlin")
        kotlin.srcDir("../../artistvideo/src/main/kotlin")
        // only the lookup half of artistvideo; the player half needs Android video views
        kotlin.exclude("**/ArtistVideo.kt")
    }
}

dependencies {
    api(project(":core-jvm")) // Timber stand-in
    api(libs.ktor.client.core)
    api(libs.ktor.client.cio)
    api(libs.ktor.client.okhttp)
    api(libs.ktor.client.content.negotiation)
    api(libs.ktor.serialization.json)
    api(libs.ktor.client.encoding)
}
