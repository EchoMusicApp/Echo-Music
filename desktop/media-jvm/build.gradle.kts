plugins { id("org.jetbrains.kotlin.jvm") }

kotlin { jvmToolchain(21) }

// The desktop player engine: a queue/state machine implementing the Media3-style Player, with a pluggable audio backend,
// plus the original Media3-based helper functions (MediaItemExt, PlayerExt) copied from core.
dependencies {
    api(project(":media3-compat"))
    api(project(":core-jvm"))
    api(project(":innertube-jvm"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}
