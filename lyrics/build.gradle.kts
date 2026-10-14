plugins {
  id("com.android.library")
  id("com.google.devtools.ksp")
}

android {
  namespace = "com.music.echo.lyrics"
  compileSdk = 37
  defaultConfig { minSdk = 26 }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
}

kotlin { jvmToolchain(21) }

dependencies {
  implementation(project(":core"))
  implementation(project(":unison"))
  implementation(project(":lrclib"))
  implementation(project(":kugou"))
  implementation(project(":simpmusic"))
  implementation(project(":youlyplus"))
  implementation(project(":betterlyrics"))
  implementation(project(":paxsenixlyrics"))
  implementation(project(":innertube"))

  implementation(libs.kuromoji.ipadic)
  implementation(libs.tinypinyin)
  implementation("androidx.core:core-ktx:1.13.1")
  implementation(libs.hilt)
  ksp(libs.hilt.compiler)
}

dependencies {
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.okhttp)
  implementation(libs.ktor.client.content.negotiation)
  implementation(libs.ktor.serialization.json)
}
