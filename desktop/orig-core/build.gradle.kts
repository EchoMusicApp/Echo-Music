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
            srcDir("../../core/src/main/kotlin") // only the files listed above (AiLogger)
            srcDir("src/main/kotlin")
            include(file("files.txt").readLines().filter { it.isNotBlank() } + listOf("echo/music/iad1tya/R.kt", "echo/music/iad1tya/RTable.kt", "echo/music/iad1tya/BuildConfig.kt", "androidx/compose/ui/res/**", "echo/music/iad1tya/ui/component/backdrop/**", "echo/music/iad1tya/ai/AiLogger.kt", "echo/music/iad1tya/widget/**", "echo/music/iad1tya/LocalProviders.kt", "echo/music/iad1tya/utils/DataStoreShim.kt"))
        }
    }
}

// App drawables as classpath resources (R.drawable ids -> drawable/<file>), prepared by tools/gen_res.py.
val genRes by tasks.registering(Exec::class) { commandLine("python3", "tools/gen_res.py") }
sourceSets { main { resources { srcDir("build/gen-res") } } }
tasks.named("processResources") { dependsOn(genRes) }

dependencies {
    implementation(project(":core-jvm"))
    implementation(project(":db-jvm"))
    implementation(project(":android-shim"))
    implementation(project(":lyrics-jvm"))
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
    implementation("com.valentinilk.shimmer:compose-shimmer:1.3.3")
    implementation("androidx.room:room-common-jvm:2.8.4")
    implementation("javax.inject:javax.inject:1")
    implementation(libs.ktor.client.okhttp)
    implementation("androidx.annotation:annotation-jvm:1.9.1")
    implementation("org.jetbrains:annotations:26.0.2")
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
                named("java") { option("lite") }
                create("kotlin") { option("lite") }
            }
        }
    }
}
