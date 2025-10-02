package com.maxrave.echo.service

import android.util.Log
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import iad1tya.echo.music.data.model.metadata.Line
import iad1tya.echo.music.data.model.metadata.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class DetectedLanguage(
    val code: String,
    val name: String,
    val confidence: Float
)

data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val detectedLanguage: DetectedLanguage,
    val targetLanguage: String
)

@Singleton
class TranslationService @Inject constructor(
    private val languageDownloadManager: LanguageDownloadManager
) {
    
    companion object {
        private const val TAG = "TranslationService"
        private const val MIN_CONFIDENCE_THRESHOLD = 0.5f
        
        // Extended language support including regional languages
        private val LANGUAGE_NAMES = mapOf(
            "en" to "English",
            "hi" to "Hindi",
            "ur" to "Urdu",
            "bn" to "Bengali",
            "ta" to "Tamil",
            "te" to "Telugu",
            "kn" to "Kannada",
            "gu" to "Gujarati",
            "mr" to "Marathi",
            "es" to "Spanish",
            "fr" to "French",
            "de" to "German",
            "it" to "Italian",
            "pt" to "Portuguese",
            "ru" to "Russian",
            "ja" to "Japanese",
            "ko" to "Korean",
            "zh" to "Chinese",
            "ar" to "Arabic",
            "th" to "Thai",
            "vi" to "Vietnamese",
            "tr" to "Turkish",
            "pl" to "Polish",
            "nl" to "Dutch",
            "sv" to "Swedish",
            "da" to "Danish",
            "no" to "Norwegian",
            "fi" to "Finnish",
            "he" to "Hebrew",
            "id" to "Indonesian",
            "ms" to "Malay",
            "tl" to "Filipino",
            "sw" to "Swahili",
            "yo" to "Yoruba",
            "zu" to "Zulu",
            "af" to "Afrikaans"
        )
        
        // Special handling for mixed languages like Hinglish
        private val MIXED_LANGUAGE_PATTERNS = mapOf(
            "hinglish" to listOf("hi", "en"), // Hindi + English
            "spanglish" to listOf("es", "en"), // Spanish + English
            "franglais" to listOf("fr", "en")  // French + English
        )
    }

    /**
     * Detects the language of given text
     */
    suspend fun detectLanguage(text: String): DetectedLanguage? = withContext(Dispatchers.IO) {
        try {
            if (text.isBlank()) return@withContext null
            
            val languageIdentifier = LanguageIdentification.getClient()
            val identifiedLanguage = languageIdentifier.identifyLanguage(text).await()
            
            if (identifiedLanguage == "und") {
                // Language could not be identified
                Log.w(TAG, "Could not identify language for text: ${text.take(50)}...")
                return@withContext null
            }
            
            // Get confidence score
            val possibleLanguages = languageIdentifier.identifyPossibleLanguages(text).await()
            val confidence = possibleLanguages.find { it.languageTag == identifiedLanguage }?.confidence ?: 0f
            
            if (confidence < MIN_CONFIDENCE_THRESHOLD) {
                Log.w(TAG, "Low confidence ($confidence) for detected language: $identifiedLanguage")
            }
            
            val languageName = LANGUAGE_NAMES[identifiedLanguage] ?: identifiedLanguage.uppercase()
            
            DetectedLanguage(
                code = identifiedLanguage,
                name = languageName,
                confidence = confidence
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting language", e)
            null
        }
    }

    /**
     * Detects language from lyrics by analyzing multiple lines
     */
    suspend fun detectLyricsLanguage(lyrics: Lyrics): DetectedLanguage? = withContext(Dispatchers.IO) {
        try {
            val lines = lyrics.lines ?: return@withContext null
            if (lines.isEmpty()) return@withContext null
            
            // Combine first few lines for better detection
            val sampleText = lines.take(5)
                .mapNotNull { it.words }
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .take(500) // Limit to 500 characters for efficiency
            
            if (sampleText.isBlank()) return@withContext null
            
            Log.d(TAG, "Detecting language for sample: ${sampleText.take(100)}...")
            
            // Try to detect mixed languages (like Hinglish)
            val detectedMixed = detectMixedLanguage(sampleText)
            if (detectedMixed != null) {
                return@withContext detectedMixed
            }
            
            // Regular language detection
            detectLanguage(sampleText)
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting lyrics language", e)
            null
        }
    }

    /**
     * Attempts to detect mixed languages like Hinglish
     */
    private suspend fun detectMixedLanguage(text: String): DetectedLanguage? {
        try {
            // Simple heuristic for Hinglish detection
            val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
            val hasLatin = text.any { it in 'A'..'Z' || it in 'a'..'z' }
            
            if (hasDevanagari && hasLatin) {
                Log.d(TAG, "Detected mixed script (likely Hinglish)")
                return DetectedLanguage(
                    code = "hi", // Use Hindi as base for translation
                    name = "Hinglish (Hindi + English)",
                    confidence = 0.8f
                )
            }
            
            // Could add more mixed language detection here
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting mixed language", e)
            return null
        }
    }

    /**
     * Enhanced lyrics translation with automatic language detection
     */
    suspend fun translateLyricsWithDetection(
        lyrics: Lyrics,
        targetLanguage: String,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onLanguageDetected: (DetectedLanguage) -> Unit = { }
    ): Lyrics? = withContext(Dispatchers.IO) {
        try {
            // First detect the source language
            val detectedLanguage = detectLyricsLanguage(lyrics)
            if (detectedLanguage != null) {
                Log.d(TAG, "Detected language: ${detectedLanguage.name} (${detectedLanguage.code}) with confidence ${detectedLanguage.confidence}")
                onLanguageDetected(detectedLanguage)
                
                // If detected language is same as target, no translation needed
                if (detectedLanguage.code == targetLanguage) {
                    Log.d(TAG, "Source and target languages are the same, skipping translation")
                    return@withContext lyrics
                }
            }
            
            // Proceed with translation
            translateLyrics(lyrics, targetLanguage, detectedLanguage?.code, onProgress)
        } catch (e: Exception) {
            Log.e(TAG, "Error in enhanced translation", e)
            null
        }
    }

    /**
     * Translates lyrics using ML Kit offline translation with source language detection
     */
    suspend fun translateLyrics(
        lyrics: Lyrics,
        targetLanguage: String,
        sourceLanguage: String? = null,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Lyrics? = withContext(Dispatchers.IO) {
        try {
            // Check if target language is downloaded
            if (!languageDownloadManager.isLanguageDownloaded(targetLanguage)) {
                Log.w(TAG, "Target language $targetLanguage is not downloaded")
                return@withContext null
            }
            
            val lines = lyrics.lines ?: return@withContext null
            val translatedLines = mutableListOf<Line>()
            
            // Get ML Kit language codes
            val targetMlKitCode = languageDownloadManager.getMlKitLanguageCode(targetLanguage)
                ?: return@withContext null
            
            // Use detected source language or default to auto-detect
            val sourceMlKitCode = sourceLanguage?.let { 
                languageDownloadManager.getMlKitLanguageCode(it) 
            } ?: TranslateLanguage.ENGLISH // Default fallback
                
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceMlKitCode)
                .setTargetLanguage(targetMlKitCode)
                .build()
                
            val translator = Translation.getClient(options)
            
            lines.forEachIndexed { index, line ->
                onProgress(index + 1, lines.size)
                
                val translatedText = translateText(translator, line.words)
                if (translatedText != null) {
                    translatedLines.add(
                        Line(
                            startTimeMs = line.startTimeMs,
                            words = translatedText,
                            syllables = line.syllables,
                            endTimeMs = line.endTimeMs
                        )
                    )
                } else {
                    // If translation fails, keep original text
                    translatedLines.add(line)
                }
            }
            
            // Close the translator to free resources
            translator.close()
            
            Lyrics(
                error = false,
                lines = translatedLines,
                syncType = lyrics.syncType
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error translating lyrics", e)
            null
        }
    }

    /**
     * Translates a single text using ML Kit offline translation
     */
    private suspend fun translateText(translator: Translator, text: String): String? = withContext(Dispatchers.IO) {
        try {
            if (text.isBlank()) return@withContext text
            
            translator.translate(text).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error translating text: $text", e)
            null
        }
    }

    /**
     * Checks if a language is available for translation
     */
    suspend fun isLanguageAvailable(languageCode: String): Boolean {
        return languageDownloadManager.isLanguageDownloaded(languageCode)
    }

    /**
     * Gets the display name for a language code
     */
    fun getLanguageName(languageCode: String): String {
        return LANGUAGE_NAMES[languageCode] ?: languageCode.uppercase()
    }

    /**
     * Gets all supported language codes
     */
    fun getSupportedLanguages(): Map<String, String> {
        return LANGUAGE_NAMES
    }

    /**
     * Checks if a language code is supported
     */
    fun isLanguageSupported(languageCode: String): Boolean {
        return LANGUAGE_NAMES.containsKey(languageCode)
    }
}