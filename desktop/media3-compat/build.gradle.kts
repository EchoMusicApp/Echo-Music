plugins { id("org.jetbrains.kotlin.jvm") }

kotlin { jvmToolchain(21) }

// A small, desktop-only copy of the parts of Media3's `androidx.media3.common` API that the app uses (written in Java so
// `import androidx.media3.common.Player.REPEAT_MODE_ALL` and public fields work exactly as in the original code).
dependencies { api(project(":android-shim")) }
