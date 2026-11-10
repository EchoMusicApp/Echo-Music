plugins { id("org.jetbrains.kotlin.jvm") }

kotlin { jvmToolchain(21) }

// The original queue classes (playback/queues + QueueExt) compiled unchanged against the desktop Media3 types.
sourceSets {
    main {
        kotlin.srcDir("../../playback/src/main/kotlin")
        kotlin.include("echo/music/iad1tya/playback/queues/**", "echo/music/iad1tya/playback/QueueExt.kt", "echo/music/iad1tya/playback/PlaybackCutoffGuard.kt", "echo/music/iad1tya/playback/SeekButtonTarget.kt")
    }
}

dependencies {
    api(project(":media-jvm"))
    api(project(":db-jvm"))
}
