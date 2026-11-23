#include <jni.h>
#include <android/log.h>
#include <string>

#define LOG_TAG "usbaudio_driver"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#include "spsc_ring_buffer.h"
#include "descriptor_parser.h"
#include "usb_stream_engine.h"

// Native driver version constant — must match UsbAudioDriver.kt JVM fallback.
static constexpr const char* NATIVE_DRIVER_VERSION = "1.0.0-usbaudio";

static echo::music::usbaudio::SpscRingBuffer g_test_ring_buffer(echo::music::usbaudio::SpscRingBuffer::DEFAULT_CAPACITY);
static echo::music::usbaudio::UsbStreamEngine g_stream_engine;

extern "C" {

JNIEXPORT jstring JNICALL
Java_echo_music_usbaudio_UsbAudioDriver_nativeGetVersion(JNIEnv* env, jobject /* thiz */) {
    LOGI("nativeGetVersion called");
    return env->NewStringUTF(NATIVE_DRIVER_VERSION);
}

JNIEXPORT jint JNICALL
Java_echo_music_usbaudio_UsbAudioDriver_nativeTestRingBufferWrite(JNIEnv* env, jobject /* thiz */, jbyteArray data) {
    if (!data) return 0;
    jsize len = env->GetArrayLength(data);
    if (len == 0) return 0;

    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    size_t written = g_test_ring_buffer.write(reinterpret_cast<const uint8_t*>(bytes), static_cast<size_t>(len));
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);

    return static_cast<jint>(written);
}

JNIEXPORT jbyteArray JNICALL
Java_echo_music_usbaudio_UsbAudioDriver_nativeTestRingBufferRead(JNIEnv* env, jobject /* thiz */, jint size) {
    if (size <= 0) return env->NewByteArray(0);

    std::vector<uint8_t> temp(static_cast<size_t>(size));
    size_t read_bytes = g_test_ring_buffer.read(temp.data(), static_cast<size_t>(size));

    jbyteArray result = env->NewByteArray(static_cast<jsize>(read_bytes));
    if (read_bytes > 0) {
        env->SetByteArrayRegion(result, 0, static_cast<jsize>(read_bytes), reinterpret_cast<const jbyte*>(temp.data()));
    }
    return result;
}

JNIEXPORT void JNICALL
Java_echo_music_usbaudio_UsbAudioDriver_nativeTestRingBufferFlush(JNIEnv* /* env */, jobject /* thiz */) {
    g_test_ring_buffer.flush();
}

JNIEXPORT jobject JNICALL
Java_echo_music_usbaudio_UsbAudioDriver_nativeParseDescriptors(JNIEnv* env, jobject /* thiz */, jbyteArray descriptors) {
    echo::music::usbaudio::ParsedDacCapabilities caps;
    jsize len = descriptors ? env->GetArrayLength(descriptors) : 0;
    if (len > 0) {
        jbyte* bytes = env->GetByteArrayElements(descriptors, nullptr);
        caps = echo::music::usbaudio::DescriptorParser::parse(reinterpret_cast<const uint8_t*>(bytes), static_cast<size_t>(len));
        env->ReleaseByteArrayElements(descriptors, bytes, JNI_ABORT);
    }

    // Build List<DacFormat>
    jclass listClass = env->FindClass("java/util/ArrayList");
    jmethodID listInit = env->GetMethodID(listClass, "<init>", "()V");
    jmethodID listAdd = env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z");
    jobject formatsList = env->NewObject(listClass, listInit);

    jclass integerClass = env->FindClass("java/lang/Integer");
    jmethodID integerValueOf = env->GetStaticMethodID(integerClass, "valueOf", "(I)Ljava/lang/Integer;");

    jclass dacFormatClass = env->FindClass("echo/music/usbaudio/model/DacFormat");
    jmethodID dacFormatInit = env->GetMethodID(dacFormatClass, "<init>",
        "(IIILjava/lang/Integer;IIILjava/util/List;)V");

    for (const auto& fmt : caps.supportedFormats) {
        jobject ratesList = env->NewObject(listClass, listInit);
        for (uint32_t r : fmt.sampleRates) {
            jobject rateObj = env->CallStaticObjectMethod(integerClass, integerValueOf, static_cast<jint>(r));
            env->CallBooleanMethod(ratesList, listAdd, rateObj);
            env->DeleteLocalRef(rateObj);
        }

        jobject syncEpObj = nullptr;
        if (fmt.syncEndpointAddress.has_value()) {
            syncEpObj = env->CallStaticObjectMethod(integerClass, integerValueOf, static_cast<jint>(*fmt.syncEndpointAddress));
        }

        jobject dacFormatObj = env->NewObject(dacFormatClass, dacFormatInit,
            static_cast<jint>(fmt.interfaceNumber),
            static_cast<jint>(fmt.altSetting),
            static_cast<jint>(fmt.endpointAddress),
            syncEpObj,
            static_cast<jint>(fmt.bitDepth),
            static_cast<jint>(fmt.subslotBytes),
            static_cast<jint>(fmt.channels),
            ratesList
        );

        env->CallBooleanMethod(formatsList, listAdd, dacFormatObj);
        env->DeleteLocalRef(dacFormatObj);
        env->DeleteLocalRef(ratesList);
        if (syncEpObj) env->DeleteLocalRef(syncEpObj);
    }

    // Build supportedSampleRates list
    jobject sampleRatesList = env->NewObject(listClass, listInit);
    for (uint32_t r : caps.supportedSampleRates) {
        jobject rateObj = env->CallStaticObjectMethod(integerClass, integerValueOf, static_cast<jint>(r));
        env->CallBooleanMethod(sampleRatesList, listAdd, rateObj);
        env->DeleteLocalRef(rateObj);
    }

    // Optional Integers
    jobject clockSourceIdObj = nullptr;
    if (caps.clockSourceId.has_value()) {
        clockSourceIdObj = env->CallStaticObjectMethod(integerClass, integerValueOf, static_cast<jint>(*caps.clockSourceId));
    }

    jobject volumeFuIdObj = nullptr;
    if (caps.volumeFeatureUnitId.has_value()) {
        volumeFuIdObj = env->CallStaticObjectMethod(integerClass, integerValueOf, static_cast<jint>(*caps.volumeFeatureUnitId));
    }

    // Construct DacCapabilities
    jclass capsClass = env->FindClass("echo/music/usbaudio/model/DacCapabilities");
    jmethodID capsInit = env->GetMethodID(capsClass, "<init>",
        "(ILjava/util/List;Ljava/util/List;Ljava/lang/Integer;ZLjava/lang/Integer;FFF)V");

    jobject capsObj = env->NewObject(capsClass, capsInit,
        static_cast<jint>(caps.uacVersion),
        formatsList,
        sampleRatesList,
        clockSourceIdObj,
        static_cast<jboolean>(caps.hasHardwareVolume),
        volumeFuIdObj,
        static_cast<jfloat>(caps.minVolumeDb),
        static_cast<jfloat>(caps.maxVolumeDb),
        static_cast<jfloat>(caps.volumeResDb)
    );

    return capsObj;
}

