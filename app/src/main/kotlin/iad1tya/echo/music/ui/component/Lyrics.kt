package iad1tya.echo.music.ui.component

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.CircularProgressIndicator
import iad1tya.echo.music.lyrics.LyricsTranslationHelper
import iad1tya.echo.music.ui.component.LyricsShareDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import coil3.toBitmap
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.DarkModeKey
import iad1tya.echo.music.constants.LyricsAnimationStyle
import iad1tya.echo.music.constants.LyricsAnimationStyleKey
import iad1tya.echo.music.constants.LyricsGlowEffectKey
import iad1tya.echo.music.constants.AppleMusicLyricsBlurKey
import iad1tya.echo.music.constants.LyricsClickKey
import iad1tya.echo.music.constants.LyricsRomanizeBelarusianKey
import iad1tya.echo.music.constants.LyricsRomanizeBulgarianKey
import iad1tya.echo.music.constants.LyricsRomanizeCyrillicByLineKey
import iad1tya.echo.music.constants.LyricsRomanizeJapaneseKey
import iad1tya.echo.music.constants.LyricsRomanizeKoreanKey
import iad1tya.echo.music.constants.LyricsRomanizeKyrgyzKey
import iad1tya.echo.music.constants.LyricsRomanizeRussianKey
import iad1tya.echo.music.constants.LyricsRomanizeSerbianKey
import iad1tya.echo.music.constants.LyricsRomanizeUkrainianKey
import iad1tya.echo.music.constants.LyricsRomanizeMacedonianKey
import iad1tya.echo.music.constants.LyricsScrollKey
import iad1tya.echo.music.constants.LyricsTextPositionKey
import iad1tya.echo.music.constants.PlayerBackgroundStyle
import iad1tya.echo.music.constants.PlayerBackgroundStyleKey
import iad1tya.echo.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import iad1tya.echo.music.lyrics.LyricsEntry
import iad1tya.echo.music.lyrics.LyricsUtils.findCurrentLineIndex
import iad1tya.echo.music.lyrics.LyricsUtils.isBelarusian
import iad1tya.echo.music.lyrics.LyricsUtils.isChinese
import iad1tya.echo.music.lyrics.LyricsUtils.isJapanese
import iad1tya.echo.music.lyrics.LyricsUtils.isKorean
import iad1tya.echo.music.lyrics.LyricsUtils.isKyrgyz
import iad1tya.echo.music.lyrics.LyricsUtils.isRussian
import iad1tya.echo.music.lyrics.LyricsUtils.isSerbian
import iad1tya.echo.music.lyrics.LyricsUtils.isBulgarian
import iad1tya.echo.music.lyrics.LyricsUtils.isUkrainian
import iad1tya.echo.music.lyrics.LyricsUtils.isMacedonian
import iad1tya.echo.music.lyrics.LyricsUtils.parseLyrics
import iad1tya.echo.music.lyrics.LyricsUtils.romanizeCyrillic
import iad1tya.echo.music.lyrics.LyricsUtils.romanizeJapanese
import iad1tya.echo.music.lyrics.LyricsUtils.romanizeKorean
import iad1tya.echo.music.ui.component.shimmer.ShimmerHost
import iad1tya.echo.music.ui.component.shimmer.TextPlaceholder
import iad1tya.echo.music.ui.screens.settings.DarkMode
import iad1tya.echo.music.ui.screens.settings.LyricsPosition
import iad1tya.echo.music.ui.utils.fadingEdge
import iad1tya.echo.music.utils.ComposeToImage
import iad1tya.echo.music.utils.rememberEnumPreference
import iad1tya.echo.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds
import iad1tya.echo.music.constants.OpenRouterApiKey
import iad1tya.echo.music.constants.OpenRouterBaseUrlKey
import iad1tya.echo.music.constants.OpenRouterModelKey
import iad1tya.echo.music.constants.AutoTranslateLyricsKey
import iad1tya.echo.music.constants.AutoTranslateLyricsMismatchKey
import iad1tya.echo.music.constants.TranslateLanguageKey
import iad1tya.echo.music.constants.AiProviderKey
import iad1tya.echo.music.lyrics.LanguageDetectionHelper
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("UnusedBoxWithConstraintsScope", "StringFormatInvalid")
@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    palette: List<Color> = emptyList(),
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current // Get configuration

    val landscapeOffset =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val lyricsTextPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.CENTER)
    val changeLyrics by rememberPreference(LyricsClickKey, true)
    val scrollLyrics by rememberPreference(LyricsScrollKey, true)
    val romanizeJapaneseLyrics by rememberPreference(LyricsRomanizeJapaneseKey, true)
    val romanizeKoreanLyrics by rememberPreference(LyricsRomanizeKoreanKey, true)
    val romanizeRussianLyrics by rememberPreference(LyricsRomanizeRussianKey, true)
    val romanizeUkrainianLyrics by rememberPreference(LyricsRomanizeUkrainianKey, true)
    val romanizeSerbianLyrics by rememberPreference(LyricsRomanizeSerbianKey, true)
    val romanizeBulgarianLyrics by rememberPreference(LyricsRomanizeBulgarianKey, true)
    val romanizeBelarusianLyrics by rememberPreference(LyricsRomanizeBelarusianKey, true)
    val romanizeKyrgyzLyrics by rememberPreference(LyricsRomanizeKyrgyzKey, true)
    val romanizeMacedonianLyrics by rememberPreference(LyricsRomanizeMacedonianKey, true)
    val romanizeCyrillicByLine by rememberPreference(LyricsRomanizeCyrillicByLineKey, false)
    
    val openRouterApiKey by rememberPreference(OpenRouterApiKey, "")
    val openRouterBaseUrl by rememberPreference(OpenRouterBaseUrlKey, "https://openrouter.ai/api/v1/chat/completions")
    val openRouterModel by rememberPreference(OpenRouterModelKey, "mistralai/mistral-small-3.1-24b-instruct:free")
    val autoTranslateLyrics by rememberPreference(AutoTranslateLyricsKey, false)
    val autoTranslateLyricsMismatch by rememberPreference(AutoTranslateLyricsMismatchKey, false)
    val translateLanguage by rememberPreference(TranslateLanguageKey, "en")
    val translateMode by rememberPreference(iad1tya.echo.music.constants.TranslateModeKey, "Literal")
    val aiProvider by rememberPreference(AiProviderKey, "OpenRouter")
    
    val lyricsTextSize by rememberPreference(iad1tya.echo.music.constants.LyricsTextSizeKey, 20f)
    val lyricsLineSpacing by rememberPreference(iad1tya.echo.music.constants.LyricsLineSpacingKey, 2f)
    val lyricsAnimationStyle by rememberEnumPreference(LyricsAnimationStyleKey, LyricsAnimationStyle.VIVIMUSIC_1)
    val lyricsGlowEffect by rememberPreference(LyricsGlowEffectKey, false)
    val appleMusicLyricsBlur by rememberPreference(AppleMusicLyricsBlurKey, true)
    
    val scope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val lyrics = remember(lyricsEntity) { lyricsEntity?.lyrics?.trim() }

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.BLUR
    )

    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val lines = remember(lyrics, scope) {
        if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
            emptyList()
        } else if (lyrics.startsWith("[")) {
            val parsedLines = parseLyrics(lyrics)

            val isRussianLyrics = romanizeRussianLyrics && !romanizeCyrillicByLine && isRussian(lyrics)
            val isUkrainianLyrics = romanizeUkrainianLyrics && !romanizeCyrillicByLine && isUkrainian(lyrics)
            val isSerbianLyrics = romanizeSerbianLyrics && !romanizeCyrillicByLine && isSerbian(lyrics)
            val isBulgarianLyrics = romanizeBulgarianLyrics && !romanizeCyrillicByLine && isBulgarian(lyrics)
            val isBelarusianLyrics = romanizeBelarusianLyrics && !romanizeCyrillicByLine && isBelarusian(lyrics)
            val isKyrgyzLyrics = romanizeKyrgyzLyrics && !romanizeCyrillicByLine && isKyrgyz(lyrics)
            val isMacedonianLyrics = romanizeMacedonianLyrics && !romanizeCyrillicByLine && isMacedonian(lyrics)

            parsedLines.map { entry ->
                val newEntry = LyricsEntry(entry.time, entry.text)
                
                if (romanizeJapaneseLyrics && isJapanese(entry.text) && !isChinese(entry.text)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeJapanese(entry.text)
                    }
                }

                if (romanizeKoreanLyrics && isKorean(entry.text)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeKorean(entry.text)
                    }
                }

                if (romanizeRussianLyrics && (if (romanizeCyrillicByLine) isRussian(entry.text) else isRussianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                else if (romanizeUkrainianLyrics && (if (romanizeCyrillicByLine) isUkrainian(entry.text) else isUkrainianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                else if (romanizeSerbianLyrics && (if (romanizeCyrillicByLine) isSerbian(entry.text) else isSerbianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                else if (romanizeBulgarianLyrics && (if (romanizeCyrillicByLine) isBulgarian(entry.text) else isBulgarianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                else if (romanizeBelarusianLyrics && (if (romanizeCyrillicByLine) isBelarusian(entry.text) else isBelarusianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                else if (romanizeKyrgyzLyrics && (if (romanizeCyrillicByLine) isKyrgyz(entry.text) else isKyrgyzLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                else if (romanizeMacedonianLyrics && (if (romanizeCyrillicByLine) isMacedonian(entry.text) else isMacedonianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                newEntry
            }.let {
                listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + it
            }
        } else {
            val isRussianLyrics = romanizeRussianLyrics && !romanizeCyrillicByLine && isRussian(lyrics)
            val isUkrainianLyrics = romanizeUkrainianLyrics && !romanizeCyrillicByLine && isUkrainian(lyrics)
            val isSerbianLyrics = romanizeSerbianLyrics && !romanizeCyrillicByLine && isSerbian(lyrics)
            val isBulgarianLyrics = romanizeBulgarianLyrics && !romanizeCyrillicByLine && isBulgarian(lyrics)
            val isBelarusianLyrics = romanizeBelarusianLyrics && !romanizeCyrillicByLine && isBelarusian(lyrics)
            val isKyrgyzLyrics = romanizeKyrgyzLyrics && !romanizeCyrillicByLine && isKyrgyz(lyrics)
            val isMacedonianLyrics = romanizeMacedonianLyrics && !romanizeCyrillicByLine && isMacedonian(lyrics)

            lyrics.lines().mapIndexed { index, line ->
                val newEntry = LyricsEntry(index * 100L, line)

                if (romanizeJapaneseLyrics && isJapanese(line) && !isChinese(line)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeJapanese(line)
                    }
                }

                if (romanizeKoreanLyrics && isKorean(line)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeKorean(line)
                    }
                }

                if (romanizeRussianLyrics && (if (romanizeCyrillicByLine) isRussian(line) else isRussianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                else if (romanizeUkrainianLyrics && (if (romanizeCyrillicByLine) isUkrainian(line) else isUkrainianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                else if (romanizeSerbianLyrics && (if (romanizeCyrillicByLine) isSerbian(line) else isSerbianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                else if (romanizeBulgarianLyrics && (if (romanizeCyrillicByLine) isBulgarian(line) else isBulgarianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                else if (romanizeBelarusianLyrics && (if (romanizeCyrillicByLine) isBelarusian(line) else isBelarusianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                else if (romanizeKyrgyzLyrics && (if (romanizeCyrillicByLine) isKyrgyz(line) else isKyrgyzLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                else if (romanizeMacedonianLyrics && (if (romanizeCyrillicByLine) isMacedonian(line) else isMacedonianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                newEntry
            }
        }
    }
    
    // State for translation status
    val translationStatus by LyricsTranslationHelper.status.collectAsState()
    
    // Listen for manual trigger
    LaunchedEffect(Unit) {
        LyricsTranslationHelper.manualTrigger.collect {
             if (lines.isNotEmpty()) {
                 if (aiProvider == "Google Translate") {
                     LyricsTranslationHelper.translateLyricsNative(
                         lyrics = lines,
                         targetLanguage = translateLanguage,
                         scope = scope,
                         mode = translateMode,
                     )
                 } else if (openRouterApiKey.isNotBlank()) {
                     LyricsTranslationHelper.translateLyrics(
                         lyrics = lines,
                         targetLanguage = translateLanguage,
                         apiKey = openRouterApiKey,
                         baseUrl = openRouterBaseUrl,
                         model = openRouterModel,
                         mode = translateMode,
                         scope = scope
                     )
                 } else {
                     Toast.makeText(context, "API Key Required", Toast.LENGTH_SHORT).show()
                 }
             }
        }
    }

    LaunchedEffect(lines, autoTranslateLyrics, autoTranslateLyricsMismatch, openRouterApiKey, aiProvider, isVisible, translateMode, translateLanguage) {
        if (isVisible && lines.isNotEmpty()) {
            val isNative = aiProvider == "Google Translate"
            // First, try to apply cached translations
            val targetLang = if (autoTranslateLyricsMismatch) Locale.getDefault().language else translateLanguage
            val hasCached = LyricsTranslationHelper.applyCachedTranslations(lines, translateMode, targetLang)
            
            // If no cache and auto-translate is enabled, translate
            if (!hasCached && autoTranslateLyrics && (isNative || openRouterApiKey.isNotBlank())) {
                val needsTranslation = lines.any { it.translatedTextFlow.value == null && it.text.isNotBlank() }
                if (needsTranslation) {
                    var shouldTranslate = true
                    if (autoTranslateLyricsMismatch) {
                        val combinedText = lines.take(5).joinToString(" ") { it.text }
                        val detectedLang = LanguageDetectionHelper.identifyLanguage(combinedText)
                        val systemLang = Locale.getDefault().language
                        
                        if (detectedLang != null && detectedLang == systemLang) {
                            shouldTranslate = false
                        }
                    }

                    if (shouldTranslate) {
                        if (isNative) {
                            LyricsTranslationHelper.translateLyricsNative(
                                lyrics = lines,
                                targetLanguage = targetLang,
                                scope = scope,
                                mode = translateMode,
                            )
                        } else {
                            LyricsTranslationHelper.translateLyrics(
                                lyrics = lines,
                                targetLanguage = targetLang,
                                apiKey = openRouterApiKey,
                                baseUrl = openRouterBaseUrl,
                                model = openRouterModel,
                                mode = translateMode,
                                scope = scope
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Status UI
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(1f)
            .padding(top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        when (val status = translationStatus) {
            is LyricsTranslationHelper.TranslationStatus.Translating -> {
                Card(
                     colors = CardDefaults.cardColors(
                         containerColor = MaterialTheme.colorScheme.primaryContainer
                     ),
                     shape = RoundedCornerShape(16.dp),
                     elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Translating lyrics...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            is LyricsTranslationHelper.TranslationStatus.Error -> {
                Card(
                     colors = CardDefaults.cardColors(
                         containerColor = MaterialTheme.colorScheme.errorContainer
                     ),
                     shape = RoundedCornerShape(16.dp),
                     elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.error),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = status.message,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            is LyricsTranslationHelper.TranslationStatus.Success -> {
                 Card(
                     colors = CardDefaults.cardColors(
                         containerColor = MaterialTheme.colorScheme.tertiaryContainer
                     ),
                     shape = RoundedCornerShape(16.dp),
                     elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Translated",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            else -> {}
        }
    }

    val isSynced =
        remember(lyrics) {
            !lyrics.isNullOrEmpty() && lyrics.startsWith("[")
        }

    val textColor = if (playerBackground != PlayerBackgroundStyle.DEFAULT) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    var currentLineIndex by remember {
        mutableIntStateOf(-1)
    }

    var currentPlaybackPosition by remember {
        mutableLongStateOf(0L)
    }
    // Because LaunchedEffect has delay, which leads to inconsistent with current line color and scroll animation,
    // we use deferredCurrentLineIndex when user is scrolling
    var deferredCurrentLineIndex by rememberSaveable {
        mutableIntStateOf(0)
    }

    var previousLineIndex by rememberSaveable {
        mutableIntStateOf(0)
    }

    var lastPreviewTime by rememberSaveable {
        mutableLongStateOf(0L)
    }
    var isSeeking by remember {
        mutableStateOf(false)
    }

    var initialScrollDone by rememberSaveable {
        mutableStateOf(false)
    }

    var shouldScrollToFirstLine by rememberSaveable {
        mutableStateOf(true)
    }

    var isAppMinimized by rememberSaveable {
        mutableStateOf(false)
    }

    var showProgressDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showImageCustomizationDialog by remember { mutableStateOf(false) }
    var shareDialogData by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    var showColorPickerDialog by remember { mutableStateOf(false) }
    var previewBackgroundColor by remember { mutableStateOf(Color(0xFF242424)) }
    var previewTextColor by remember { mutableStateOf(Color.White) }
    var previewSecondaryTextColor by remember { mutableStateOf(Color.White.copy(alpha = 0.7f)) }

    // State for multi-selection
    var isSelectionModeActive by rememberSaveable { mutableStateOf(false) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    var showMaxSelectionToast by remember { mutableStateOf(false) } // State for showing max selection toast

    val lazyListState = rememberLazyListState()
    
    var isAnimating by remember { mutableStateOf(false) }

    // Handle back button press - close selection mode instead of exiting screen
    BackHandler(enabled = isSelectionModeActive) {
        isSelectionModeActive = false
        selectedIndices.clear()
    }

    // Define max selection limit
    val maxSelectionLimit = 5

    // Show toast when max selection is reached
    LaunchedEffect(showMaxSelectionToast) {
        if (showMaxSelectionToast) {
            Toast.makeText(
                context,
                context.getString(R.string.max_selection_limit, maxSelectionLimit),
                Toast.LENGTH_SHORT
            ).show()
            showMaxSelectionToast = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val visibleItemsInfo = lazyListState.layoutInfo.visibleItemsInfo
                val isCurrentLineVisible = visibleItemsInfo.any { it.index == currentLineIndex }
                if (isCurrentLineVisible) {
                    initialScrollDone = false
                }
                isAppMinimized = true
            } else if(event == Lifecycle.Event.ON_START) {
                isAppMinimized = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Reset selection mode if lyrics change
    LaunchedEffect(lines) {
        isSelectionModeActive = false
        selectedIndices.clear()
    }

    LaunchedEffect(lyrics) {
        if (lyrics.isNullOrEmpty() || !lyrics.startsWith("[")) {
            currentLineIndex = -1
            return@LaunchedEffect
        }
        while (isActive) {
            delay(50)
            val sliderPosition = sliderPositionProvider()
            isSeeking = sliderPosition != null
            val position = sliderPosition ?: playerConnection.player.currentPosition
            currentPlaybackPosition = position
            currentLineIndex = findCurrentLineIndex(
                lines,
                position
            )
        }
    }

    // When the playback slider is being dragged, always follow the seek position live
    LaunchedEffect(isSeeking) {
        if (isSeeking) {
            lastPreviewTime = 0L
        }
    }

    // Detect real user touch on the lyrics list — set manual mode so auto-scroll stops
    LaunchedEffect(lazyListState.interactionSource) {
        lazyListState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start && !isSelectionModeActive) {
                lastPreviewTime = System.currentTimeMillis()
            }
        }
    }

    LaunchedEffect(currentLineIndex, lastPreviewTime, initialScrollDone) {

        /**
         * Calculate the lyric offset Based on how many lines (\n chars)
         */
        fun calculateOffset() = with(density) {
            if (currentLineIndex < 0 || currentLineIndex >= lines.size) return@with 0
            val currentItem = lines[currentLineIndex]
            val totalNewLines = currentItem.text.count { it == '\n' }

            val dpValue = if (landscapeOffset) 16.dp else 20.dp
            dpValue.toPx().toInt() * totalNewLines
        }

        if (!isSynced) return@LaunchedEffect
        
        // Smooth page animation without sudden jumps - direct animation to center
        suspend fun performSmoothPageScroll(targetIndex: Int, duration: Int = 1500) {
            if (isAnimating) return // Prevent multiple animations
            
            isAnimating = true
            
            try {
                val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
                if (itemInfo != null) {
                    // Item is visible, animate directly to center without sudden jumps
                    val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                    val center = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                    val itemCenter = itemInfo.offset + itemInfo.size / 2
                    val offset = itemCenter - center

                    if (kotlin.math.abs(offset) > 10) {
                        lazyListState.animateScrollBy(
                            value = offset.toFloat(),
                            animationSpec = tween(durationMillis = duration)
                        )
                    }
                } else {
                    // Item is not visible, scroll to it first without animation, then it will be handled in next cycle
                    lazyListState.scrollToItem(targetIndex)
                }
            } finally {
                isAnimating = false
            }
        }
        
        if((currentLineIndex == 0 && shouldScrollToFirstLine) || !initialScrollDone) {
            shouldScrollToFirstLine = false
            // Initial scroll to center the first line with medium animation (600ms)
            val initialCenterIndex = kotlin.math.max(0, currentLineIndex)
            performSmoothPageScroll(initialCenterIndex, 800) // Initial scroll duration
            if(!isAppMinimized) {
                initialScrollDone = true
            }
        } else if (currentLineIndex != -1) {
            deferredCurrentLineIndex = currentLineIndex
            if (isSeeking) {
                // Fast scroll for seeking to center the target line (300ms)
                val seekCenterIndex = kotlin.math.max(0, currentLineIndex - 1)
                performSmoothPageScroll(seekCenterIndex, 500) // Fast seek duration
            } else if (lastPreviewTime == 0L && scrollLyrics && currentLineIndex != previousLineIndex) {
                // Auto-scroll only when user hasn't manually scrolled away
                performSmoothPageScroll(currentLineIndex, 1500)
            }
        }
        if(currentLineIndex > 0) {
            shouldScrollToFirstLine = true
        }
        previousLineIndex = currentLineIndex
    }

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 12.dp)
    ) {

        if (lyrics == LYRICS_NOT_FOUND) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.lyrics_not_found),
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(0.5f)
                )
            }
        } else {
            LazyColumn(
            state = lazyListState,
            contentPadding = WindowInsets.systemBars
                .only(WindowInsetsSides.Top)
                .add(WindowInsets(top = maxHeight / 3, bottom = maxHeight / 2))
                .asPaddingValues(),
            modifier = Modifier
                .fadingEdge(top = 120.dp, bottom = 64.dp)
        ) {
            val displayedCurrentLineIndex =
                if (isSeeking || isSelectionModeActive) deferredCurrentLineIndex else currentLineIndex

            if (lyrics == null) {
                item {
                    ShimmerHost {
                        repeat(10) {
                            Box(
                                contentAlignment = when (lyricsTextPosition) {
                                    LyricsPosition.LEFT -> Alignment.CenterStart
                                    LyricsPosition.CENTER -> Alignment.Center
                                    LyricsPosition.RIGHT -> Alignment.CenterEnd
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 4.dp)
                            ) {
                                TextPlaceholder()
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = lines,
                    key = { index, item -> "$index-${item.time}" } // Add stable key
                ) { index, item ->
                    val isSelected = selectedIndices.contains(index)
                    val isActive = index == displayedCurrentLineIndex && isSynced
                    val isManualScrolling = lastPreviewTime != 0L
                    val distance = if (isManualScrolling) 0 else kotlin.math.abs(index - displayedCurrentLineIndex)

                    // Compute estimated word timing for word-level animation styles
                    val nextEntryTime = lines.getOrNull(index + 1)?.time
                    val lineDuration = remember(item.time, nextEntryTime) {
                        if (nextEntryTime != null) nextEntryTime - item.time else 4000L
                    }
                    val activeDuration = remember(lineDuration) {
                        (lineDuration * 0.95).toLong().coerceAtLeast(300L)
                    }
                    val lineRelTime = (currentPlaybackPosition - item.time).coerceAtLeast(0L)

                    // Target values for animation - style-dependent
                    val targetScale = when (lyricsAnimationStyle) {
                        LyricsAnimationStyle.VIVIMUSIC_1,
                        LyricsAnimationStyle.APPLE_V2 -> if (isActive) 1.05f else 1f
                        LyricsAnimationStyle.LYRICS_V2 -> when {
                            !isSynced || isActive -> 1.08f
                            distance == 1 -> 0.98f
                            else -> 0.92f
                        }
                        else -> when {
                            !isSynced || isActive -> 1.05f 
                            distance == 1 -> 0.95f 
                            distance >= 2 -> 0.85f  
                            else -> 1f
                        }
                    }

                    val targetAlpha = when (lyricsAnimationStyle) {
                        LyricsAnimationStyle.VIVIMUSIC_1 -> when {
                            !isSynced || (isSelectionModeActive && isSelected) -> 1f
                            isActive -> 1f
                            distance == 1 -> 0.65f
                            distance == 2 -> 0.45f
                            else -> 0.35f
                        }
                        LyricsAnimationStyle.APPLE,
                        LyricsAnimationStyle.APPLE_V2 -> when {
                            !isSynced || (isSelectionModeActive && isSelected) -> 1f
                            isActive -> 1f
                            distance == 1 -> 0.55f
                            distance == 2 -> 0.4f
                            else -> 0.3f
                        }
                        else -> when {
                            !isSynced || (isSelectionModeActive && isSelected) -> 1f
                            isActive -> 1f
                            distance == 1 -> 0.6f
                            distance == 2 -> 0.3f
                            else -> 0.15f
                        }
                    }

                    // Progressive blur for VIVIMUSIC_1 style
                    val targetBlur = if (
                        !isManualScrolling &&
                        lyricsAnimationStyle == LyricsAnimationStyle.VIVIMUSIC_1 &&
                        appleMusicLyricsBlur && !isActive && isSynced && !isSelectionModeActive
                    ) {
                        when (distance) {
                            1 -> 0f
                            2 -> 0f
                            3 -> 2f
                            4 -> 4f
                            else -> 6f
                        }
                    } else 0f

                    val animatedBlur by animateFloatAsState(
                        targetValue = targetBlur,
                        animationSpec = tween(durationMillis = 1000),
                        label = "blur"
                    )
                    
                    val animatedScale by animateFloatAsState(
                        targetValue = targetScale,
                        animationSpec = when (lyricsAnimationStyle) {
                            LyricsAnimationStyle.VIVIMUSIC_1,
                            LyricsAnimationStyle.APPLE_V2 -> tween(durationMillis = 400)
                            LyricsAnimationStyle.LYRICS_V2 -> spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                            else -> spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        },
                        label = "scale"
                    )

                    val animatedAlpha by animateFloatAsState(
                        targetValue = targetAlpha,
                        animationSpec = when (lyricsAnimationStyle) {
                            LyricsAnimationStyle.VIVIMUSIC_1,
                            LyricsAnimationStyle.APPLE_V2 -> tween(durationMillis = 300)
                            else -> spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        },
                        label = "alpha"
                    )

                    val itemModifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)) // Clip for background
                        .combinedClickable(
                            enabled = true,
                            onClick = {
                                if (isSelectionModeActive) {
                                    // Toggle selection
                                    if (isSelected) {
                                        selectedIndices.remove(index)
                                        if (selectedIndices.isEmpty()) {
                                            isSelectionModeActive =
                                                false // Exit mode if last item deselected
                                        }
                                    } else {
                                        if (selectedIndices.size < maxSelectionLimit) {
                                            selectedIndices.add(index)
                                        } else {
                                            showMaxSelectionToast = true
                                        }
                                    }
                                } else if (isSynced && changeLyrics) {
                                    playerConnection.player.seekTo(item.time)
                                    lastPreviewTime = 0L
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (!isSelectionModeActive) {
                                    isSelectionModeActive = true
                                    selectedIndices.add(index)
                                } else if (!isSelected && selectedIndices.size < maxSelectionLimit) {
                                    // If already in selection mode and item not selected, add it if below limit
                                    selectedIndices.add(index)
                                } else if (!isSelected) {
                                    // If already at limit, show toast
                                    showMaxSelectionToast = true
                                }
                            }
                        )
                        .background(
                            if (isSelected && isSelectionModeActive) MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.3f
                            )
                            else Color.Transparent
                        )
                        .padding(horizontal = 24.dp, vertical = lyricsLineSpacing.dp)
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                            alpha = animatedAlpha
                        }
                        .then(if (animatedBlur > 0f) Modifier.blur(animatedBlur.dp) else Modifier)

                    Column(
                        modifier = itemModifier,
                        horizontalAlignment = when (lyricsTextPosition) {
                            LyricsPosition.LEFT -> Alignment.Start
                            LyricsPosition.CENTER -> Alignment.CenterHorizontally
                            LyricsPosition.RIGHT -> Alignment.End
                        }
                    ) {
                        // Collect translated/romanized text
                        val translatedText by item.translatedTextFlow.collectAsState()
                        val romanizedText by item.romanizedTextFlow.collectAsState()
                        
                        // Determine what text to display based on mode and availability
                        val isTranslationError: Boolean
                        
                        // Check for translation errors
                        isTranslationError = translatedText?.startsWith("⚠️") == true || translatedText?.startsWith("Error:") == true
                        
                        // Skip rendering if original text is empty
                        if (item.text.isBlank()) {
                            return@itemsIndexed
                        }
                        
                        // Display the selected text
                        val currentTextColor = when {
                            playerBackground != PlayerBackgroundStyle.DEFAULT -> Color.White
                            isActive && palette.isNotEmpty() -> palette.first()
                            else -> textColor
                        }
                        
                        // Calculate secondary text first to determine if we need to show it
                        val secondaryText: String?
                        val showSecondaryText: Boolean
                        
                        when (translateMode) {
                            "Literal" -> {
                                // Show translation below original
                                secondaryText = translatedText
                                showSecondaryText = translatedText != null && !isTranslationError && item.text.isNotBlank()
                            }
                            "Transcribed" -> {
                                // Show AI transcription if available, otherwise local romanization
                                val aiTranscription = if (translatedText != null && !isTranslationError) translatedText else null
                                secondaryText = aiTranscription ?: romanizedText
                                showSecondaryText = secondaryText != null && item.text.isNotBlank()
                            }
                            else -> {
                                secondaryText = translatedText
                                showSecondaryText = translatedText != null && !isTranslationError && item.text.isNotBlank()
                            }
                        }
                        
                        val textAlignment = when (lyricsTextPosition) {
                            LyricsPosition.LEFT -> TextAlign.Left
                            LyricsPosition.CENTER -> TextAlign.Center
                            LyricsPosition.RIGHT -> TextAlign.Right
                        }

                        // Compute estimated word data for word-level styles
                        val wordData = remember(item.text, activeDuration) {
                            val words = item.text.split(" ").filter { it.isNotEmpty() }
                            if (words.isEmpty()) {
                                listOf(Triple(item.text, 0L, activeDuration))
                            } else {
                                val totalChars = item.text.length
                                var accumulatedTime = 0L
                                words.mapIndexed { wordIndex, word ->
                                    val wordLength = word.length
                                    val includeSpace = wordIndex < words.lastIndex
                                    val charCount = if (includeSpace) wordLength + 1 else wordLength
                                    val wordStart = accumulatedTime
                                    val wordDur = if (totalChars > 0) (activeDuration * charCount.toFloat() / totalChars).toLong() else activeDuration
                                    val wordEnd = wordStart + wordDur
                                    accumulatedTime += wordDur
                                    // Include trailing space in word text so FlowRow wraps the word+space
                                    // as one atomic unit, preventing orphaned leading spaces on wrapped lines
                                    Triple(if (includeSpace) "$word " else word, wordStart, wordEnd)
                                }
                            }
                        }

                        // Render based on animation style
                        when (lyricsAnimationStyle) {
                            LyricsAnimationStyle.VIVIMUSIC_1 -> {
                                // Apple Music premium style with word-by-word gradient fill
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = when (textAlignment) {
                                        TextAlign.Center -> Arrangement.Center
                                        TextAlign.Right -> Arrangement.End
                                        else -> Arrangement.Start
                                    },
                                    verticalArrangement = Arrangement.spacedBy(
                                        with(LocalDensity.current) { (lyricsTextSize * (lyricsLineSpacing - 1f)).sp.toDp() }
                                    )
                                ) {
                                    wordData.forEachIndexed { wordIndex, (wordText, startRelative, endRelative) ->
                                        val wordDuration = (endRelative - startRelative).coerceAtLeast(1L)
                                        
                                        val progress by animateFloatAsState(
                                            targetValue = when {
                                                lineRelTime >= endRelative -> 1f
                                                lineRelTime < startRelative -> 0f
                                                else -> (lineRelTime - startRelative).toFloat() / wordDuration
                                            },
                                            animationSpec = tween(durationMillis = 150, easing = androidx.compose.animation.core.LinearEasing),
                                            label = "wordProgress"
                                        )

                                        val finalFontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold

                                        Text(
                                            text = wordText,
                                            fontSize = lyricsTextSize.sp,
                                            style = TextStyle(
                                                brush = if (isActive) Brush.horizontalGradient(
                                                    0.0f to currentTextColor,
                                                    (progress - 0.05f).coerceAtLeast(0f) to currentTextColor,
                                                    (progress + 0.05f).coerceAtMost(1f) to currentTextColor.copy(alpha = 0.45f),
                                                    1.0f to currentTextColor.copy(alpha = 0.45f)
                                                ) else null,
                                                fontWeight = finalFontWeight,
                                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                                textAlign = textAlignment,
                                                shadow = if (isActive && (lyricsGlowEffect || progress > 0.1f)) Shadow(
                                                    color = currentTextColor.copy(alpha = 0.6f * progress),
                                                    offset = Offset.Zero,
                                                    blurRadius = (12f * progress).coerceAtLeast(0.1f)
                                                ) else null
                                            ),
                                            color = if (!isActive) currentTextColor else Color.Unspecified
                                        )
                                    }
                                }
                            }

                            LyricsAnimationStyle.APPLE_V2 -> {
                                // Character-level animation with FlowRow
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = when (textAlignment) {
                                        TextAlign.Center -> Arrangement.Center
                                        TextAlign.Right -> Arrangement.End
                                        else -> Arrangement.Start
                                    }
                                ) {
                                    wordData.forEachIndexed { wordIndex, (wordText, startRelative, endRelative) ->
                                        val wordDuration = (endRelative - startRelative).coerceAtLeast(1L)

                                        Row {
                                            wordText.forEachIndexed { charIndex, char ->
                                                val charDuration = if (wordText.isNotEmpty()) wordDuration / wordText.length else 0L
                                                val charStart = startRelative + (charIndex * charDuration)
                                                val charEnd = charStart + charDuration

                                                val charProgress = when {
                                                    !isActive -> 0f
                                                    lineRelTime >= charEnd -> 1f
                                                    lineRelTime < charStart -> 0f
                                                    else -> {
                                                        if (charDuration <= 0L) 1f
                                                        else (lineRelTime - charStart).toFloat() / charDuration
                                                    }
                                                }

                                                val sinProgress = kotlin.math.sin(charProgress * Math.PI).toFloat()
                                                val charScale = 1f + (0.015f * sinProgress)
                                                val charAlpha = if (isActive) {
                                                    0.35f + (0.65f * charProgress)
                                                } else 1f

                                                Text(
                                                    text = char.toString(),
                                                    fontSize = lyricsTextSize.sp,
                                                    color = currentTextColor.copy(alpha = charAlpha),
                                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                                                    lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                                    modifier = Modifier.graphicsLayer {
                                                        scaleX = charScale
                                                        scaleY = charScale
                                                    },
                                                    style = TextStyle(
                                                        shadow = if (isActive && charProgress > 0.3f && lyricsGlowEffect) Shadow(
                                                            color = currentTextColor.copy(alpha = 0.45f * charProgress),
                                                            offset = Offset.Zero,
                                                            blurRadius = 12f * charProgress
                                                        ) else null
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            LyricsAnimationStyle.LYRICS_V2 -> {
                                // Dual-layer with bounce animation
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = when (textAlignment) {
                                        TextAlign.Center -> Arrangement.Center
                                        TextAlign.Right -> Arrangement.End
                                        else -> Arrangement.Start
                                    }
                                ) {
                                    wordData.forEachIndexed { wordIndex, (wordText, startRelative, endRelative) ->
                                        val wordDuration = (endRelative - startRelative).coerceAtLeast(1L)
                                        val isWordComplete = !isActive || lineRelTime >= endRelative
                                        val isWordActive = isActive && lineRelTime in startRelative until endRelative

                                        val progress = when {
                                            isWordComplete && isActive -> 1f
                                            !isActive -> 0f
                                            isWordActive -> ((lineRelTime - startRelative).toFloat() / wordDuration).coerceIn(0f, 1f)
                                            lineRelTime < startRelative -> 0f
                                            else -> 1f
                                        }

                                        val sinProgress = kotlin.math.sin(progress * Math.PI).toFloat()
                                        val wordScale = 1f + (0.015f * sinProgress)
                                        val glowAlpha = if (isWordActive) (progress * 2f).coerceAtMost(1f) * 0.45f else 0f
                                        val glowRadius = if (isWordActive) (progress * 2f).coerceAtMost(1f) * 12f else 0f

                                        Box(
                                            modifier = Modifier.graphicsLayer {
                                                scaleX = wordScale
                                                scaleY = wordScale
                                            }
                                        ) {
                                            // Layer 1: Base text (dimmed)
                                            Text(
                                                text = wordText,
                                                fontSize = lyricsTextSize.sp,
                                                color = currentTextColor.copy(alpha = if (isActive) 0.35f else 0.7f),
                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                                            )
                                            // Layer 2: Filled overlay
                                            if (isWordComplete || isWordActive) {
                                                Text(
                                                    text = wordText,
                                                    fontSize = lyricsTextSize.sp,
                                                    color = currentTextColor,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                                    style = TextStyle(
                                                        shadow = if (glowAlpha > 0f) Shadow(
                                                            color = currentTextColor.copy(alpha = glowAlpha),
                                                            offset = Offset.Zero,
                                                            blurRadius = glowRadius.coerceAtLeast(1f)
                                                        ) else null
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            LyricsAnimationStyle.SLIDE -> {
                                // Horizontal gradient sweep on active line
                                val lineProgress = if (isActive && activeDuration > 0) {
                                    (lineRelTime.toFloat() / activeDuration).coerceIn(0f, 1f)
                                } else if (isActive) 1f else 0f

                                Text(
                                    text = item.text,
                                    fontSize = lyricsTextSize.sp,
                                    lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                    style = TextStyle(
                                        brush = if (isActive) Brush.horizontalGradient(
                                            0.0f to currentTextColor,
                                            (lineProgress * 0.95f).coerceIn(0f, 1f) to currentTextColor,
                                            lineProgress to currentTextColor.copy(alpha = 0.9f),
                                            (lineProgress + 0.02f).coerceIn(0f, 1f) to currentTextColor.copy(alpha = 0.5f),
                                            (lineProgress + 0.08f).coerceIn(0f, 1f) to currentTextColor.copy(alpha = 0.35f),
                                            1.0f to currentTextColor.copy(alpha = 0.35f)
                                        ) else null,
                                        shadow = if (isActive) Shadow(
                                            color = currentTextColor.copy(alpha = 0.4f * lineProgress),
                                            offset = Offset.Zero,
                                            blurRadius = 14f + (4f * lineProgress)
                                        ) else Shadow.None,
                                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                                    ),
                                    color = if (!isActive) textColor.copy(alpha = 0.8f) else Color.Unspecified,
                                    textAlign = textAlignment,
                                )
                            }

                            LyricsAnimationStyle.KARAOKE -> {
                                // Enhanced gradient with glow
                                val lineProgress = if (isActive && activeDuration > 0) {
                                    val linear = (lineRelTime.toFloat() / activeDuration).coerceIn(0f, 1f)
                                    linear * linear * (3f - 2f * linear) // smoothstep
                                } else if (isActive) 1f else 0f
                                val glowIntensity = lineProgress * lineProgress

                                Text(
                                    text = item.text,
                                    fontSize = lyricsTextSize.sp,
                                    lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                    style = TextStyle(
                                        brush = if (isActive) Brush.horizontalGradient(
                                            0.0f to currentTextColor.copy(alpha = 0.4f),
                                            (lineProgress * 0.6f).coerceIn(0f, 1f) to currentTextColor.copy(alpha = 0.75f),
                                            (lineProgress * 0.85f).coerceIn(0f, 1f) to currentTextColor.copy(alpha = 0.95f),
                                            lineProgress to currentTextColor,
                                            (lineProgress + 0.03f).coerceIn(0f, 1f) to currentTextColor.copy(alpha = 0.85f),
                                            (lineProgress + 0.1f).coerceIn(0f, 1f) to currentTextColor.copy(alpha = 0.5f),
                                            1.0f to currentTextColor.copy(alpha = if (lineProgress >= 0.9f) 0.95f else 0.4f)
                                        ) else null,
                                        shadow = if (isActive) Shadow(
                                            color = currentTextColor.copy(alpha = 0.5f + (0.3f * glowIntensity)),
                                            offset = Offset.Zero,
                                            blurRadius = 16f + (12f * glowIntensity)
                                        ) else Shadow.None,
                                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                                    ),
                                    color = if (!isActive) textColor.copy(alpha = 0.8f) else Color.Unspecified,
                                    textAlign = textAlignment,
                                )
                            }

                            LyricsAnimationStyle.GLOW -> {
                                // Intense neon glow
                                val lineProgress = if (isActive && activeDuration > 0) {
                                    val linear = (lineRelTime.toFloat() / activeDuration).coerceIn(0f, 1f)
                                    linear * linear * (3f - 2f * linear)
                                } else if (isActive) 1f else 0f
                                val glowIntensity = lineProgress * lineProgress
                                
                                Text(
                                    text = item.text,
                                    fontSize = lyricsTextSize.sp,
                                    lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                    color = if (isActive) currentTextColor.copy(alpha = 0.45f + (0.55f * lineProgress))
                                        else textColor.copy(alpha = 0.8f),
                                    style = TextStyle(
                                        shadow = if (isActive) Shadow(
                                            color = currentTextColor.copy(alpha = 0.5f + (0.3f * glowIntensity)),
                                            offset = Offset.Zero,
                                            blurRadius = 16f + (12f * glowIntensity)
                                        ) else Shadow.None,
                                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                                    ),
                                    textAlign = textAlignment,
                                )
                            }

                            LyricsAnimationStyle.FADE -> {
                                // Smooth fade with enhanced shadow
                                val lineProgress = if (isActive && activeDuration > 0) {
                                    val linear = (lineRelTime.toFloat() / activeDuration).coerceIn(0f, 1f)
                                    linear * linear * (3f - 2f * linear)
                                } else if (isActive) 1f else 0f
                                
                                Text(
                                    text = item.text,
                                    fontSize = lyricsTextSize.sp,
                                    lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                    color = if (isActive) currentTextColor.copy(alpha = 0.4f + (0.6f * lineProgress))
                                        else textColor.copy(alpha = 0.8f),
                                    style = TextStyle(
                                        shadow = if (isActive && lineProgress > 0.2f) Shadow(
                                            color = currentTextColor.copy(alpha = 0.35f * lineProgress),
                                            offset = Offset.Zero,
                                            blurRadius = 10f * lineProgress
                                        ) else Shadow.None,
                                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                                    ),
                                    textAlign = textAlignment,
                                )
                            }

                            LyricsAnimationStyle.APPLE -> {
                                // Apple Music style with glow halo
                                val lineProgress = if (isActive && activeDuration > 0) {
                                    val raw = (lineRelTime.toFloat() / activeDuration).coerceIn(0f, 1f)
                                    raw * raw * (3f - 2f * raw)
                                } else if (isActive) 1f else 0f
                                val glowIntensity = lineProgress * lineProgress
                                
                                Text(
                                    text = item.text,
                                    fontSize = lyricsTextSize.sp,
                                    lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                    color = if (isActive) currentTextColor.copy(alpha = 0.55f + (0.45f * lineProgress))
                                        else textColor.copy(alpha = 0.8f),
                                    style = TextStyle(
                                        shadow = when {
                                            isActive -> Shadow(
                                                color = currentTextColor.copy(alpha = 0.2f + (0.4f * glowIntensity)),
                                                offset = Offset.Zero,
                                                blurRadius = 10f + (12f * glowIntensity)
                                            )
                                            else -> Shadow.None
                                        },
                                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                                    ),
                                    textAlign = textAlignment,
                                )
                            }

                            else -> {
                                // NONE - clean original style
                                Text(
                                    text = item.text,
                                    fontSize = lyricsTextSize.sp,
                                    lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                    color = if (isActive) {
                                        currentTextColor
                                    } else {
                                        textColor.copy(alpha = 0.8f)
                                    },
                                    style = TextStyle(
                                        shadow = if (isActive) Shadow(
                                            color = currentTextColor.copy(alpha = 0.9f),
                                            blurRadius = 30f
                                        ) else Shadow.None
                                    ),
                                    textAlign = textAlignment,
                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold
                                )
                            }
                        }
                        
                        // SECONDARY TEXT (translation/transcription - below original, smaller)
                        if (showSecondaryText && secondaryText != null) {
                            Text(
                                text = secondaryText,
                                fontSize = (lyricsTextSize * 0.67f).sp,
                                color = if (isActive) {
                                    currentTextColor.copy(alpha = 0.7f)
                                } else {
                                    textColor.copy(alpha = 0.5f)
                                },
                                style = TextStyle(
                                    shadow = if (isActive) Shadow(
                                        color = currentTextColor.copy(alpha = 0.6f),
                                        blurRadius = 15f
                                    ) else Shadow.None
                                ),
                                textAlign = textAlignment,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                            )
                        }
                    }
                }
            }
        }



        // Action buttons: Close and Share buttons grouped together
        if (isSelectionModeActive) {
            mediaMetadata?.let { metadata ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp), // Just above player slider
                    contentAlignment = Alignment.Center
                ) {
                    // Row containing both close and share buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Close button (circular, right side of share)
                        Box(
                            modifier = Modifier
                                .size(48.dp) // Larger for better touch target
                                .background(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    isSelectionModeActive = false
                                    selectedIndices.clear()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.close),
                                contentDescription = stringResource(R.string.cancel),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Share button (rectangular with text)
                        Row(
                            modifier = Modifier
                                .background(
                                    color = if (selectedIndices.isNotEmpty())
                                        Color.White.copy(alpha = 0.9f) // White background when active
                                    else
                                        Color.White.copy(alpha = 0.5f), // Lighter white when inactive
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .clickable(enabled = selectedIndices.isNotEmpty()) {
                                    if (selectedIndices.isNotEmpty()) {
                                        val sortedIndices = selectedIndices.sorted()
                                        val selectedLyricsText = sortedIndices
                                            .mapNotNull { lines.getOrNull(it)?.text }
                                            .joinToString("\n")

                                        if (selectedLyricsText.isNotBlank()) {
                                            shareDialogData = Triple(
                                                selectedLyricsText,
                                                metadata.title,
                                                metadata.artists.joinToString { it.name }
                                            )
                                            showShareDialog = true
                                        }
                                        isSelectionModeActive = false
                                        selectedIndices.clear()
                                    }
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.share),
                                contentDescription = stringResource(R.string.share_selected),
                                tint = Color.Black, // Black icon on white background
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.share),
                                color = Color.Black, // Black text on white background
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        // Removed the more button from bottom - it's now in the top header
    }

    if (showProgressDialog) {
        BasicAlertDialog(onDismissRequest = { /* Don't dismiss */ }) {
            Card( // Use Card for better styling
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.padding(32.dp)) {
                    Text(
                        text = stringResource(R.string.generating_image) + "\n" + stringResource(R.string.please_wait),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    if (showShareDialog && shareDialogData != null) {
        val (lyricsText, songTitle, artists) = shareDialogData!! // Renamed 'lyrics' to 'lyricsText' for clarity
        BasicAlertDialog(onDismissRequest = { showShareDialog = false }) {
            Card(
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.85f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.share_lyrics),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Share as Text Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    val songLink =
                                        "https://music.youtube.com/watch?v=${mediaMetadata?.id}"
                                    // Use the potentially multi-line lyricsText here
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "\"$lyricsText\"\n\n$songTitle - $artists\n$songLink"
                                    )
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        shareIntent,
                                        context.getString(R.string.share_lyrics)
                                    )
                                )
                                showShareDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.share), // Use new share icon
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.share_as_text),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Share as Image Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showShareDialog = false
                                showImageCustomizationDialog = true
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.share), // Use new share icon
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.share_as_image),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Cancel Button Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { showShareDialog = false }
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    }

    if (showImageCustomizationDialog && shareDialogData != null) {
        val (lyricsText, _, _) = shareDialogData!!
        mediaMetadata?.let { metadata ->
            LyricsShareDialog(
                mediaMetadata = metadata,
                lyrics = lyricsText,
                onDismiss = { showImageCustomizationDialog = false },
                onShare = { bitmap ->
                    scope.launch {
                        try {
                            val timestamp = System.currentTimeMillis()
                            val filename = "lyrics_$timestamp"
                            val uri = ComposeToImage.saveBitmapAsFile(context, bitmap, filename)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Lyrics"))
                            showImageCustomizationDialog = false
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                    }
                }
            )
        }


    }
}
}

private const val METROLIST_AUTO_SCROLL_DURATION = 1500L // Much slower auto-scroll for smooth transitions
private const val METROLIST_INITIAL_SCROLL_DURATION = 1000L // Slower initial positioning
private const val METROLIST_SEEK_DURATION = 800L // Slower user interaction
private const val METROLIST_FAST_SEEK_DURATION = 600L // Less aggressive seeking

// Lyrics constants
val LyricsPreviewTime = 2.seconds
