

package iad1tya.echo.music.ui.component

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.text.Layout
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import iad1tya.echo.music.LocalDatabase
import iad1tya.echo.music.LocalListenTogetherManager
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.DarkModeKey
import iad1tya.echo.music.constants.LyricsAnimationStyle
import iad1tya.echo.music.constants.LyricsAnimationStyleKey
import iad1tya.echo.music.constants.LyricsClickKey
import iad1tya.echo.music.constants.LyricsGlowEffectKey
import iad1tya.echo.music.constants.LyricsLineSpacingKey
import iad1tya.echo.music.constants.LyricsRomanizeAsMainKey
import iad1tya.echo.music.constants.LyricsRomanizeBelarusianKey
import iad1tya.echo.music.constants.LyricsRomanizeBulgarianKey
import iad1tya.echo.music.constants.LyricsRomanizeChineseKey
import iad1tya.echo.music.constants.LyricsRomanizeCyrillicByLineKey
import iad1tya.echo.music.constants.LyricsRomanizeHindiKey
import iad1tya.echo.music.constants.LyricsRomanizePunjabiKey
import iad1tya.echo.music.constants.LyricsRomanizeJapaneseKey
import iad1tya.echo.music.constants.LyricsRomanizeKoreanKey
import iad1tya.echo.music.constants.LyricsRomanizeKyrgyzKey
import iad1tya.echo.music.constants.LyricsRomanizeMacedonianKey
import iad1tya.echo.music.constants.LyricsRomanizeRussianKey
import iad1tya.echo.music.constants.LyricsRomanizeSerbianKey
import iad1tya.echo.music.constants.LyricsRomanizeUkrainianKey
import iad1tya.echo.music.constants.LyricsStandardBlurKey
import iad1tya.echo.music.constants.LyricsScrollKey
import iad1tya.echo.music.constants.LyricsTextPositionKey
import iad1tya.echo.music.constants.LyricsTextSizeKey
import iad1tya.echo.music.constants.PlayerBackgroundStyle
import iad1tya.echo.music.constants.OpenRouterApiKey
import iad1tya.echo.music.constants.DeeplApiKey
import iad1tya.echo.music.constants.AiProviderKey
import iad1tya.echo.music.constants.OpenRouterBaseUrlKey
import iad1tya.echo.music.constants.OpenRouterModelKey
import iad1tya.echo.music.constants.TranslateLanguageKey
import iad1tya.echo.music.constants.TranslateModeKey
import iad1tya.echo.music.constants.DeeplFormalityKey
import iad1tya.echo.music.constants.PlayerBackgroundStyleKey
import iad1tya.echo.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import iad1tya.echo.music.lyrics.LyricsEntry
import iad1tya.echo.music.lyrics.LyricsUtils.findCurrentLineIndex
import iad1tya.echo.music.lyrics.LyricsUtils.isBelarusian
import iad1tya.echo.music.lyrics.LyricsUtils.isBulgarian
import iad1tya.echo.music.lyrics.LyricsUtils.isChinese
import iad1tya.echo.music.lyrics.LyricsUtils.isHindi
import iad1tya.echo.music.lyrics.LyricsUtils.isPunjabi
import iad1tya.echo.music.lyrics.LyricsUtils.isJapanese
import iad1tya.echo.music.lyrics.LyricsUtils.isKorean
import iad1tya.echo.music.lyrics.LyricsUtils.isKyrgyz
import iad1tya.echo.music.lyrics.LyricsUtils.isMacedonian
import iad1tya.echo.music.lyrics.LyricsUtils.isRussian
import iad1tya.echo.music.lyrics.LyricsUtils.isSerbian
import iad1tya.echo.music.lyrics.LyricsUtils.isUkrainian
import iad1tya.echo.music.lyrics.LyricsUtils.parseLyrics
import iad1tya.echo.music.lyrics.LyricsUtils.romanizeChinese
import iad1tya.echo.music.lyrics.LyricsUtils.romanizeHindi
import iad1tya.echo.music.lyrics.LyricsUtils.romanizePunjabi
import iad1tya.echo.music.lyrics.LyricsUtils.romanizeCyrillic
import iad1tya.echo.music.lyrics.LyricsUtils.romanizeJapanese
import iad1tya.echo.music.lyrics.LyricsUtils.romanizeKorean
import iad1tya.echo.music.lyrics.LyricsTranslationHelper
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("UnusedBoxWithConstraintsScope", "StringFormatInvalid")
@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    showLyrics: Boolean
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalWindowInfo.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost

    val lyricsTextPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.LEFT)
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
    val romanizeAsMain by rememberPreference(LyricsRomanizeAsMainKey, false)
    val romanizeChineseLyrics by rememberPreference(LyricsRomanizeChineseKey, true)
    val romanizeHindiLyrics by rememberPreference(LyricsRomanizeHindiKey, true)
    val romanizePunjabiLyrics by rememberPreference(LyricsRomanizePunjabiKey, true)
    val lyricsGlowEffect by rememberPreference(LyricsGlowEffectKey, false)
    val lyricsAnimationStyle by rememberEnumPreference(LyricsAnimationStyleKey, LyricsAnimationStyle.echomusic_1)
    val lyricsTextSize by rememberPreference(LyricsTextSizeKey, 24f)
    val lyricsLineSpacing by rememberPreference(LyricsLineSpacingKey, 1.3f)
    val lyricsStandardBlur by rememberPreference(LyricsStandardBlurKey, false)
    
    val openRouterApiKey by rememberPreference(OpenRouterApiKey, "")
    val deeplApiKey by rememberPreference(DeeplApiKey, "")
    val aiProvider by rememberPreference(AiProviderKey, "OpenRouter")
    val openRouterBaseUrl by rememberPreference(OpenRouterBaseUrlKey, "https://openrouter.ai/api/v1/chat/completions")
    val openRouterModel by rememberPreference(OpenRouterModelKey, "google/gemini-2.5-flash-lite")
    val translateLanguage by rememberPreference(TranslateLanguageKey, "en")
    val translateMode by rememberPreference(TranslateModeKey, "Literal")
    val deeplFormality by rememberPreference(DeeplFormalityKey, "default")
    
    val scope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val lyrics = remember(lyricsEntity) { lyricsEntity?.lyrics?.trim() }

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.GRADIENT
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
                val newEntry = LyricsEntry(entry.time, entry.text, entry.words, agent = entry.agent, isBackground = entry.isBackground)
                
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

                else if (romanizeChineseLyrics && isChinese(entry.text)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeChinese(entry.text)
                    }
                }

                else if (romanizeHindiLyrics && isHindi(entry.text)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeHindi(entry.text)
                    }
                }

                else if (romanizePunjabiLyrics && isPunjabi(entry.text)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizePunjabi(entry.text)
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

                else if (romanizeChineseLyrics && isChinese(line)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeChinese(line)
                    }
                }

                else if (romanizeHindiLyrics && isHindi(line)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeHindi(line)
                    }
                }

                else if (romanizePunjabiLyrics && isPunjabi(line)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizePunjabi(line)
                    }
                }

                newEntry
            }
        }
    }
    val isSynced =
        remember(lyrics) {
            !lyrics.isNullOrEmpty() && lyrics.startsWith("[")
        }

    
    val translationStatus by LyricsTranslationHelper.status.collectAsState()
    val hasActiveTranslations by LyricsTranslationHelper.hasActiveTranslations.collectAsState()
    
    
    DisposableEffect(Unit) {
        LyricsTranslationHelper.setCompositionActive(true)
        onDispose {
            LyricsTranslationHelper.setCompositionActive(false)
            LyricsTranslationHelper.cancelTranslation()
        }
    }
    
    
    LaunchedEffect(lines, lyricsEntity, translateLanguage, translateMode) {
        if (lines.isNotEmpty() && lyricsEntity != null) {
            LyricsTranslationHelper.loadTranslationsFromDatabase(
                lyrics = lines,
                lyricsEntity = lyricsEntity,
                targetLanguage = translateLanguage,
                mode = translateMode
            )
        }
    }
    
    
    LaunchedEffect(showLyrics, lines.size) {
        LyricsTranslationHelper.manualTrigger.collect {
            val effectiveApiKey = if (aiProvider == "DeepL") deeplApiKey else openRouterApiKey
            if (showLyrics && lines.isNotEmpty() && effectiveApiKey.isNotBlank()) {
                LyricsTranslationHelper.translateLyrics(
                    lyrics = lines,
                    targetLanguage = translateLanguage,
                    apiKey = openRouterApiKey,
                    baseUrl = openRouterBaseUrl,
                    model = openRouterModel,
                    mode = translateMode,
                    scope = scope,
                    context = context,
                    provider = aiProvider,
                    deeplApiKey = deeplApiKey,
                    deeplFormality = deeplFormality,
                    useStreaming = true,
                    songId = currentSong?.id ?: "",
                    database = database
                )
            } else if (effectiveApiKey.isBlank()) {
                Toast.makeText(context, context.getString(R.string.ai_api_key_required), Toast.LENGTH_SHORT).show()
            }
        }
    }

    
    LaunchedEffect(Unit) {
        LyricsTranslationHelper.clearTranslationsTrigger.collect {
            lines.forEach { it.translatedTextFlow.value = null }
        }
    }

    
    val expressiveAccent = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.primary
        PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.GLOW_ANIMATED, PlayerBackgroundStyle.APPLE_MUSIC, PlayerBackgroundStyle.LIVE_MESH -> {
            
            Color.White
        }
    }
    val textColor = expressiveAccent

    var currentLineIndex by remember {
        mutableIntStateOf(-1)
    }
    var currentPlaybackPosition by remember {
        mutableLongStateOf(0L)
    }
    
    
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
    var shareDialogData by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    var showColorPickerDialog by remember { mutableStateOf(false) }
    var previewBackgroundColor by remember { mutableStateOf(Color(0xFF242424)) }
    var previewTextColor by remember { mutableStateOf(Color.White) }
    var previewSecondaryTextColor by remember { mutableStateOf(Color.White.copy(alpha = 0.7f)) }

    
    var isSelectionModeActive by rememberSaveable { mutableStateOf(false) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    var showMaxSelectionToast by remember { mutableStateOf(false) } 

    val isLyricsProviderShown = lyricsEntity?.provider != null && lyricsEntity?.provider != "Unknown" && !isSelectionModeActive

    val lazyListState = rememberLazyListState()
    
    
    var isAnimating by remember { mutableStateOf(false) }
    var isAutoScrollEnabled by rememberSaveable { mutableStateOf(true) }

    
    BackHandler(enabled = isSelectionModeActive) {
        isSelectionModeActive = false
        selectedIndices.clear()
    }

    
    val maxSelectionLimit = 5

    
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

    
    DisposableEffect(showLyrics) {
        val activity = context as? Activity
        if (showLyrics) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

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
            androidx.compose.runtime.withFrameMillis { }
            val sliderPosition = sliderPositionProvider()
            isSeeking = sliderPosition != null
            val position = sliderPosition ?: playerConnection.player.currentPosition
            currentPlaybackPosition = position
            val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
            currentLineIndex = findCurrentLineIndex(lines, position + lyricsOffset)
        }
    }

    LaunchedEffect(isSeeking, lastPreviewTime) {
        if (isSeeking) {
            lastPreviewTime = 0L
        } else if (lastPreviewTime != 0L) {
            delay(LyricsPreviewTime)
            lastPreviewTime = 0L
        }
    }

    suspend fun performSmoothPageScroll(targetIndex: Int, duration: Int = 1500) {
        if (isAnimating) return 
        isAnimating = true
        try {
            val lookUpIndex = if (isLyricsProviderShown) targetIndex + 1 else targetIndex
            val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == lookUpIndex }
            if (itemInfo != null) {
                
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
                
                lazyListState.scrollToItem(targetIndex)
            }
        } finally {
            isAnimating = false
        }
    }
    LaunchedEffect(currentLineIndex, lastPreviewTime, initialScrollDone, isAutoScrollEnabled) {
        if (!isSynced) return@LaunchedEffect
        if (isAutoScrollEnabled) {
        if((currentLineIndex == 0 && shouldScrollToFirstLine) || !initialScrollDone) {
            shouldScrollToFirstLine = false
            
            val initialCenterIndex = kotlin.math.max(0, currentLineIndex)
            performSmoothPageScroll(initialCenterIndex, 800) 
            if(!isAppMinimized) {
                initialScrollDone = true
            }
        } else if (currentLineIndex != -1) {
            deferredCurrentLineIndex = currentLineIndex
            if (isSeeking) {
                
                val seekCenterIndex = kotlin.math.max(0, currentLineIndex)
                performSmoothPageScroll(seekCenterIndex, 500) 
            } else if ((lastPreviewTime == 0L || currentLineIndex != previousLineIndex) && scrollLyrics) {
                
                if (currentLineIndex != previousLineIndex) {
                    
                    val centerTargetIndex = currentLineIndex
                    performSmoothPageScroll(centerTargetIndex, 1500) 
                }
            }
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
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .padding(top = 56.dp),
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
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.ai_translating_lyrics),
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
                                text = stringResource(R.string.ai_lyrics_translated),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                is LyricsTranslationHelper.TranslationStatus.Idle -> {
                    
                }
            }
        }

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
                .fadingEdge(vertical = 64.dp)
                .nestedScroll(remember {
                    object : NestedScrollConnection {
                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            if (source == NestedScrollSource.UserInput) {
                                isAutoScrollEnabled = false
                            }
                            if (!isSelectionModeActive) { 
                                lastPreviewTime = System.currentTimeMillis()
                            }
                            return super.onPostScroll(consumed, available, source)
                        }

                        override suspend fun onPostFling(
                            consumed: Velocity,
                            available: Velocity
                        ): Velocity {
                            isAutoScrollEnabled = false
                            if (!isSelectionModeActive) { 
                                lastPreviewTime = System.currentTimeMillis()
                            }
                            return super.onPostFling(consumed, available)
                        }
                    }
                })
        ) {
            val displayedCurrentLineIndex = if (!isAutoScrollEnabled) {
                currentLineIndex
            } else {
                if (isSeeking || isSelectionModeActive) deferredCurrentLineIndex else currentLineIndex
            }

            
            if (isLyricsProviderShown) {
                item {
                    Text(
                        text = "Lyrics from ${lyricsEntity?.provider}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

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
                val lyricsOffset = currentSong?.song?.lyricsOffset?.toLong() ?: 0L
                val effectivePlaybackPosition = currentPlaybackPosition + lyricsOffset

                itemsIndexed(
                    items = lines,
                    key = { index, item -> "$index-${item.time}" } 
                ) { index, item ->
                    val isSelected = selectedIndices.contains(index)
                    if (lyricsAnimationStyle == LyricsAnimationStyle.echomusic_1 && item.words?.isNotEmpty() == true) {
                        val currentLineTime = if (displayedCurrentLineIndex >= 0 && displayedCurrentLineIndex < lines.size) {
                            lines[displayedCurrentLineIndex].time
                        } else -1L
                        val isLineAtSameTime = item.time == currentLineTime
                        val isActiveByIndex = index == displayedCurrentLineIndex
                        val isActiveByTime = isLineAtSameTime && displayedCurrentLineIndex >= 0

                        echomusicLyricsLine(
                            entry = item,
                            nextEntryTime = lines.getOrNull(index + 1)?.time,
                            effectivePlaybackPosition = effectivePlaybackPosition,
                            isSynced = isSynced,
                            isActive = isActiveByIndex || isActiveByTime,
                            distanceFromCurrent = kotlin.math.abs(index - displayedCurrentLineIndex),
                            lyricsTextPosition = lyricsTextPosition,
                            textColor = textColor,
                            showRomanized = currentSong?.romanizeLyrics == true && (
                                    romanizeJapaneseLyrics ||
                                            romanizeKoreanLyrics ||
                                            romanizeRussianLyrics ||
                                            romanizeUkrainianLyrics ||
                                            romanizeSerbianLyrics ||
                                            romanizeBulgarianLyrics ||
                                            romanizeBelarusianLyrics ||
                                            romanizeKyrgyzLyrics ||
                                            romanizeMacedonianLyrics ||
                                            romanizeChineseLyrics ||
                                            romanizeHindiLyrics ||
                                            romanizePunjabiLyrics),
                            textSize = lyricsTextSize,
                            lineSpacing = lyricsLineSpacing,
                            showTranslated = hasActiveTranslations,
                            isAutoScrollActive = isAutoScrollEnabled,
                            isSelectionModeActive = isSelectionModeActive,
                            isSelected = isSelected,
                            expressiveAccent = expressiveAccent,
                            onClick = {
                                if (isSelectionModeActive) {
                                    if (isSelected) {
                                        selectedIndices.remove(index)
                                        if (selectedIndices.isEmpty()) isSelectionModeActive = false
                                    } else {
                                        if (selectedIndices.size < maxSelectionLimit) selectedIndices.add(index)
                                        else showMaxSelectionToast = true
                                    }
                                } else if (isSynced && changeLyrics && !isGuest) {
                                    val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
                                    playerConnection.seekTo((item.time - lyricsOffset).coerceAtLeast(0))
                                    scope.launch {
                                        lazyListState.scrollToItem(index = index)
                                        val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                        if (itemInfo != null) {
                                            val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                                            val center = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                                            val itemCenter = itemInfo.offset + itemInfo.size / 2
                                            val offset = itemCenter - center
                                            if (kotlin.math.abs(offset) > 10) {
                                                lazyListState.animateScrollBy(
                                                    value = offset.toFloat(),
                                                    animationSpec = tween(durationMillis = 1500)
                                                )
                                            }
                                        }
                                    }
                                    lastPreviewTime = 0L
                                }
                            },
                            onLongClick = {
                                if (!isSelectionModeActive) {
                                    isSelectionModeActive = true
                                    selectedIndices.add(index)
                                } else if (!isSelected && selectedIndices.size < maxSelectionLimit) {
                                    selectedIndices.add(index)
                                } else if (!isSelected) {
                                    showMaxSelectionToast = true
                                }
                            }
                        )
                        return@itemsIndexed
                    } else if (lyricsAnimationStyle == LyricsAnimationStyle.METRO_LYRICS) {
                        val currentLineTime = if (displayedCurrentLineIndex >= 0 && displayedCurrentLineIndex < lines.size) {
                            lines[displayedCurrentLineIndex].time
                        } else -1L
                        val isLineAtSameTime = item.time == currentLineTime
                        val isActiveByIndex = index == displayedCurrentLineIndex
                        val isActiveByTime = isLineAtSameTime && displayedCurrentLineIndex >= 0

                        MetroLyricsLine(
                            entry = item,
                            nextEntryTime = lines.getOrNull(index + 1)?.time,
                            effectivePlaybackPosition = effectivePlaybackPosition,
                            lyricsOffset = lyricsOffset,
                            isSynced = isSynced,
                            isActive = isActiveByIndex || isActiveByTime,
                            distanceFromCurrent = kotlin.math.abs(index - displayedCurrentLineIndex),
                            lyricsTextPosition = lyricsTextPosition,
                            textColor = textColor,
                            showRomanized = currentSong?.romanizeLyrics == true && (
                                    romanizeJapaneseLyrics ||
                                            romanizeKoreanLyrics ||
                                            romanizeRussianLyrics ||
                                            romanizeUkrainianLyrics ||
                                            romanizeSerbianLyrics ||
                                            romanizeBulgarianLyrics ||
                                            romanizeBelarusianLyrics ||
                                            romanizeKyrgyzLyrics ||
                                            romanizeMacedonianLyrics ||
                                            romanizeChineseLyrics ||
                                            romanizeHindiLyrics ||
                                            romanizePunjabiLyrics),
                            showTranslated = hasActiveTranslations,
                            isAutoScrollActive = isAutoScrollEnabled,
                            isSelectionModeActive = isSelectionModeActive,
                            isSelected = isSelected,
                            expressiveAccent = expressiveAccent,
                            onClick = {
                                if (isSelectionModeActive) {
                                    if (isSelected) {
                                        selectedIndices.remove(index)
                                        if (selectedIndices.isEmpty()) isSelectionModeActive = false
                                    } else {
                                        if (selectedIndices.size < maxSelectionLimit) selectedIndices.add(index)
                                        else showMaxSelectionToast = true
                                    }
                                } else if (isSynced && changeLyrics && !isGuest) {
                                    val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
                                    playerConnection.seekTo((item.time - lyricsOffset).coerceAtLeast(0))
                                    scope.launch {
                                        lazyListState.scrollToItem(index = index)
                                        val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                        if (itemInfo != null) {
                                            val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                                            val center = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                                            val itemCenter = itemInfo.offset + itemInfo.size / 2
                                            val offset = itemCenter - center
                                            if (kotlin.math.abs(offset) > 10) {
                                                lazyListState.animateScrollBy(
                                                    value = offset.toFloat(),
                                                    animationSpec = tween(durationMillis = 1500)
                                                )
                                            }
                                        }
                                    }
                                    lastPreviewTime = 0L
                                }
                            },
                            onLongClick = {
                                if (!isSelectionModeActive) {
                                    isSelectionModeActive = true
                                    selectedIndices.add(index)
                                } else if (!isSelected && selectedIndices.size < maxSelectionLimit) {
                                    selectedIndices.add(index)
                                } else if (!isSelected) {
                                    showMaxSelectionToast = true
                                }
                            }
                        )
                        return@itemsIndexed
                    }
                    val itemModifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)) 
                        .combinedClickable(
                            enabled = true,
                            onClick = {
                                if (isSelectionModeActive) {
                                    
                                    if (isSelected) {
                                        selectedIndices.remove(index)
                                        if (selectedIndices.isEmpty()) {
                                            isSelectionModeActive =
                                                false 
                                        }
                                    } else {
                                        if (selectedIndices.size < maxSelectionLimit) {
                                            selectedIndices.add(index)
                                        } else {
                                            showMaxSelectionToast = true
                                        }
                                    }
                                } else if (isSynced && changeLyrics && !isGuest) {
                                    
                                    val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
                                    playerConnection.seekTo((item.time - lyricsOffset).coerceAtLeast(0))
                                    
                                    scope.launch {
                                        
                                        lazyListState.scrollToItem(index = index)

                                        
                                        val itemInfo =
                                            lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                        if (itemInfo != null) {
                                            val viewportHeight =
                                                lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                                            val center =
                                                lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                                            val itemCenter = itemInfo.offset + itemInfo.size / 2
                                            val offset = itemCenter - center

                                            if (kotlin.math.abs(offset) > 10) { 
                                                lazyListState.animateScrollBy(
                                                    value = offset.toFloat(),
                                                    animationSpec = tween(durationMillis = 1500) 
                                                )
                                            }
                                        }
                                    }
                                    lastPreviewTime = 0L
                                }
                            },
                            onLongClick = {
                                if (!isSelectionModeActive) {
                                    isSelectionModeActive = true
                                    selectedIndices.add(index)
                                } else if (!isSelected && selectedIndices.size < maxSelectionLimit) {
                                    
                                    selectedIndices.add(index)
                                } else if (!isSelected) {
                                    
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
                        .padding(horizontal = 24.dp, vertical = (8 * lyricsLineSpacing).dp)
                    
                    
                    
                    val currentLineTime = if (displayedCurrentLineIndex >= 0 && displayedCurrentLineIndex < lines.size) {
                        lines[displayedCurrentLineIndex].time
                    } else -1L
                    val isLineAtSameTime = item.time == currentLineTime
                    val isActiveByIndex = index == displayedCurrentLineIndex
                    val isActiveByTime = isLineAtSameTime && displayedCurrentLineIndex >= 0
                    
                    val alpha by animateFloatAsState(
                        targetValue = when {
                            !isSynced || (isSelectionModeActive && isSelected) -> 1f
                            isActiveByIndex || isActiveByTime -> 1f
                            else -> 0.5f
                        },
                        animationSpec = tween(durationMillis = 400)
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (isActiveByIndex || isActiveByTime) 1.05f else 1f,
                        animationSpec = tween(durationMillis = 400)
                    )

                    
                    
                    val targetBlur = if (!lyricsStandardBlur || !isAutoScrollEnabled || (isSelectionModeActive && isSelected) || isActiveByIndex || isActiveByTime) {
                        0f
                    } else {
                        val distance = kotlin.math.abs(index - (if (displayedCurrentLineIndex >= 0) displayedCurrentLineIndex else currentLineIndex))
                        when (distance) {
                            1 -> 0f
                            2 -> 0f
                            3 -> 2f
                            4 -> 4f
                            else -> 6f
                        }
                    }

                    val blurRadius by animateFloatAsState(
                        targetValue = targetBlur,
                        animationSpec = tween(durationMillis = 1000),
                        label = "standard_blur"
                    )

                    
                    val agentAlignment = when {
                        item.isBackground -> Alignment.CenterHorizontally 
                        item.agent == "v1" -> Alignment.Start 
                        item.agent == "v2" -> Alignment.End 
                        item.agent == "v1000" -> Alignment.CenterHorizontally 
                        else -> when (lyricsTextPosition) {
                            LyricsPosition.LEFT -> Alignment.Start
                            LyricsPosition.CENTER -> Alignment.CenterHorizontally
                            LyricsPosition.RIGHT -> Alignment.End
                        }
                    }
                    
                    val agentTextAlign = when {
                        item.isBackground -> TextAlign.Center
                        item.agent == "v1" -> TextAlign.Left
                        item.agent == "v2" -> TextAlign.Right
                        item.agent == "v1000" -> TextAlign.Center
                        else -> when (lyricsTextPosition) {
                            LyricsPosition.LEFT -> TextAlign.Left
                            LyricsPosition.CENTER -> TextAlign.Center
                            LyricsPosition.RIGHT -> TextAlign.Right
                        }
                    }
                    
                    
                    val bgScale = if (item.isBackground) 0.85f else 1f

                    Column(
                        modifier = itemModifier.graphicsLayer {
                            this.alpha = if (item.isBackground) alpha * 0.8f else alpha
                            this.scaleX = scale * bgScale
                            this.scaleY = scale * bgScale
                            if (blurRadius > 0f) {
                                this.renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                    blurRadius * density.density,
                                    blurRadius * density.density,
                                    android.graphics.Shader.TileMode.CLAMP
                                ).asComposeRenderEffect()
                            }
                        },
                        horizontalAlignment = agentAlignment
                    ) {
                        
                        val isActiveLine = (isActiveByIndex || isActiveByTime) && isSynced
                        val lineColor = if (isActiveLine) {
                            if (item.isBackground) expressiveAccent.copy(alpha = 0.85f) else expressiveAccent
                        } else {
                            expressiveAccent.copy(alpha = if (item.isBackground) 0.5f else 0.7f)
                        }
                        val alignment = agentTextAlign
                        
                        val romanizedTextState by item.romanizedTextFlow.collectAsState()
                        val romanizedText = romanizedTextState
                        val isRomanizedAvailable = romanizedText != null
                        
                        val mainText = if (romanizeAsMain && isRomanizedAvailable) romanizedText!! else item.text
                        val subText = if (romanizeAsMain && isRomanizedAvailable) item.text else romanizedText
                        
                        val hasWordTimings = if (romanizeAsMain && isRomanizedAvailable) false else item.words?.isNotEmpty() == true
                        
                        
                        if (hasWordTimings && lyricsAnimationStyle == LyricsAnimationStyle.NONE) {
                            val styledText = buildAnnotatedString {
                                item.words?.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && effectivePlaybackPosition >= wordStartMs && effectivePlaybackPosition <= wordEndMs
                                    val hasWordPassed = isActiveLine && effectivePlaybackPosition > wordEndMs

                                    val transitionProgress = when {
                                        !isActiveLine -> 0f
                                        hasWordPassed -> 1f
                                        isWordActive && wordDuration > 0 -> {
                                            val elapsed = effectivePlaybackPosition - wordStartMs
                                            val linear = (elapsed.toFloat() / wordDuration).coerceIn(0f, 1f)
                                            linear * linear * (3f - 2f * linear)
                                        }
                                        else -> 0f
                                    }

                                    val wordAlpha = when {
                                        !isActiveLine -> 0.7f
                                        hasWordPassed -> 1f
                                        isWordActive -> 0.5f + (0.5f * transitionProgress)
                                        else -> 0.35f
                                    }

                                    val wordColor = expressiveAccent.copy(alpha = wordAlpha)
                                    val wordWeight = when {
                                        !isActiveLine -> FontWeight.Bold
                                        hasWordPassed -> FontWeight.Bold
                                        isWordActive -> FontWeight.ExtraBold
                                        else -> FontWeight.Medium
                                    }

                                    withStyle(style = SpanStyle(color = wordColor, fontWeight = wordWeight)) {
                                        append(word.text)
                                    }
                                    if (wordIndex < (item.words.size ?: 0) - 1) append(" ")
                                }
                            }
                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing.coerceAtMost(1.3f)).sp
                            )
                        } else if (hasWordTimings && lyricsAnimationStyle == LyricsAnimationStyle.FADE) {
                            val styledText = buildAnnotatedString {
                                item.words?.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && effectivePlaybackPosition >= wordStartMs && effectivePlaybackPosition <= wordEndMs
                                    val hasWordPassed = isActiveLine && effectivePlaybackPosition > wordEndMs

                                    val fadeProgress = if (isWordActive && wordDuration > 0) {
                                        val timeElapsed = effectivePlaybackPosition - wordStartMs
                                        val linear = (timeElapsed.toFloat() / wordDuration.toFloat()).coerceIn(0f, 1f)
                                        
                                        linear * linear * (3f - 2f * linear)
                                    } else if (hasWordPassed) 1f else 0f

                                    val wordAlpha = when {
                                        !isActiveLine -> 0.55f
                                        hasWordPassed -> 1f
                                        isWordActive -> 0.4f + (0.6f * fadeProgress)
                                        else -> 0.4f
                                    }
                                    val wordColor = expressiveAccent.copy(alpha = wordAlpha)
                                    val wordWeight = when {
                                        !isActiveLine -> FontWeight.Bold
                                        hasWordPassed -> FontWeight.Bold
                                        isWordActive -> FontWeight.ExtraBold
                                        else -> FontWeight.Medium
                                    }
                                    
                                    val wordShadow = when {
                                        isWordActive && fadeProgress > 0.2f -> Shadow(
                                            color = expressiveAccent.copy(alpha = 0.35f * fadeProgress),
                                            offset = Offset.Zero,
                                            blurRadius = 10f * fadeProgress
                                        )
                                        hasWordPassed -> Shadow(
                                            color = expressiveAccent.copy(alpha = 0.15f),
                                            offset = Offset.Zero,
                                            blurRadius = 6f
                                        )
                                        else -> null
                                    }

                                    withStyle(style = SpanStyle(color = wordColor, fontWeight = wordWeight, shadow = wordShadow)) {
                                        append(word.text)
                                    }
                                    if (wordIndex < (item.words.size ?: 0) - 1) append(" ")
                                }
                            }
                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing.coerceAtMost(1.3f)).sp
                            )
                        } else if (hasWordTimings && lyricsAnimationStyle == LyricsAnimationStyle.GLOW) {
                            val styledText = buildAnnotatedString {
                                item.words?.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && effectivePlaybackPosition in wordStartMs..wordEndMs
                                    val hasWordPassed = isActiveLine && effectivePlaybackPosition > wordEndMs

                                    val fillProgress = if (isWordActive && wordDuration > 0) {
                                        val linear = ((effectivePlaybackPosition - wordStartMs).toFloat() / wordDuration).coerceIn(0f, 1f)
                                        linear * linear * (3f - 2f * linear)
                                    } else if (hasWordPassed) 1f else 0f

                                    val glowIntensity = fillProgress * fillProgress
                                    val brightness = 0.45f + (0.55f * fillProgress)

                                    val wordColor = when {
                                        !isActiveLine -> expressiveAccent.copy(alpha = 0.5f)
                                        isWordActive || hasWordPassed -> expressiveAccent.copy(alpha = brightness)
                                        else -> expressiveAccent.copy(alpha = 0.35f)
                                    }
                                    val wordWeight = when {
                                        !isActiveLine -> FontWeight.Bold
                                        isWordActive -> FontWeight.ExtraBold
                                        hasWordPassed -> FontWeight.Bold
                                        else -> FontWeight.Medium
                                    }
                                    val wordShadow = if (isWordActive && glowIntensity > 0.05f) {
                                        Shadow(color = expressiveAccent.copy(alpha = 0.5f + (0.3f * glowIntensity)), offset = Offset.Zero, blurRadius = 16f + (12f * glowIntensity))
                                    } else if (hasWordPassed) {
                                        Shadow(color = expressiveAccent.copy(alpha = 0.25f), offset = Offset.Zero, blurRadius = 8f)
                                    } else null

                                    withStyle(style = SpanStyle(color = wordColor, fontWeight = wordWeight, shadow = wordShadow)) {
                                        append(word.text)
                                    }
                                    if (wordIndex < (item.words.size ?: 0) - 1) append(" ")
                                }
                            }
                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing.coerceAtMost(1.3f)).sp
                            )
                        } else if (hasWordTimings && lyricsAnimationStyle == LyricsAnimationStyle.SLIDE) {
                            val styledText = buildAnnotatedString {
                                item.words?.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && effectivePlaybackPosition >= wordStartMs && effectivePlaybackPosition < wordEndMs
                                    val hasWordPassed = (isActiveLine && effectivePlaybackPosition >= wordEndMs) || (!isActiveLine && item.time < currentLineTime)

                                    if (isWordActive && wordDuration > 0) {
                                        val timeElapsed = effectivePlaybackPosition - wordStartMs
                                        val fillProgress = (timeElapsed.toFloat() / wordDuration.toFloat()).coerceIn(0f, 1f)
                                        val breatheValue = (timeElapsed % 3000) / 3000f
                                        val breatheEffect = (kotlin.math.sin(breatheValue * Math.PI.toFloat() * 2f) * 0.03f).coerceIn(0f, 0.03f)
                                        val glowIntensity = (0.3f + fillProgress * 0.7f + breatheEffect).coerceIn(0f, 1.1f)

                                        val slideBrush = Brush.horizontalGradient(
                                            0.0f to expressiveAccent,
                                            (fillProgress * 0.95f).coerceIn(0f, 1f) to expressiveAccent,
                                            fillProgress to expressiveAccent.copy(alpha = 0.9f),
                                            (fillProgress + 0.02f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.5f),
                                            (fillProgress + 0.08f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.35f),
                                            1.0f to expressiveAccent.copy(alpha = 0.35f)
                                        )

                                        withStyle(style = SpanStyle(
                                            brush = slideBrush,
                                            fontWeight = FontWeight.ExtraBold,
                                            shadow = Shadow(color = expressiveAccent.copy(alpha = 0.4f * glowIntensity), offset = Offset(0f, 0f), blurRadius = 14f + (4f * fillProgress))
                                        )) {
                                            append(word.text)
                                        }
                                    } else if (hasWordPassed && isActiveLine) {
                                        withStyle(style = SpanStyle(
                                            color = expressiveAccent,
                                            fontWeight = FontWeight.Bold,
                                            shadow = Shadow(color = expressiveAccent.copy(alpha = 0.4f), offset = Offset(0f, 0f), blurRadius = 12f)
                                        )) {
                                            append(word.text)
                                        }
                                    } else {
                                        val wordColor = if (!isActiveLine) lineColor else expressiveAccent.copy(alpha = 0.35f)
                                        withStyle(style = SpanStyle(color = wordColor, fontWeight = FontWeight.Medium)) {
                                            append(word.text)
                                        }
                                    }
                                    if (wordIndex < (item.words.size ?: 0) - 1) append(" ")
                                }
                            }
                            Text(text = styledText, fontSize = lyricsTextSize.sp, textAlign = alignment, lineHeight = (lyricsTextSize * lyricsLineSpacing).sp)
                        } else if (hasWordTimings && lyricsAnimationStyle == LyricsAnimationStyle.KARAOKE) {
                            val styledText = buildAnnotatedString {
                                item.words?.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && effectivePlaybackPosition >= wordStartMs && effectivePlaybackPosition < wordEndMs
                                    val hasWordPassed = (isActiveLine && effectivePlaybackPosition >= wordEndMs) || (!isActiveLine && item.time < currentLineTime)

                                    if (isWordActive && wordDuration > 0) {
                                        val timeElapsed = effectivePlaybackPosition - wordStartMs
                                        val linearProgress = (timeElapsed.toFloat() / wordDuration.toFloat()).coerceIn(0f, 1f)
                                        
                                        val fillProgress = linearProgress * linearProgress * (3f - 2f * linearProgress)
                                        
                                        
                                        val glowIntensity = fillProgress * fillProgress

                                        val wordBrush = Brush.horizontalGradient(
                                            0.0f to expressiveAccent.copy(alpha = 0.4f),
                                            (fillProgress * 0.6f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.75f),
                                            (fillProgress * 0.85f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.95f),
                                            fillProgress to expressiveAccent,
                                            (fillProgress + 0.03f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.85f),
                                            (fillProgress + 0.1f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.5f),
                                            1.0f to expressiveAccent.copy(alpha = if (fillProgress >= 0.9f) 0.95f else 0.4f)
                                        )

                                        
                                        val wordShadow = Shadow(
                                            color = expressiveAccent.copy(alpha = 0.5f + (0.3f * glowIntensity)),
                                            offset = Offset.Zero,
                                            blurRadius = 16f + (12f * glowIntensity)
                                        )

                                        withStyle(style = SpanStyle(
                                            brush = wordBrush,
                                            fontWeight = FontWeight.ExtraBold,
                                            shadow = wordShadow
                                        )) {
                                            append(word.text)
                                        }
                                    } else if (hasWordPassed && isActiveLine) {
                                        
                                        withStyle(style = SpanStyle(
                                            color = expressiveAccent,
                                            fontWeight = FontWeight.Bold,
                                            shadow = Shadow(
                                                color = expressiveAccent.copy(alpha = 0.25f),
                                                offset = Offset.Zero,
                                                blurRadius = 8f
                                            )
                                        )) {
                                            append(word.text)
                                        }
                                    } else {
                                        
                                        val wordColor = if (!isActiveLine) lineColor else expressiveAccent.copy(alpha = 0.4f)
                                        withStyle(style = SpanStyle(color = wordColor, fontWeight = FontWeight.Medium)) {
                                            append(word.text)
                                        }
                                    }
                                    if (wordIndex < (item.words.size ?: 0) - 1) append(" ")
                                }
                            }
                            Text(text = styledText, fontSize = lyricsTextSize.sp, textAlign = alignment, lineHeight = (lyricsTextSize * lyricsLineSpacing.coerceAtMost(1.3f)).sp)
                        } else if (hasWordTimings && lyricsAnimationStyle == LyricsAnimationStyle.APPLE) {
                            val styledText = buildAnnotatedString {
                                item.words?.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && effectivePlaybackPosition >= wordStartMs && effectivePlaybackPosition < wordEndMs
                                    val hasWordPassed = (isActiveLine && effectivePlaybackPosition >= wordEndMs) || (!isActiveLine && item.time < currentLineTime)

                                    val rawProgress = if (isWordActive && wordDuration > 0) {
                                        val elapsed = effectivePlaybackPosition - wordStartMs
                                        (elapsed.toFloat() / wordDuration).coerceIn(0f, 1f)
                                    } else if (hasWordPassed) 1f else 0f

                                    
                                    val smoothProgress = rawProgress * rawProgress * (3f - 2f * rawProgress)

                                    val wordAlpha = when {
                                        !isActiveLine -> 0.55f
                                        hasWordPassed -> 1f
                                        isWordActive -> 0.55f + (0.45f * smoothProgress)
                                        else -> 0.4f
                                    }
                                    val wordColor = expressiveAccent.copy(alpha = wordAlpha)
                                    val wordWeight = when {
                                        !isActiveLine -> FontWeight.SemiBold
                                        hasWordPassed -> FontWeight.Bold
                                        isWordActive -> FontWeight.ExtraBold
                                        else -> FontWeight.Normal
                                    }
                                    
                                    val glowIntensity = smoothProgress * smoothProgress
                                    val wordShadow = when {
                                        isWordActive -> Shadow(
                                            color = expressiveAccent.copy(alpha = 0.2f + (0.4f * glowIntensity)),
                                            offset = Offset.Zero,
                                            blurRadius = 10f + (12f * glowIntensity)
                                        )
                                        hasWordPassed && isActiveLine -> Shadow(
                                            color = expressiveAccent.copy(alpha = 0.2f),
                                            offset = Offset.Zero,
                                            blurRadius = 8f
                                        )
                                        else -> null
                                    }

                                    withStyle(style = SpanStyle(color = wordColor, fontWeight = wordWeight, shadow = wordShadow)) {
                                        append(word.text)
                                    }
                                    if (wordIndex < (item.words.size ?: 0) - 1) append(" ")
                                }
                            }
                            Text(text = styledText, fontSize = lyricsTextSize.sp, textAlign = alignment, lineHeight = (lyricsTextSize * lyricsLineSpacing.coerceAtMost(1.3f)).sp)
                        } else if (lyricsAnimationStyle == LyricsAnimationStyle.APPLE_V2) {
                            val nextEntryTime = lines.getOrNull(index + 1)?.time
                            val duration = remember(item.time, nextEntryTime) {
                                if (nextEntryTime != null) nextEntryTime - item.time else 4000L
                            }
                            
                            val activeDuration = (duration * 0.95).toLong().coerceAtLeast(300L)

                            val wordData = remember(item.text, item.words, activeDuration) {
                                if (item.words?.isNotEmpty() == true) {
                                    
                                    item.words!!.mapIndexed { wordIndex, word ->
                                        val wordStart = ((word.startTime * 1000).toLong() - item.time).coerceAtLeast(0L)
                                        val wordEnd = ((word.endTime * 1000).toLong() - item.time).coerceAtLeast(wordStart + 50L)
                                        Triple(word.text, wordStart, wordEnd)
                                    }
                                } else {
                                    
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
                                            Triple(word, wordStart, wordEnd)
                                        }
                                    }
                                }
                            }

                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = when (agentAlignment) {
                                    Alignment.Start -> Arrangement.Start
                                    Alignment.CenterHorizontally -> Arrangement.Center
                                    Alignment.End -> Arrangement.End
                                    else -> Arrangement.Start
                                },
                                verticalArrangement = Arrangement.spacedBy(
                                    
                                    with(LocalDensity.current) { (lyricsTextSize * (lyricsLineSpacing.coerceAtMost(1.3f) - 1f)).sp.toDp() }
                                )
                            ) {
                                wordData.forEachIndexed { wordIndex, (wordText, startRelative, endRelative) ->
                                    val lineRelTime = (effectivePlaybackPosition - item.time).coerceAtLeast(0L)
                                    val wordDuration = endRelative - startRelative

                                    Row {
                                        wordText.forEachIndexed { charIndex, char ->
                                            val charDuration = if (wordText.isNotEmpty()) wordDuration / wordText.length else 0L
                                            val charStart = startRelative + (charIndex * charDuration)
                                            val charEnd = charStart + charDuration

                                            val charProgress = when {
                                                !isActiveLine -> 0f
                                                lineRelTime >= charEnd -> 1f
                                                lineRelTime < charStart -> 0f
                                                else -> {
                                                    if (charDuration <= 0L) 1f
                                                    else (lineRelTime - charStart).toFloat() / charDuration
                                                }
                                            }

                                            Text(
                                                text = char.toString(),
                                                fontSize = lyricsTextSize.sp,
                                                color = expressiveAccent.copy(alpha = if (!isActiveLine) 1f else if (charProgress >= 1f) 1f else 0.3f + (0.7f * charProgress)),
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = (-0.5).sp
                                            )
                                        }
                                        if (wordIndex < wordData.size - 1) {
                                            Text(
                                                text = " ",
                                                fontSize = lyricsTextSize.sp,
                                                letterSpacing = (-0.5).sp
                                            )
                                        }
                                    }
                                }
                                
                                
                                if (hasActiveTranslations) {
                                    val translatedText by item.translatedTextFlow.collectAsState()
                                    translatedText?.let { translated ->
                                        Text(
                                            text = translated,
                                            fontSize = (lyricsTextSize * 0.7f).sp,
                                            color = expressiveAccent.copy(alpha = if (isActiveLine) 0.8f else 0.3f),
                                            textAlign = agentTextAlign,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                                            lineHeight = (lyricsTextSize * 0.7f * lyricsLineSpacing.coerceAtMost(1.3f)).sp
                                        )
                                    }
                                }
                            }
                        } else if (lyricsAnimationStyle == LyricsAnimationStyle.LYRICS_V2) {
                            LyricsLineV2(
                                entry = item,
                                isActive = isActiveLine,
                                isPast = !isActiveLine && item.time < currentPlaybackPosition,
                                effectivePlaybackPosition = effectivePlaybackPosition + 150L, 
                                expressiveAccent = expressiveAccent,
                                inactiveAlpha = 0.35f, 
                                baseFontSize = lyricsTextSize,
                                lineHeight = lyricsTextSize * lyricsLineSpacing.coerceAtMost(1.3f),
                                showTranslated = hasActiveTranslations,
                                agentAlignment = agentAlignment,
                                agentTextAlign = agentTextAlign
                            )
                        } else if (isActiveLine && lyricsGlowEffect) {
                            
                            val fillProgress = remember { Animatable(0f) }
                            
                            val pulseProgress = remember { Animatable(0f) }
                            
                            LaunchedEffect(index) {
                                fillProgress.snapTo(0f)
                                fillProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(
                                        durationMillis = 1200,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }
                            
                            
                            LaunchedEffect(Unit) {
                                while (true) {
                                    pulseProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(
                                            durationMillis = 3000,
                                            easing = LinearEasing
                                        )
                                    )
                                    pulseProgress.snapTo(0f)
                                }
                            }
                            
                            val fill = fillProgress.value
                            val pulse = pulseProgress.value
                            
                            
                            val pulseEffect = (kotlin.math.sin(pulse * Math.PI.toFloat()) * 0.15f).coerceIn(0f, 0.15f)
                            val glowIntensity = (fill + pulseEffect).coerceIn(0f, 1.2f)
                            
                            
                            val glowBrush = Brush.horizontalGradient(
                                0.0f to expressiveAccent.copy(alpha = 0.3f),
                                (fill * 0.7f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.9f),
                                fill to expressiveAccent,
                                (fill + 0.1f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.7f),
                                1.0f to expressiveAccent.copy(alpha = if (fill >= 1f) 1f else 0.3f)
                            )
                            
                            val styledText = buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        shadow = Shadow(
                                            color = expressiveAccent.copy(alpha = 0.8f * glowIntensity),
                                            offset = Offset(0f, 0f),
                                            blurRadius = 28f * (1f + pulseEffect)
                                        ),
                                        brush = glowBrush
                                    )
                                ) {
                                    append(mainText)
                                }
                            }
                            
                            
                            val bounceScale = if (fill < 0.3f) {
                                
                                1f + (kotlin.math.sin(fill * 3.33f * Math.PI.toFloat()) * 0.03f)
                            } else {
                                
                                1f
                            }
                            
                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing.coerceAtMost(1.3f)).sp,
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = bounceScale
                                        scaleY = bounceScale
                                    }
                            )
                        } else if (isActiveLine && !lyricsGlowEffect) {
                            
                            Text(
                                text = mainText,
                                fontSize = lyricsTextSize.sp,
                                color = expressiveAccent,
                                textAlign = alignment,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing.coerceAtMost(1.3f)).sp
                            )
                        } else {
                            
                            Text(
                                text = mainText,
                                fontSize = lyricsTextSize.sp,
                                color = lineColor,
                                textAlign = alignment,
                                fontWeight = FontWeight.Bold,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing.coerceAtMost(1.3f)).sp
                            )
                        }
                        if (currentSong?.romanizeLyrics == true
                            && (romanizeJapaneseLyrics ||
                                    romanizeKoreanLyrics ||
                                    romanizeRussianLyrics ||
                                    romanizeUkrainianLyrics ||
                                    romanizeSerbianLyrics ||
                                    romanizeBulgarianLyrics ||
                                    romanizeBelarusianLyrics ||
                                    romanizeKyrgyzLyrics ||
                                    romanizeMacedonianLyrics ||
                                    romanizeChineseLyrics ||
                                    romanizeHindiLyrics ||
                                    romanizePunjabiLyrics)) {
                            
                            subText?.let { text ->
                                Text(
                                    text = text,
                                    fontSize = 18.sp,
                                    color = expressiveAccent.copy(alpha = 0.6f),
                                    textAlign = when (lyricsTextPosition) {
                                        LyricsPosition.LEFT -> TextAlign.Left
                                        LyricsPosition.CENTER -> TextAlign.Center
                                        LyricsPosition.RIGHT -> TextAlign.Right
                                    },
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        
                        
                        if (hasActiveTranslations && 
                            lyricsAnimationStyle != LyricsAnimationStyle.LYRICS_V2 && 
                            lyricsAnimationStyle != LyricsAnimationStyle.APPLE_V2) {
                            val translatedText by item.translatedTextFlow.collectAsState()
                            translatedText?.let { translated ->
                                Text(
                                    text = translated,
                                    fontSize = (lyricsTextSize * 0.7f).sp,
                                    color = expressiveAccent.copy(alpha = 0.8f),
                                    textAlign = when (lyricsTextPosition) {
                                        LyricsPosition.LEFT -> TextAlign.Left
                                        LyricsPosition.CENTER -> TextAlign.Center
                                        LyricsPosition.RIGHT -> TextAlign.Right
                                    },
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                                    lineHeight = (lyricsTextSize * 0.7f * lyricsLineSpacing.coerceAtMost(1.3f)).sp
                                )
                            }
                        }
                    }
                }
            }
        }
        
        
    }

    Box(
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
    ) {
        AnimatedVisibility(
            visible = !isAutoScrollEnabled && isSynced && !isSelectionModeActive,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            FilledTonalButton(onClick = {
                scope.launch {
                    performSmoothPageScroll(currentLineIndex, 1500)
                }
                isAutoScrollEnabled = true
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.sync),
                    contentDescription = stringResource(R.string.auto_scroll),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.auto_scroll))
            }
        }

        AnimatedVisibility(
            visible = isSelectionModeActive,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        isSelectionModeActive = false
                        selectedIndices.clear()
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = stringResource(R.string.cancel),
                        modifier = Modifier.size(20.dp)
                    )
                }
                FilledTonalButton(
                    onClick = {
                        if (selectedIndices.isNotEmpty()) {
                            val sortedIndices = selectedIndices.sorted()
                            val selectedLyricsText = sortedIndices
                                .mapNotNull { lines.getOrNull(it)?.text }
                                .joinToString("\n")

                            if (selectedLyricsText.isNotBlank()) {
                                shareDialogData = Triple(
                                    selectedLyricsText,
                                    mediaMetadata?.title ?: "",
                                    mediaMetadata?.artists?.joinToString { it.name } ?: ""
                                )
                                showShareDialog = true
                            }
                            isSelectionModeActive = false
                            selectedIndices.clear()
                        }
                    },
                    enabled = selectedIndices.isNotEmpty()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.share),
                        contentDescription = stringResource(R.string.share_selected),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.share))
                }
            }
        }
    }

    if (showProgressDialog) {
        BasicAlertDialog(onDismissRequest = {  }) {
            Card( 
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
        val (lyricsText, songTitle, artists) = shareDialogData!! 
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
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    val songLink =
                                        "https://share.echomusic.fun/watch?v=${mediaMetadata?.id}"
                                    
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
                            painter = painterResource(id = R.drawable.share), 
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
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                
                                shareDialogData = Triple(lyricsText, songTitle, artists)
                                showColorPickerDialog = true
                                showShareDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.share), 
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

    if (showColorPickerDialog && shareDialogData != null) {
        val (lyricsText, songTitle, artists) = shareDialogData!!
        val coverUrl = mediaMetadata?.thumbnailUrl
        val paletteColors = remember { mutableStateListOf<Color>() }
        
        var previewBackgroundStyle by remember { mutableStateOf(LyricsBackgroundStyle.SOLID) }

        val previewCardWidth = configuration.containerDpSize.width * 0.90f
        val previewPadding = 20.dp * 2
        val previewBoxPadding = 28.dp * 2
        val previewAvailableWidth = previewCardWidth - previewPadding - previewBoxPadding
        val previewBoxHeight = 340.dp
        val headerFooterEstimate = (48.dp + 14.dp + 16.dp + 20.dp + 8.dp + 28.dp * 2)
        val previewAvailableHeight = previewBoxHeight - headerFooterEstimate

        val lyricsTextAlign = when (lyricsTextPosition) {
            LyricsPosition.LEFT -> TextAlign.Left
            LyricsPosition.CENTER -> TextAlign.Center
            LyricsPosition.RIGHT -> TextAlign.Right
        }

        val textStyleForMeasurement = TextStyle(
            color = previewTextColor,
            fontWeight = FontWeight.Bold,
            textAlign = lyricsTextAlign
        )
        val textMeasurer = rememberTextMeasurer()

        rememberAdjustedFontSize(
            text = lyricsText,
            maxWidth = previewAvailableWidth,
            maxHeight = previewAvailableHeight,
            density = density,
            initialFontSize = 50.sp,
            minFontSize = 22.sp,
            style = textStyleForMeasurement,
            textMeasurer = textMeasurer
        )

        LaunchedEffect(coverUrl) {
            if (coverUrl != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val loader = ImageLoader(context)
                        val req = ImageRequest.Builder(context).data(coverUrl).allowHardware(false).build()
                        val result = loader.execute(req)
                        val bmp = result.image?.toBitmap()
                        if (bmp != null) {
                            val palette = Palette.from(bmp).generate()
                            val swatches = palette.swatches.sortedByDescending { it.population }
                            val colors = swatches.map { Color(it.rgb) }
                                .filter { color ->
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
                                    hsv[1] > 0.2f
                                }
                            paletteColors.clear()
                            paletteColors.addAll(colors.take(5))
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showColorPickerDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.systemBars.asPaddingValues())
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        // Ensure the card is constrained so the inner column can scroll
                        .heightIn(max = 650.dp) 
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 28.dp)
                    ) {
                    Text(
                        text = stringResource(id = R.string.customize_colors),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = stringResource(id = R.string.player_background_style), style = MaterialTheme.typography.titleMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        LyricsBackgroundStyle.entries.forEach { style ->
                            val label = when(style) {
                                LyricsBackgroundStyle.SOLID -> stringResource(R.string.player_background_solid)
                                LyricsBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                                LyricsBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                            }
                            val selected = previewBackgroundStyle == style
                            
                            androidx.compose.material3.FilterChip(
                                selected = selected,
                                onClick = { previewBackgroundStyle = style },
                                label = { Text(label) }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        LyricsImageCard(
                            lyricText = lyricsText,
                            mediaMetadata = mediaMetadata ?: return@Box,
                            backgroundColor = previewBackgroundColor,
                            backgroundStyle = previewBackgroundStyle,
                            textColor = previewTextColor,
                                secondaryTextColor = previewSecondaryTextColor,
                                textAlign = lyricsTextAlign
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(text = stringResource(id = R.string.background_color), style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Start)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 12.dp)) {
                        (paletteColors + listOf(Color(0xFF242424), Color(0xFF121212), Color.White, Color.Black, Color(0xFFF5F5F5), Color(0xFFEC5464), Color(0xFF039BE5), Color(0xFF43A047), Color(0xFF8E24AA))).distinct().take(12).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(color)
                                    .clickable { previewBackgroundColor = color }
                                    .border(
                                        width = if (previewBackgroundColor == color) 3.dp else 1.dp,
                                        color = if (previewBackgroundColor == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(id = R.string.text_color), style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Start)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 12.dp)) {
                        (paletteColors + listOf(Color.White, Color.Black, Color(0xFF1DB954), Color(0xFFEC5464), Color(0xFF039BE5), Color(0xFFFFB300))).distinct().take(12).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(color)
                                    .clickable { previewTextColor = color }
                                    .border(
                                        width = if (previewTextColor == color) 3.dp else 1.dp,
                                        color = if (previewTextColor == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(id = R.string.secondary_text_color), style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Start)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 12.dp)) {
                        (paletteColors.map { it.copy(alpha = 0.7f) } + listOf(Color.White.copy(alpha = 0.7f), Color.Black.copy(alpha = 0.7f), Color(0xFF1DB954).copy(alpha=0.7f), Color(0xFFEC5464).copy(alpha=0.7f), Color(0xFF039BE5).copy(alpha=0.7f))).distinct().take(12).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(color)
                                    .clickable { previewSecondaryTextColor = color }
                                    .border(
                                        width = if (previewSecondaryTextColor == color) 3.dp else 1.dp,
                                        color = if (previewSecondaryTextColor == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            showColorPickerDialog = false
                            showProgressDialog = true
                            scope.launch {
                                try {
                                    val screenWidth = configuration.containerSize.width
                                    val screenHeight = configuration.containerSize.height

                                    val image = ComposeToImage.createLyricsImage(
                                        context = context,
                                        coverArtUrl = coverUrl,
                                        songTitle = songTitle,
                                        artistName = artists,
                                        lyrics = lyricsText,
                                        width = (screenWidth * density.density).toInt(),
                                        height = (screenHeight * density.density).toInt(),
                                        backgroundColor = previewBackgroundColor.toArgb(),
                                        backgroundStyle = previewBackgroundStyle,
                                        textColor = previewTextColor.toArgb(),
                                        secondaryTextColor = previewSecondaryTextColor.toArgb(),
                                        lyricsAlignment = when (lyricsTextPosition) {
                                            LyricsPosition.LEFT -> Layout.Alignment.ALIGN_NORMAL
                                            LyricsPosition.CENTER -> Layout.Alignment.ALIGN_CENTER
                                            LyricsPosition.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
                                        }
                                    )
                                    val timestamp = System.currentTimeMillis()
                                    val filename = "lyrics_$timestamp"
                                    val uri = ComposeToImage.saveBitmapAsFile(context, image, filename)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_lyrics)))
                                } catch (e: Exception) {
                                    Toast.makeText(context, context.getString(R.string.failed_to_create_image, e.message), Toast.LENGTH_SHORT).show()
                                } finally {
                                    showProgressDialog = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.share))
                    }
                    
                    Spacer(modifier = Modifier.padding(bottom = androidx.compose.foundation.layout.WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()))
                }
            }
        }
        } // closes Dialog
        } 
    }
}


private const val echomusic_AUTO_SCROLL_DURATION = 1500L 
private const val echomusic_INITIAL_SCROLL_DURATION = 1000L 
private const val echomusic_SEEK_DURATION = 800L 
private const val echomusic_FAST_SEEK_DURATION = 600L 


val LyricsPreviewTime = 2.seconds
