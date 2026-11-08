plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin { jvmToolchain(21) }

// The original dsp-core models plus audio-dsp (equalizer, stereo widener, automix ducking, profile storage), compiled unchanged.
sourceSets {
    main {
        kotlin.srcDir("../../dsp-core/src/main/kotlin")
        kotlin.srcDir("../../audio-dsp/src/main/kotlin")
    }
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    api(project(":media3-compat"))
    api(project(":android-shim"))
    api("com.google.dagger:hilt-core:2.60.1")
}
