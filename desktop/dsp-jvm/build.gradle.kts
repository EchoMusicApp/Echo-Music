plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin { jvmToolchain(21) }

// The original dsp-core module (equalizer data models), compiled unchanged.
sourceSets { main { kotlin.srcDir("../../dsp-core/src/main/kotlin") } }

dependencies { api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0") }
