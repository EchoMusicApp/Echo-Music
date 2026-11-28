plugins {
  id("org.jetbrains.kotlin.jvm")
}

kotlin {
  jvmToolchain(21)
}

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  testImplementation(libs.junit)
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}
