plugins { id("org.jetbrains.kotlin.jvm") }

kotlin { jvmToolchain(21) }

// Tiny desktop stand-ins for Android classes the original code imports (Context, SharedPreferences, LruCache, ...).
// Kept out of db-jvm on purpose: Room generates different code when Android classes are on its classpath.

dependencies {
    api(project(":core-jvm")) // settings helpers use its enum/DataStore classes
    api("org.json:json:20251224")
    api("com.belerweb:pinyin4j:2.5.1")
}
