import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.net.URL

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
plugins {
    id("com.android.application")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobufPlugin)
}

val hasGoogleServicesConfig = file("google-services.json").exists()

if (hasGoogleServicesConfig) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "iad1tya.echo.music"
    compileSdk = 36
    ndkVersion = "27.0.12077973"


    defaultConfig {
        applicationId = "iad1tya.echo.music"
        minSdk = 26
        targetSdk = 36
        versionCode = 523
        versionName = "5.2.81"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        // LastFM API keys from GitHub Secrets
        val lastFmKey = localProperties.getProperty("LASTFM_API_KEY") ?: System.getenv("LASTFM_API_KEY") ?: ""
        val lastFmSecret = localProperties.getProperty("LASTFM_SECRET") ?: System.getenv("LASTFM_SECRET") ?: ""

        buildConfigField("String", "LASTFM_API_KEY", "\"$lastFmKey\"")
        buildConfigField("String", "LASTFM_SECRET", "\"$lastFmSecret\"")

        // GitHub OAuth keys
        val githubClientId = localProperties.getProperty("GH_CLIENT_ID") ?: System.getenv("GH_CLIENT_ID") ?: ""
        val githubClientSecret = localProperties.getProperty("GH_CLIENT_SECRET") ?: System.getenv("GH_CLIENT_SECRET") ?: ""
        buildConfigField("String", "GH_CLIENT_ID", "\"$githubClientId\"")
        buildConfigField("String", "GH_CLIENT_SECRET", "\"$githubClientSecret\"")

        buildConfigField("String", "FLOW_NEURO_BASE_URL", project.findProperty("FLOW_NEURO_BASE_URL")?.toString()?.let { "\"$it\"" } ?: "\"https://api.flowneuroengine.com\"")
        buildConfigField("String", "FLOW_NEURO_API_KEY", project.findProperty("FLOW_NEURO_API_KEY")?.toString()?.let { "\"$it\"" } ?: "\"\"")

//add nightly build label support
        val isNightly = project.hasProperty("nightly") && project.property("nightly") == "true"
        buildConfigField("Boolean", "IS_NIGHTLY", isNightly.toString())

        val discordApplicationId = "1518210534070292541"
        val discordApplicationIdLong = 1518210534070292541L
        val discordRedirectScheme = "discord-$discordApplicationId"

        buildConfigField("String", "DISCORD_APPLICATION_ID", "\"$discordApplicationId\"")
        buildConfigField("long", "DISCORD_APPLICATION_ID_LONG", "${discordApplicationIdLong}L")
        buildConfigField("String", "DISCORD_REDIRECT_SCHEME", "\"$discordRedirectScheme\"")
        manifestPlaceholders["discordRedirectScheme"] = discordRedirectScheme
    }


    flavorDimensions += listOf("abi", "variant")
    productFlavors {
        // FOSS variant (default) - F-Droid compatible, no Google Play Services
        create("foss") {
            dimension = "variant"
            isDefault = true
            buildConfigField("Boolean", "CAST_AVAILABLE", "false")
        }

        // GMS variant - with Google Cast support (requires Google Play Services)
        create("gms") {
            dimension = "variant"
            buildConfigField("Boolean", "CAST_AVAILABLE", "true")
        }

        create("universal") {
            dimension = "abi"
            buildConfigField("String", "ARCHITECTURE", "\"universal\"")
        }
        create("arm64") {
            dimension = "abi"
            buildConfigField("String", "ARCHITECTURE", "\"arm64\"")
            ndk { abiFilters.add("arm64-v8a") }
        }
        create("armeabi") {
            dimension = "abi"
            buildConfigField("String", "ARCHITECTURE", "\"armeabi\"")
            ndk { abiFilters.add("armeabi-v7a") }
        }
        create("x86") {
            dimension = "abi"
            buildConfigField("String", "ARCHITECTURE", "\"x86\"")
            ndk { abiFilters.add("x86") }
        }
        create("x86_64") {
            dimension = "abi"
            buildConfigField("String", "ARCHITECTURE", "\"x86_64\"")
            ndk { abiFilters.add("x86_64") }
        }
    }

    signingConfigs {
        create("persistentDebug") {
            storeFile = file("persistent-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            storeFile = file("keystore/release.keystore")
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
        getByName("debug") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storePassword = "android"
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = false
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "ARCHITECTURE", "\"release\"")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("String", "ARCHITECTURE", "\"debug\"")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        lintConfig = file("lint.xml")
        warningsAsErrors = false
        abortOnError = false
        checkDependencies = false
    }

    androidResources {
        generateLocaleConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += listOf(
                "**/libandroidx.graphics.path.so",
                "**/libdatastore_shared_counter.so"
            )
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/CONTRIBUTORS.md"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn"
        )
        suppressWarnings.set(false)
    }
}

