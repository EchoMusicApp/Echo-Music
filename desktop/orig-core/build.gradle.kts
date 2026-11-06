plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin { jvmToolchain(21) }

// Step 1 of the native port: compile the original app's own files that use no Android-only imports.
// The list is files.txt; grow it as files become portable.
sourceSets {
    main {
        kotlin {
            srcDir("../../app/src/main/kotlin")
            srcDir("src/main/kotlin")
            include(file("files.txt").readLines().filter { it.isNotBlank() } + "timber/**")
        }
    }
}

dependencies {
    implementation(project(":innertube-jvm"))
    implementation(project(":lrclib-jvm"))
    implementation(project(":kugou-jvm"))
    implementation(project(":betterlyrics-jvm"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.components.resources)
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
}