JNIEXPORT jint JNICALL
Java_echo_music_usbaudio_UsbAudioDriver_nativeCalculateNominalPacketSamples(JNIEnv* /* env */, jobject /* thiz */, jint sampleRate) {
    return static_cast<jint>(echo::music::usbaudio::UsbStreamEngine::calculateNominalPacketSamples(static_cast<uint32_t>(sampleRate)));
}

JNIEXPORT jint JNICALL
Java_echo_music_usbaudio_UsbAudioDriver_nativeStartStream(JNIEnv* /* env */, jobject /* thiz */,
                                                          jint fd, jint interfaceNumber, jint altSetting,
                                                          jint dataEp, jint syncEp,
                                                          jint sampleRate, jint bitDepth, jint channels) {
    return g_stream_engine.startStream(fd, interfaceNumber, altSetting, dataEp, syncEp,
                                       static_cast<uint32_t>(sampleRate),
                                       static_cast<uint32_t>(bitDepth),
                                       static_cast<uint32_t>(channels));
}

JNIEXPORT jint JNICALL
Java_echo_music_usbaudio_UsbAudioDriver_nativeGetAvailableWrite(JNIEnv* /* env */, jobject /* thiz */) {
    return static_cast<jint>(g_stream_engine.availableWrite());
}

JNIEXPORT jint JNICALL
Java_echo_music_usbaudio_UsbAudioDriver_nativeStopStream(JNIEnv* /* env */, jobject /* thiz */) {
    return g_stream_engine.stopStream();
}

JNIEXPORT jint JNICALL
Java_echo_music_usbaudio_UsbAudioDriver_nativeWriteAudio(JNIEnv* env, jobject /* thiz */, jbyteArray buffer, jint size) {
    if (!buffer || size <= 0) return 0;
    jsize len = env->GetArrayLength(buffer);
    int to_write = std::min(static_cast<int>(len), size);
    if (to_write <= 0) return 0;

    jbyte* bytes = env->GetByteArrayElements(buffer, nullptr);
    size_t written = g_stream_engine.writeAudio(reinterpret_cast<const uint8_t*>(bytes), static_cast<size_t>(to_write));
    env->ReleaseByteArrayElements(buffer, bytes, JNI_ABORT);

    return static_cast<jint>(written);
}

JNIEXPORT void JNICALL
Java_echo_music_usbaudio_UsbAudioDriver_nativeSetVolumeMultiplier(JNIEnv* /* env */, jobject /* thiz */, jdouble multiplier) {
    g_stream_engine.setVolumeMultiplier(static_cast<double>(multiplier));
}

JNIEXPORT void JNICALL
Java_echo_music_usbaudio_UsbAudioDriver_nativeFlushStream(JNIEnv* /* env */, jobject /* thiz */) {
    g_stream_engine.flush();
}

JNIEXPORT jlong JNICALL
Java_echo_music_usbaudio_UsbAudioDriver_nativeGetFramesPlayed(JNIEnv* /* env */, jobject /* thiz */) {
    return static_cast<jlong>(g_stream_engine.getFramesPlayed());
}

JNIEXPORT jboolean JNICALL
Java_echo_music_usbaudio_UsbAudioDriver_nativeHasPendingData(JNIEnv* /* env */, jobject /* thiz */) {
    return g_stream_engine.ringBuffer().availableRead() > 0 ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
