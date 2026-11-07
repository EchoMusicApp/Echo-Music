plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin { jvmToolchain(21) }

// The original lyrics module and the small provider modules it uses, compiled unchanged as one plain JVM library.
sourceSets {
    main {
        kotlin.srcDir("../../lyrics/src/main/kotlin")
        kotlin.srcDir("../../unison/src/main/kotlin")
        kotlin.srcDir("../../simpmusic/src/main/kotlin")
        kotlin.srcDir("../../youlyplus/src/main/kotlin")
        kotlin.srcDir("../../paxsenixlyrics/src/main/kotlin")
    }
}

dependencies {
    api(project(":android-shim"))
    api(project(":core-jvm"))
    api(project(":lrclib-jvm"))
    api(project(":kugou-jvm"))
    api(project(":betterlyrics-jvm"))
    api(project(":innertube-jvm"))
    api(libs.ktor.client.core)
    api(libs.ktor.client.cio)
    api(libs.ktor.client.content.negotiation)
    api(libs.ktor.serialization.json)
    api(libs.ktor.client.encoding)
    api(libs.kuromoji.ipadic)
    api(libs.tinypinyin)
    api(libs.json)
    api("com.google.dagger:hilt-core:2.60.1")
}
