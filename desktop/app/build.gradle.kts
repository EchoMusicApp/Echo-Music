plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(project(":innertube-jvm"))
    implementation(project(":lrclib-jvm"))
    implementation(project(":kugou-jvm"))
    implementation(project(":betterlyrics-jvm"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    // VLC bindings. The user needs VLC (64-bit) installed; vlcj finds it automatically.
    implementation("uk.co.caprica:vlcj:4.8.3")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.16")
}

// Build number, passed by CI (-PappVersion=N) from desktop/VERSION; shown in the window title.
val appVersion = (findProperty("appVersion") as String?) ?: "dev"

compose.desktop {
    application {
        mainClass = "echo.desktop.MainKt"
        jvmArgs("-Dechomusic.version=$appVersion")
        nativeDistributions {
            // CI drops a portable VLC engine into app/resources/windows/vlc; it is copied next to the app.
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            packageName = "EchoMusic"
            packageVersion = "0.1.0"
            // Network/extractor libraries pull in many JDK modules; ship them all to avoid runtime surprises.
            includeAllModules = true
        }
    }
}
