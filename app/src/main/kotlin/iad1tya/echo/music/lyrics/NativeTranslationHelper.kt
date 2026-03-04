package iad1tya.echo.music.lyrics

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device translation using ML Kit Translate.
 * No API key needed — models (~30 MB per language pair) are downloaded on first use.
 * Supports ~59 languages. Source language is auto-detected via ML Kit Language ID.
 */
object NativeTranslationHelper {

    /**
     * Translates [lines] to [targetLanguageBcp47] (e.g. "en", "hi", "ja").
     *
     * @param onStatus  Progress string callback for UI display.
     * @return          Result containing translated lines (same count, blank lines preserved).
     */
    suspend fun translateLines(
        lines: List<String>,
        targetLanguageBcp47: String,
        onStatus: (String) -> Unit = {},
    ): Result<List<String>> {
        return try {
            // 1 — Detect source language from first few non-blank lines
            onStatus("Detecting language…")
            val sampleText = lines.filter { it.isNotBlank() }.take(6).joinToString(" ")
            val detected = detectLanguage(sampleText)
            val sourceLang: String = detected
                ?.let { tag ->
                    try { TranslateLanguage.fromLanguageTag(tag) ?: TranslateLanguage.ENGLISH }
                    catch (_: Exception) { TranslateLanguage.ENGLISH }
                }
                ?: TranslateLanguage.ENGLISH

            val targetLang: String = try {
                TranslateLanguage.fromLanguageTag(targetLanguageBcp47) ?: TranslateLanguage.ENGLISH
            } catch (_: Exception) {
                TranslateLanguage.ENGLISH
            }

            // If source == target, nothing to translate
            if (sourceLang == targetLang) {
                return Result.success(lines)
            }

            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build()
            val translator = Translation.getClient(options)

            // 2 — Download the model pair if not already cached
            onStatus("Downloading translation model…")
            try {
                downloadModel(translator)
            } catch (e: Exception) {
                translator.close()
                return Result.failure(Exception("Model download failed: ${e.message}"))
            }

            // 3 — Translate line-by-line (ML Kit has no batch API)
            val results = ArrayList<String>(lines.size)
            val totalNonBlank = lines.count { it.isNotBlank() }
            var done = 0
            for (line in lines) {
                if (line.isBlank()) {
                    results.add(line)
                } else {
                    val out = runCatching { translateSingle(translator, line) }
                        .getOrDefault(line)   // keep original on per-line failure
                    results.add(out)
                    done++
                    onStatus("Translating $done / $totalNonBlank…")
                }
            }

            translator.close()
            Result.success(results)

        } catch (e: Exception) {
            Timber.e(e, "NativeTranslationHelper error")
            Result.failure(e)
        }
    }

    // ── ML Kit coroutine wrappers ─────────────────────────────────────────────

    private val languageIdentifier by lazy { LanguageIdentification.getClient() }

    private suspend fun detectLanguage(text: String): String? =
        suspendCancellableCoroutine { cont ->
            languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener { code ->
                    cont.resume(if (code == "und") null else code)
                }
                .addOnFailureListener { cont.resume(null) }
        }

    private suspend fun downloadModel(
        translator: com.google.mlkit.nl.translate.Translator,
    ) = suspendCancellableCoroutine<Unit> { cont ->
        val conditions = DownloadConditions.Builder().build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    private suspend fun translateSingle(
        translator: com.google.mlkit.nl.translate.Translator,
        text: String,
    ): String = suspendCancellableCoroutine { cont ->
        translator.translate(text)
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resume(text) }  // return original on error
    }
}
