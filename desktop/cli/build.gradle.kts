plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

kotlin { jvmToolchain(21) }

application { mainClass.set("echo.desktop.MainKt") }

dependencies {
    implementation(project(":innertube-jvm"))
    implementation(libs.ktor.client.okhttp)
    runtimeOnly("org.slf4j:slf4j-simple:2.0.16")
}
