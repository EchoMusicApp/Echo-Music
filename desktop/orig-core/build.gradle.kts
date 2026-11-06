plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("com.google.protobuf")
}

kotlin { jvmToolchain(21) }

// Step 1 of the native port: compile the original app's own files that use no Android-only imports.
// The list is files.txt; grow it as files become portable.
sourceSets {
    main {
        kotlin {
            srcDir("../../app/src/main/kotlin")
            srcDir("src/main/kotlin")
            include(file("files.txt").readLines().filter { it.isNotBlank() } + listOf("timber/**", "echo/music/iad1tya/BuildConfig.kt"))
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
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("org.json:json:20240303")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.google.dagger:hilt-core:2.60.1")
    implementation("com.google.protobuf:protobuf-javalite:4.36.2")
    implementation("com.google.protobuf:protobuf-kotlin-lite:4.36.2")
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
}

sourceSets { main { proto { srcDir("../../app/src/main/proto") } } }

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:4.36.2" }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") { option("lite") }
                create("kotlin") { option("lite") }
            }
        }
    }
}
