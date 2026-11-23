# Keep classes and constructors looked up from jni_bridge.cpp.
-keep class echo.music.usbaudio.model.DacFormat { <init>(...); }
-keep class echo.music.usbaudio.model.DacCapabilities { <init>(...); }

# Keep names used by the exported JNI method symbols.
-keepclasseswithmembernames class echo.music.usbaudio.UsbAudioDriver { native <methods>; }
