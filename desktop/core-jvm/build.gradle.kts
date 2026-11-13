plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin { jvmToolchain(21) }

// The original app's `core` (database entities, models, helpers) and `domain` modules, compiled on the JVM.
// Only files without Android-only imports are listed; lists grow as more become portable.
sourceSets {
    main {
        kotlin {
            srcDir("../../core/src/main/kotlin")
            srcDir("../../domain/src/main/kotlin")
            srcDir("src/main/kotlin")
            include(file("core-files.txt").readLines().filter { it.isNotBlank() } +
                file("domain-files.txt").readLines().filter { it.isNotBlank() } + "timber/**")
        }
    }
}

dependencies {
    api(project(":innertube-jvm"))
    api(project(":metadata"))
    api(compose.runtime)
    api(compose.ui)
    api(compose.animation)
    api("androidx.room:room-common-jvm:2.8.4")
    api("androidx.datastore:datastore-preferences-core-jvm:1.2.1")
    api("javax.inject:javax.inject:1")
    api("org.apache.commons:commons-lang3:3.18.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    api("com.google.protobuf:protobuf-javalite:4.36.2")
    api(libs.ktor.client.core)
    api(libs.ktor.serialization.json)
}
