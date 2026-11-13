plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.google.devtools.ksp") version "2.3.12"
}

kotlin { jvmToolchain(21) }

// Original Room DAOs (adapted by tools/gen_dao.py) + a desktop MusicDatabase with a bundled SQLite driver.
val genDao by tasks.registering(Exec::class) {
    commandLine("python3", "tools/gen_dao.py")
}
sourceSets { main { kotlin { srcDir("build/gen-db"); srcDir("src/main/kotlin") } } }
tasks.matching { it.name.startsWith("ksp") || it.name == "compileKotlin" }.configureEach { dependsOn(genDao) }

ksp { arg("room.schemaLocation", layout.buildDirectory.dir("schemas").get().asFile.path) }

dependencies {
    api(project(":core-jvm"))
    api("androidx.room:room-runtime-jvm:2.8.4")
    api("androidx.sqlite:sqlite-bundled-jvm:2.6.2")
    ksp("androidx.room:room-compiler:2.8.4")
}
