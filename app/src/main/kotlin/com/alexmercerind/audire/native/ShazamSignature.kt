package com.alexmercerind.audire.native

import android.util.Log

// This class provides JNI binding to Shazam's signature algorithm.
//
// ShazamSignature.create takes audio samples as ShortArray.
// Format: PCM 16 Bit LE
// Sample Rate: 16000 Hz
//
// References:
// https://github.com/marin-m/SongRec
// https://github.com/alexmercerind/shazam-signature-jni
class ShazamSignature {
    private var isLoaded = false

    init {
        try {
            System.loadLibrary("shazam_signature_jni")
            isLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            Log.e("ShazamSignature", "Failed to load native library", e)
        } catch (e: SecurityException) {
            Log.e("ShazamSignature", "Security exception loading native library", e)
        }
    }

    external fun create(input: ShortArray): String

    fun safeCreate(input: ShortArray): String {
        if (!isLoaded) {
            throw UnsatisfiedLinkError("Native library shazam_signature_jni not loaded")
        }
        return create(input)
    }
}