dependencies {
    // Firebase - GMS flavor only (excluded from F-Droid / FOSS builds)
    "gmsImplementation"(platform("com.google.firebase:firebase-bom:34.15.0"))
    "gmsImplementation"("com.google.firebase:firebase-analytics")
    "gmsImplementation"("com.google.firebase:firebase-crashlytics")

    // Google Drive Sync - GMS flavor only
    "gmsImplementation"(libs.play.services.auth)
    "gmsImplementation"(libs.google.api.client.android)
    "gmsImplementation"(libs.google.api.services.drive) {
        exclude(group = "org.apache.httpcomponents")
    }


    implementation(libs.haze)
    implementation(libs.guava)
    implementation(libs.coroutines.guava)
    implementation(libs.concurrent.futures)

    implementation(libs.activity)
    implementation(libs.hilt.navigation)
    implementation(libs.datastore)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.animation)
    implementation(libs.compose.reorderable)

    implementation(libs.viewmodel)
    implementation(libs.viewmodel.compose)
    implementation(libs.lifecycle.process)

    implementation(libs.material3)
    implementation(libs.androidx.adaptive)
    implementation(libs.androidx.adaptive.layout)
    implementation(libs.androidx.adaptive.navigation)
    implementation(libs.palette)
    implementation(libs.materialKolor)
    implementation(libs.androidx.browser)

    implementation(libs.appcompat)

    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    implementation(libs.ucrop)

    implementation(libs.shimmer)

    implementation(libs.media3)
    implementation(libs.media3.session)
    implementation(libs.media3.hls)
    implementation(libs.media3.ui)
    implementation(libs.media3.okhttp)

    // Google Cast - only included in GMS flavor (not available in F-Droid/FOSS builds)
    "gmsImplementation"(libs.media3.cast)
    "gmsImplementation"(libs.mediarouter)
    "gmsImplementation"(libs.cast.framework)

    implementation(libs.room.runtime)
    implementation(libs.kuromoji.ipadic)
    implementation(libs.tinypinyin)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    implementation(libs.apache.lang3)

    implementation(libs.hilt)
    implementation(libs.jsoup)
    ksp(libs.hilt.compiler)

    implementation(project(":innertube"))
    implementation(project(":kugou"))
    implementation(project(":lrclib"))
    implementation(project(":betterlyrics"))
    implementation(project(":simpmusic"))
    implementation(project(":youlyplus"))
    implementation(project(":canvas"))
    implementation(project(":shazamkit"))
    implementation(project(":artistvideo"))
    implementation(project(":applecanvas"))
    implementation(project(":echomusiccanvas"))
    implementation(project(":paxsenixlyrics"))
    implementation(project(":unison"))


    implementation(libs.ktor.client.core)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)

    // Protobuf for message serialization (lite version for Android)
    implementation(libs.protobuf.javalite)
    implementation(libs.protobuf.kotlin.lite)

    coreLibraryDesugaring(libs.desugaring)
    implementation(libs.timber)
    implementation(libs.smoothCorner)
    implementation(libs.lottie.compose)
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation(libs.work.runtime.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.ffmpeg.kit.audio)

}
