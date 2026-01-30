# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

## Kotlin Serialization
# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclasseswithmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclasseswithmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclasseswithmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-dontwarn javax.servlet.ServletContainerInitializer
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn org.slf4j.impl.StaticLoggerBinder

## Rules for NewPipeExtractor
-keep class org.schabi.newpipe.extractor.services.youtube.protos.** { *; }
-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.javascript.engine.** { *; }
-dontwarn org.mozilla.javascript.JavaToJSONConverters
-dontwarn org.mozilla.javascript.tools.**
-keep class javax.script.** { *; }
-dontwarn javax.script.**
-keep class jdk.dynalink.** { *; }
-dontwarn jdk.dynalink.**

## Logging (does not affect Timber)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    ## Leave in release builds
    #public static int i(...);
    #public static int w(...);
    #public static int e(...);
}

# Generated automatically by the Android Gradle plugin.
-dontwarn java.beans.BeanDescriptor
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor

# Keep all classes within the kuromoji package
-keep class com.atilika.kuromoji.** { *; }

## Queue Persistence Rules
# Keep queue-related classes to prevent serialization issues in release builds
-keep class iad1tya.echo.music.models.PersistQueue { *; }
-keep class iad1tya.echo.music.models.PersistPlayerState { *; }
-keep class iad1tya.echo.music.models.QueueData { *; }
-keep class iad1tya.echo.music.models.QueueType { *; }
-keep class iad1tya.echo.music.playback.queues.** { *; }

# Keep serialization methods for queue persistence
-keepclassmembers class * implements java.io.Serializable {
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
}

## UCrop Rules
-dontwarn com.yalantis.ucrop**
-keep class com.yalantis.ucrop** { *; }
-keep interface com.yalantis.ucrop** { *; }

## Firebase Rules
# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep Crashlytics
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Keep Analytics
-keep class com.google.firebase.analytics.** { *; }
-dontwarn com.google.firebase.analytics.**

## Hilt/Dagger Rules
-keepclasseswithmembers class * {
    @dagger.* <methods>;
}
-keep class dagger.* { *; }
-keep class javax.inject.* { *; }
-keep class * extends dagger.internal.Binding
-keep class * extends dagger.internal.ModuleAdapter
-keep class * extends dagger.internal.StaticInjection
-keepnames @dagger.Module class *
-keepclassmembers class * {
    @javax.inject.* <fields>;
    @javax.inject.* <init>(...);
}

## Hilt specific
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$ViewComponentBuilderEntryPoint
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel

## Media3/ExoPlayer Rules - Enhanced to prevent service crashes
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-keepclassmembers class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep ExoPlayer classes
-keep class com.google.android.exoplayer2.** { *; }
-keepclassmembers class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**

# Keep MediaSession and Player classes
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.common.Player { *; }
-keep class androidx.media3.common.Player$* { *; }
-keep interface androidx.media3.common.Player$* { *; }
-keep class androidx.media3.exoplayer.ExoPlayer { *; }
-keepclassmembers class androidx.media3.exoplayer.ExoPlayer { *; }

# Keep PlaybackException and error handling
-keep class androidx.media3.common.PlaybackException { *; }
-keepclassmembers class androidx.media3.common.PlaybackException { *; }

# Keep MediaItem and related classes
-keep class androidx.media3.common.MediaItem { *; }
-keep class androidx.media3.common.MediaItem$* { *; }
-keep class androidx.media3.common.MediaMetadata { *; }

# Keep legacy media support
-keep class androidx.media.** { *; }

## DataStore Rules
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

## Room Database Rules
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

## Compose Rules
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

## Coil Rules
-keep class coil.** { *; }
-dontwarn coil.**

## OkHttp/Retrofit Rules
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

## Keep App Classes
-keep class iad1tya.echo.music.** { *; }
-keep interface iad1tya.echo.music.** { *; }

## Keep all model classes
-keep class iad1tya.echo.music.models.** { *; }
-keep class iad1tya.echo.music.db.entities.** { *; }

## Keep service classes - Critical for preventing service crashes
-keep class * extends android.app.Service {
    <init>(...);
    void onCreate();
    void onDestroy();
    int onStartCommand(android.content.Intent, int, int);
    android.os.IBinder onBind(android.content.Intent);
}
-keep class * extends androidx.media3.session.MediaLibraryService {
    <init>(...);
    *** onCreate();
    *** onDestroy();
    *** onGetSession(...);
}
-keep class * extends androidx.media3.session.MediaSessionService {
    <init>(...);
    *** onCreate();
    *** onDestroy();
}

## Keep MusicService specifically to prevent crashes
-keep class iad1tya.echo.music.playback.MusicService {
    <init>();
    *** player;
    *** mediaSession;
    *** database;
    *** connectivityManager;
    *** connectivityObserver;
    *** onCreate();
    *** onDestroy();
    *** onBind(...);
    *** onPlayerError(...);
    public *** *;
}

## Keep ExoDownloadService
-keep class iad1tya.echo.music.playback.ExoDownloadService {
    <init>();
    *** onCreate();
    *** onDestroy();
    public *** *;
}

## Keep Binder classes
-keep class * extends android.os.Binder {
    <init>();
    public *** *;
}

## JSON Rules - Fix VerifyError
-keep class org.json.** { *; }
-keepclassmembers class org.json.** { *; }
-dontwarn org.json.**

## Innertube Rules
-keep class com.echo.innertube.** { *; }
-keep interface com.echo.innertube.** { *; }
-dontwarn com.echo.innertube.**

## Shazam Signature Rules
-keep class com.alexmercerind.audire.native.** { *; }
-keep interface com.alexmercerind.audire.native.** { *; }

## KuGou Rules
-keep class com.echo.kugou.** { *; }
-keep interface com.echo.kugou.** { *; }
-dontwarn com.echo.kugou.**
