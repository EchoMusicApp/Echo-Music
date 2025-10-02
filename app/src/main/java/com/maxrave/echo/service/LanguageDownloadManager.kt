package com.maxrave.echo.service

import android.content.Context
import android.util.Log
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageDownloadManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "LanguageDownloadManager"
        
        // Language code mapping for ML Kit
        private val LANGUAGE_CODE_MAP = mapOf(
            "en" to TranslateLanguage.ENGLISH,
            "es" to TranslateLanguage.SPANISH,
            "fr" to TranslateLanguage.FRENCH,
            "de" to TranslateLanguage.GERMAN,
            "it" to TranslateLanguage.ITALIAN,
            "pt" to TranslateLanguage.PORTUGUESE,
            "ru" to TranslateLanguage.RUSSIAN,
            "ja" to TranslateLanguage.JAPANESE,
            "ko" to TranslateLanguage.KOREAN,
            "zh" to TranslateLanguage.CHINESE,
            "ar" to TranslateLanguage.ARABIC,
            "hi" to TranslateLanguage.HINDI,
            "th" to TranslateLanguage.THAI,
            "vi" to TranslateLanguage.VIETNAMESE,
            "tr" to TranslateLanguage.TURKISH,
            "pl" to TranslateLanguage.POLISH,
            "nl" to TranslateLanguage.DUTCH,
            "sv" to TranslateLanguage.SWEDISH,
            "da" to TranslateLanguage.DANISH,
            "no" to TranslateLanguage.NORWEGIAN,
            "fi" to TranslateLanguage.FINNISH,
            "he" to TranslateLanguage.HEBREW,
            "id" to TranslateLanguage.INDONESIAN,
            "ms" to TranslateLanguage.MALAY,
            "tl" to TranslateLanguage.TAGALOG,
            "sw" to TranslateLanguage.SWAHILI,
            "af" to TranslateLanguage.AFRIKAANS,
            "bn" to TranslateLanguage.BENGALI,
            "gu" to TranslateLanguage.GUJARATI,
            "kn" to TranslateLanguage.KANNADA,
            "mr" to TranslateLanguage.MARATHI,
            "ta" to TranslateLanguage.TAMIL,
            "te" to TranslateLanguage.TELUGU,
            "ur" to TranslateLanguage.URDU
        )
    }

    data class LanguageInfo(
        val code: String,
        val name: String,
        val isDownloaded: Boolean,
        val isDownloading: Boolean = false,
        val downloadProgress: Float = 0f // 0.0 to 1.0
    )

    private val downloadingLanguages = mutableSetOf<String>()
    private val downloadProgress = mutableMapOf<String, Float>()

    /**
     * Gets the display name for a language code
     */
    fun getLanguageDisplayName(languageCode: String): String {
        return when (languageCode) {
            "en" -> "English"
            "es" -> "Spanish"
            "fr" -> "French"
            "de" -> "German"
            "it" -> "Italian"
            "pt" -> "Portuguese"
            "ru" -> "Russian"
            "ja" -> "Japanese"
            "ko" -> "Korean"
            "zh" -> "Chinese"
            "ar" -> "Arabic"
            "hi" -> "Hindi"
            "th" -> "Thai"
            "vi" -> "Vietnamese"
            "tr" -> "Turkish"
            "pl" -> "Polish"
            "nl" -> "Dutch"
            "sv" -> "Swedish"
            "da" -> "Danish"
            "no" -> "Norwegian"
            "fi" -> "Finnish"
            "he" -> "Hebrew"
            "id" -> "Indonesian"
            "ms" -> "Malay"
            "tl" -> "Filipino"
            "sw" -> "Swahili"
            "af" -> "Afrikaans"
            "bn" -> "Bengali"
            "gu" -> "Gujarati"
            "kn" -> "Kannada"
            "mr" -> "Marathi"
            "ta" -> "Tamil"
            "te" -> "Telugu"
            "ur" -> "Urdu"
            else -> languageCode.uppercase()
        }
    }

    /**
     * Gets all available languages with their download status
     */
    suspend fun getAvailableLanguages(): List<LanguageInfo> {
        return LANGUAGE_CODE_MAP.keys.map { code ->
            LanguageInfo(
                code = code,
                name = getLanguageDisplayName(code),
                isDownloaded = isLanguageDownloaded(code),
                isDownloading = downloadingLanguages.contains(code),
                downloadProgress = downloadProgress[code] ?: 0f
            )
        }.sortedBy { it.name }
    }

    /**
     * Checks if a language model is downloaded by attempting a quick translation
     */
    suspend fun isLanguageDownloaded(languageCode: String): Boolean {
        val mlKitLanguage = LANGUAGE_CODE_MAP[languageCode] ?: return false
        
        return try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(mlKitLanguage)
                .build()
            
            val translator = Translation.getClient(options)
            
            // Try a quick translation to check if model is available
            // This will fail if the model is not downloaded
            translator.translate("test").await()
            translator.close()
            true
        } catch (e: Exception) {
            Log.d(TAG, "Language model not downloaded: $languageCode")
            false
        }
    }

    /**
     * Downloads a language model with progress tracking
     */
    suspend fun downloadLanguage(
        languageCode: String,
        onProgress: (progress: Float) -> Unit = {}
    ): Result<Unit> {
        val mlKitLanguage = LANGUAGE_CODE_MAP[languageCode]
            ?: return Result.failure(IllegalArgumentException("Unsupported language: $languageCode"))

        if (downloadingLanguages.contains(languageCode)) {
            Log.w(TAG, "Language $languageCode is already downloading, skipping")
            return Result.success(Unit)
        }

        return try {
            downloadingLanguages.add(languageCode)
            downloadProgress[languageCode] = 0f
            
            Log.d(TAG, "Starting download for language: $languageCode")
            
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(mlKitLanguage)
                .build()
            
            val translator = Translation.getClient(options)
            
            // Start with initial progress
            downloadProgress[languageCode] = 0.1f
            onProgress(0.1f)
            kotlinx.coroutines.delay(100)
            
            // Simulate progress updates since ML Kit doesn't provide real progress
            for (progress in 20..80 step 10) {
                if (!downloadingLanguages.contains(languageCode)) {
                    // Download was cancelled
                    Log.d(TAG, "Download cancelled for language: $languageCode")
                    return Result.failure(Exception("Download cancelled"))
                }
                downloadProgress[languageCode] = progress / 100f
                onProgress(progress / 100f)
                kotlinx.coroutines.delay(200) // Simulate download time
            }
            
            // Set to 90% before actual download
            downloadProgress[languageCode] = 0.9f
            onProgress(0.9f)
            
            // Download the model
            Log.d(TAG, "Downloading ML Kit model for language: $languageCode")
            translator.downloadModelIfNeeded().await()
            
            // Complete the download
            downloadProgress[languageCode] = 1f
            onProgress(1f)
            
            // Wait a moment to show 100% completion
            kotlinx.coroutines.delay(300)
            
            Log.d(TAG, "Successfully downloaded language model: $languageCode")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download language model: $languageCode", e)
            Result.failure(e)
        } finally {
            downloadingLanguages.remove(languageCode)
            downloadProgress.remove(languageCode)
        }
    }

    /**
     * Cancels a download in progress
     */
    fun cancelDownload(languageCode: String) {
        Log.d(TAG, "Cancelling download for language: $languageCode")
        downloadingLanguages.remove(languageCode)
        downloadProgress.remove(languageCode)
    }

    /**
     * Deletes a downloaded language model
     * Note: ML Kit doesn't provide a direct way to delete models, 
     * so this is a placeholder for future implementation
     */
    suspend fun deleteLanguage(languageCode: String): Result<Unit> {
        return try {
            Log.d(TAG, "Delete language model not supported by ML Kit: $languageCode")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete language model: $languageCode", e)
            Result.failure(e)
        }
    }

    /**
     * Gets the ML Kit language code for a given language code
     */
    fun getMlKitLanguageCode(languageCode: String): String? {
        return LANGUAGE_CODE_MAP[languageCode]
    }

    /**
     * Gets the size estimate for a language model (approximate values in MB)
     */
    fun getLanguageModelSize(languageCode: String): Int {
        return when (languageCode) {
            "en" -> 0 // English is usually pre-installed
            "zh", "ja", "ko", "ar", "hi", "th" -> 35 // Languages with complex scripts
            else -> 25 // Most European languages
        }
    }
}
