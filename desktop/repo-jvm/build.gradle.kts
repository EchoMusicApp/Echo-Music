plugins { id("org.jetbrains.kotlin.jvm") }

kotlin { jvmToolchain(21) }

// The original repository classes (core/repository) and their domain mappers, compiled unchanged on top of the desktop database.
sourceSets {
    main {
        kotlin.srcDir("../../core/src/main/kotlin")
        kotlin.include("echo/music/core/repository/**", "echo/music/core/mapping/**")
    }
}

dependencies {
    api(project(":db-jvm"))
    api("com.google.dagger:hilt-core:2.60.1")
}
