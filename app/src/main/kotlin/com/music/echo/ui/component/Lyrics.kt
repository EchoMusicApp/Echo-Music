package echo.music.iad1tya.ui.component

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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.LocalListenTogetherManager
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.AiProviderKey
import echo.music.iad1tya.constants.AutoTranslateKey
import echo.music.iad1tya.constants.DarkModeKey
import echo.music.iad1tya.constants.DeeplApiKey
import echo.music.iad1tya.constants.DeeplFormalityKey
import echo.music.iad1tya.constants.LyricsAnimationStyle
import echo.music.iad1tya.constants.LyricsAnimationStyleKey
import echo.music.iad1tya.constants.LyricsClickKey
import echo.music.iad1tya.constants.LyricsGlowEffectKey
import echo.music.iad1tya.constants.LyricsLineSpacingKey
import echo.music.iad1tya.constants.LyricsRomanizeAsMainKey
import echo.music.iad1tya.constants.LyricsRomanizeBelarusianKey
import echo.music.iad1tya.constants.LyricsRomanizeBulgarianKey
import echo.music.iad1tya.constants.LyricsRomanizeChineseKey
import echo.music.iad1tya.constants.LyricsRomanizeCyrillicByLineKey
import echo.music.iad1tya.constants.LyricsRomanizeHindiKey
import echo.music.iad1tya.constants.LyricsRomanizeJapaneseKey
import echo.music.iad1tya.constants.LyricsRomanizeKoreanKey
import echo.music.iad1tya.constants.LyricsRomanizeKyrgyzKey
import echo.music.iad1tya.constants.LyricsRomanizeMacedonianKey
import echo.music.iad1tya.constants.LyricsRomanizePunjabiKey
import echo.music.iad1tya.constants.LyricsRomanizeRussianKey
import echo.music.iad1tya.constants.LyricsRomanizeSerbianKey
import echo.music.iad1tya.constants.LyricsRomanizeUkrainianKey
import echo.music.iad1tya.constants.LyricsScrollKey
import echo.music.iad1tya.constants.LyricsStandardBlurKey
import echo.music.iad1tya.constants.LyricsTextPositionKey
import echo.music.iad1tya.constants.LyricsTextSizeKey
import echo.music.iad1tya.constants.OpenRouterApiKey
import echo.music.iad1tya.constants.OpenRouterBaseUrlKey
import echo.music.iad1tya.constants.OpenRouterModelKey
import echo.music.iad1tya.constants.PlayerBackgroundStyle
import echo.music.iad1tya.constants.PlayerBackgroundStyleKey
import echo.music.iad1tya.constants.TranslateLanguageKey
import echo.music.iad1tya.constants.TranslateModeKey
import echo.music.iad1tya.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import echo.music.iad1tya.lyrics.LyricsEntry
import echo.music.iad1tya.lyrics.WordTimestamp
import echo.music.iad1tya.lyrics.LyricsTranslationHelper
import echo.music.iad1tya.lyrics.LyricsUtils.findCurrentLineIndex
import echo.music.iad1tya.lyrics.LyricsUtils.isBelarusian
import echo.music.iad1tya.lyrics.LyricsUtils.isBulgarian
import echo.music.iad1tya.lyrics.LyricsUtils.isChinese
import echo.music.iad1tya.lyrics.LyricsUtils.isHindi
import echo.music.iad1tya.lyrics.LyricsUtils.isJapanese
import echo.music.iad1tya.lyrics.LyricsUtils.isKorean
import echo.music.iad1tya.lyrics.LyricsUtils.isKyrgyz
import echo.music.iad1tya.lyrics.LyricsUtils.isMacedonian
import echo.music.iad1tya.lyrics.LyricsUtils.isPunjabi
import echo.music.iad1tya.lyrics.LyricsUtils.isRussian
import echo.music.iad1tya.lyrics.LyricsUtils.isSerbian
import echo.music.iad1tya.lyrics.LyricsUtils.isUkrainian
import echo.music.iad1tya.lyrics.LyricsUtils.parseLyrics
import echo.music.iad1tya.lyrics.LyricsUtils.romanizeChinese
import echo.music.iad1tya.lyrics.LyricsUtils.romanizeCyrillic
import echo.music.iad1tya.lyrics.LyricsUtils.romanizeHindi
import echo.music.iad1tya.lyrics.LyricsUtils.romanizeJapanese
import echo.music.iad1tya.lyrics.LyricsUtils.romanizeKorean
import echo.music.iad1tya.lyrics.LyricsUtils.romanizePunjabi
import echo.music.iad1tya.ui.component.shimmer.ShimmerHost
import echo.music.iad1tya.ui.component.shimmer.TextPlaceholder
import echo.music.iad1tya.ui.screens.settings.DarkMode
import echo.music.iad1tya.ui.screens.settings.LyricsPosition
import echo.music.iad1tya.ui.utils.fadingEdge
import echo.music.iad1tya.utils.ComposeToImage
import echo.music.iad1tya.utils.rememberEnumPreference
import echo.music.iad1tya.utils.rememberPreference
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("UnusedBoxWithConstraintsScope", "StringFormatInvalid")
@Composable
fun Lyrics(
  sliderPositionProvider: () -> Long?,
  modifier: Modifier = Modifier,
  showLyrics: Boolean,
  compact: Boolean = false
) {
  val playerConnection = LocalPlayerConnection.current ?: return
  val database = LocalDatabase.current
  val menuState = LocalMenuState.current
  val density = LocalDensity.current
  val hapticFeedback = LocalHapticFeedback.current
  val context = LocalContext.current
  val configuration = LocalWindowInfo.current
  val listenTogetherManager = LocalListenTogetherManager.current
  val isGuest = listenTogetherManager?.isGuestPlaybackRestricted == true

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
  val lyricsAnimationStyle by
    rememberEnumPreference(LyricsAnimationStyleKey, LyricsAnimationStyle.echomusic_1)
  val lyricsTextSize by rememberPreference(LyricsTextSizeKey, 24f)
  val lyricsLineSpacing by rememberPreference(LyricsLineSpacingKey, 1.3f)
  val lyricsStandardBlur by rememberPreference(LyricsStandardBlurKey, false)

  val openRouterApiKey by rememberPreference(OpenRouterApiKey, "")
  val deeplApiKey by rememberPreference(DeeplApiKey, "")
  val aiProvider by rememberPreference(AiProviderKey, "OpenRouter")
  val openRouterBaseUrl by
    rememberPreference(OpenRouterBaseUrlKey, "https://openrouter.ai/api/v1/chat/completions")
  val openRouterModel by rememberPreference(OpenRouterModelKey, "openrouter/free")
  val translateLanguage by rememberPreference(TranslateLanguageKey, "en")
  val translateMode by rememberPreference(TranslateModeKey, "Literal")
  val autoTranslate by rememberPreference(AutoTranslateKey, false)
  val deeplFormality by rememberPreference(DeeplFormalityKey, "default")

  val scope = rememberCoroutineScope()

  val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
  val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
  val currentSong by playerConnection.currentSong.collectAsState(initial = null)
  val lyrics = remember(lyricsEntity) { lyricsEntity?.lyrics?.trim() }

  val playerBackground by
    rememberEnumPreference(
      key = PlayerBackgroundStyleKey,
      defaultValue = PlayerBackgroundStyle.GRADIENT
    )

  val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
  val isSystemInDarkTheme = isSystemInDarkTheme()
  val useDarkTheme =
    remember(darkTheme, isSystemInDarkTheme) {
      if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

  val currentSongRomanize = currentSong?.song?.romanizeLyrics ?: true
  val parsedLines = remember(lyrics) { lyrics?.let(::parseLyrics).orEmpty() }
  val isSynced = remember(parsedLines) { parsedLines.any { it.time > 0L } }
  val lines =
    remember(lyrics, parsedLines, scope, currentSongRomanize) {
      if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
        emptyList()
      } else if (parsedLines.isNotEmpty()) {
        val effJapanese = currentSongRomanize && romanizeJapaneseLyrics
        val effKorean = currentSongRomanize && romanizeKoreanLyrics
        val effRussian = currentSongRomanize && romanizeRussianLyrics
        val effUkrainian = currentSongRomanize && romanizeUkrainianLyrics
        val effSerbian = currentSongRomanize && romanizeSerbianLyrics
        val effBulgarian = currentSongRomanize && romanizeBulgarianLyrics
        val effBelarusian = currentSongRomanize && romanizeBelarusianLyrics
        val effKyrgyz = currentSongRomanize && romanizeKyrgyzLyrics
        val effMacedonian = currentSongRomanize && romanizeMacedonianLyrics
        val effChinese = currentSongRomanize && romanizeChineseLyrics
        val effHindi = currentSongRomanize && romanizeHindiLyrics
        val effPunjabi = currentSongRomanize && romanizePunjabiLyrics

        val isRussianLyrics = effRussian && !romanizeCyrillicByLine && isRussian(lyrics)
        val isUkrainianLyrics = effUkrainian && !romanizeCyrillicByLine && isUkrainian(lyrics)
        val isSerbianLyrics = effSerbian && !romanizeCyrillicByLine && isSerbian(lyrics)
        val isBulgarianLyrics = effBulgarian && !romanizeCyrillicByLine && isBulgarian(lyrics)
        val isBelarusianLyrics = effBelarusian && !romanizeCyrillicByLine && isBelarusian(lyrics)
        val isKyrgyzLyrics = effKyrgyz && !romanizeCyrillicByLine && isKyrgyz(lyrics)
        val isMacedonianLyrics = effMacedonian && !romanizeCyrillicByLine && isMacedonian(lyrics)

        parsedLines
          .map { entry ->
            val newEntry =
              LyricsEntry(
                entry.time,
                entry.text,
                entry.words,
                agent = entry.agent,
                isBackground = entry.isBackground
              )

            if (effJapanese && isJapanese(entry.text) && !isChinese(entry.text)) {
              scope.launch { newEntry.romanizedTextFlow.value = romanizeJapanese(entry.text) }
            }

            if (effKorean && isKorean(entry.text)) {
              scope.launch { newEntry.romanizedTextFlow.value = romanizeKorean(entry.text) }
            }

            if (
              effRussian && (if (romanizeCyrillicByLine) isRussian(entry.text) else isRussianLyrics)
            ) {
              scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text) }
            } else if (
              effUkrainian &&
                (if (romanizeCyrillicByLine) isUkrainian(entry.text) else isUkrainianLyrics)
            ) {
              scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text) }
            } else if (
              effSerbian && (if (romanizeCyrillicByLine) isSerbian(entry.text) else isSerbianLyrics)
            ) {
              scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text) }
            } else if (
              effBulgarian &&
                (if (romanizeCyrillicByLine) isBulgarian(entry.text) else isBulgarianLyrics)
            ) {
              scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text) }
            } else if (
              effBelarusian &&
                (if (romanizeCyrillicByLine) isBelarusian(entry.text) else isBelarusianLyrics)
            ) {
              scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text) }
            } else if (
              effKyrgyz && (if (romanizeCyrillicByLine) isKyrgyz(entry.text) else isKyrgyzLyrics)
            ) {
              scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text) }
            } else if (
              effMacedonian &&
                (if (romanizeCyrillicByLine) isMacedonian(entry.text) else isMacedonianLyrics)
            ) {
              scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text) }
            } else if (effChinese && isChinese(entry.text)) {
              scope.launch { newEntry.romanizedTextFlow.value = romanizeChinese(entry.text) }
            } else if (effHindi && isHindi(entry.text)) {
              scope.launch { newEntry.romanizedTextFlow.value = romanizeHindi(entry.text) }
            } else if (effPunjabi && isPunjabi(entry.text)) {
              scope.launch { newEntry.romanizedTextFlow.value = romanizePunjabi(entry.text) }
            }

            newEntry
          }
          .let { listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + it }
      } else {

        val effJapanese = currentSongRomanize && romanizeJapaneseLyrics
        val effKorean = currentSongRomanize && romanizeKoreanLyrics
        val effRussian = currentSongRomanize && romanizeRussianLyrics
        val effUkrainian = currentSongRomanize && romanizeUkrainianLyrics
        val effSerbian = currentSongRomanize && romanizeSerbianLyrics
        val effBulgarian = currentSongRomanize && romanizeBulgarianLyrics
        val effBelarusian = currentSongRomanize && romanizeBelarusianLyrics
        val effKyrgyz = currentSongRomanize && romanizeKyrgyzLyrics
        val effMacedonian = currentSongRomanize && romanizeMacedonianLyrics
        val effChinese = currentSongRomanize && romanizeChineseLyrics
        val effHindi = currentSongRomanize && romanizeHindiLyrics
        val effPunjabi = currentSongRomanize && romanizePunjabiLyrics

        val isRussianLyrics = effRussian && !romanizeCyrillicByLine && isRussian(lyrics)
        val isUkrainianLyrics = effUkrainian && !romanizeCyrillicByLine && isUkrainian(lyrics)
        val isSerbianLyrics = effSerbian && !romanizeCyrillicByLine && isSerbian(lyrics)
        val isBulgarianLyrics = effBulgarian && !romanizeCyrillicByLine && isBulgarian(lyrics)
        val isBelarusianLyrics = effBelarusian && !romanizeCyrillicByLine && isBelarusian(lyrics)
        val isKyrgyzLyrics = effKyrgyz && !romanizeCyrillicByLine && isKyrgyz(lyrics)
        val isMacedonianLyrics = effMacedonian && !romanizeCyrillicByLine && isMacedonian(lyrics)

        lyrics.lines().mapIndexed { index, line ->
          val newEntry = LyricsEntry(index * 100L, line)

          if (effJapanese && isJapanese(line) && !isChinese(line)) {
            scope.launch { newEntry.romanizedTextFlow.value = romanizeJapanese(line) }
          }

          if (effKorean && isKorean(line)) {
            scope.launch { newEntry.romanizedTextFlow.value = romanizeKorean(line) }
          }

          if (effRussian && (if (romanizeCyrillicByLine) isRussian(line) else isRussianLyrics)) {
            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(line) }
          } else if (
            effUkrainian && (if (romanizeCyrillicByLine) isUkrainian(line) else isUkrainianLyrics)
          ) {
            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(line) }
          } else if (
            effSerbian && (if (romanizeCyrillicByLine) isSerbian(line) else isSerbianLyrics)
          ) {
            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(line) }
          } else if (
            effBulgarian && (if (romanizeCyrillicByLine) isBulgarian(line) else isBulgarianLyrics)
          ) {
            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(line) }
          } else if (
            effBelarusian &&
              (if (romanizeCyrillicByLine) isBelarusian(line) else isBelarusianLyrics)
          ) {
            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(line) }
          } else if (
            effKyrgyz && (if (romanizeCyrillicByLine) isKyrgyz(line) else isKyrgyzLyrics)
          ) {
            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(line) }
          } else if (
            effMacedonian &&
              (if (romanizeCyrillicByLine) isMacedonian(line) else isMacedonianLyrics)
          ) {
            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(line) }
          } else if (effChinese && isChinese(line)) {
            scope.launch { newEntry.romanizedTextFlow.value = romanizeChinese(line) }
          } else if (effHindi && isHindi(line)) {
            scope.launch { newEntry.romanizedTextFlow.value = romanizeHindi(line) }
          } else if (effPunjabi && isPunjabi(line)) {
            scope.launch { newEntry.romanizedTextFlow.value = romanizePunjabi(line) }
          }

          newEntry
        }
      }
    }
  val translationStatus by LyricsTranslationHelper.status.collectAsState()
  val hasActiveTranslations by LyricsTranslationHelper.hasActiveTranslations.collectAsState()
  var showTranslationBanner by remember { mutableStateOf(false) }

  DisposableEffect(Unit) {
    LyricsTranslationHelper.setCompositionActive(true)
    onDispose {
      LyricsTranslationHelper.setCompositionActive(false)
      LyricsTranslationHelper.cancelTranslation()
      LyricsTranslationHelper.resetStatus()
    }
  }

  LaunchedEffect(mediaMetadata?.id) {
    LyricsTranslationHelper.cancelTranslation()
    LyricsTranslationHelper.resetStatus()
    showTranslationBanner = false
  }

  LaunchedEffect(translationStatus) {
    if (translationStatus !is LyricsTranslationHelper.TranslationStatus.Idle) {
      showTranslationBanner = true
      kotlinx.coroutines.delay(3000)
      showTranslationBanner = false
      if (
        translationStatus is LyricsTranslationHelper.TranslationStatus.Error ||
          translationStatus is LyricsTranslationHelper.TranslationStatus.Success
      ) {
        LyricsTranslationHelper.resetStatus()
      }
    } else {
      showTranslationBanner = false
    }
  }

  LaunchedEffect(lines, lyricsEntity, translateLanguage, translateMode, autoTranslate) {
    if (lines.isNotEmpty() && lyricsEntity != null) {
      LyricsTranslationHelper.loadTranslationsFromDatabase(
        lyrics = lines,
        lyricsEntity = lyricsEntity,
        targetLanguage = translateLanguage,
        mode = translateMode
      )

      kotlinx.coroutines.delay(100)

      val effectiveApiKey = if (aiProvider == "DeepL") deeplApiKey else openRouterApiKey
      if (
        autoTranslate &&
          effectiveApiKey.isNotBlank() &&
          !LyricsTranslationHelper.hasTranslations(lyricsEntity) &&
          LyricsTranslationHelper.status.value !is
            LyricsTranslationHelper.TranslationStatus.Translating &&
          !LyricsTranslationHelper.hasActiveTranslations.value
      ) {
        LyricsTranslationHelper.triggerManualTranslation()
      }
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
        Toast.makeText(context, context.getString(R.string.ai_api_key_required), Toast.LENGTH_SHORT)
          .show()
      }
    }
  }

  LaunchedEffect(Unit) {
    LyricsTranslationHelper.clearTranslationsTrigger.collect {
      lines.forEach { it.translatedTextFlow.value = null }
    }
  }

  val expressiveAccent = Color.White
  val textColor = Color.White

  var currentLineIndex by remember { mutableIntStateOf(-1) }

  var deferredCurrentLineIndex by rememberSaveable { mutableIntStateOf(0) }

  var previousLineIndex by rememberSaveable { mutableIntStateOf(0) }

  var lastPreviewTime by rememberSaveable { mutableLongStateOf(0L) }
  var isSeeking by remember { mutableStateOf(false) }

  var initialScrollDone by rememberSaveable { mutableStateOf(false) }

  var shouldScrollToFirstLine by rememberSaveable { mutableStateOf(true) }

  var isAppMinimized by rememberSaveable { mutableStateOf(false) }

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

  val isLyricsProviderShown =
    !compact &&
      lyricsEntity?.provider != null &&
      lyricsEntity?.provider != "Unknown" &&
      !isSelectionModeActive

  val lazyListState = rememberLazyListState()
  var isAutoScrollEnabled by rememberSaveable { mutableStateOf(true) }
  var lastManualScrollTime by remember { mutableStateOf(0L) }

  LaunchedEffect(
    isAutoScrollEnabled, isSynced, isSelectionModeActive, showLyrics,
    lazyListState.isScrollInProgress, lastManualScrollTime,
  ) {
    if (!isAutoScrollEnabled && isSynced && !isSelectionModeActive && showLyrics &&
      !lazyListState.isScrollInProgress) {
      delay(4000)
      lastPreviewTime = 0L
      isAutoScrollEnabled = true
    }
  }

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
        )
        .show()
      showMaxSelectionToast = false
    }
  }

  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(showLyrics) {
    val activity = context as? Activity
    if (showLyrics) {
      activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
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
      } else if (event == Lifecycle.Event.ON_START) {
        isAppMinimized = false
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  LaunchedEffect(lines) {
    isSelectionModeActive = false
    selectedIndices.clear()
    isAutoScrollEnabled = true
  }

  LaunchedEffect(lyrics, isSynced, currentSong?.song?.id) {
    if (!isSynced) {
      currentLineIndex = -1
      return@LaunchedEffect
    }
    while (isActive) {
      val sliderPosition = sliderPositionProvider()
      isSeeking = sliderPosition != null
      val pos = sliderPosition ?: playerConnection.player.currentPosition
      val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
      val newIndex = findCurrentLineIndex(lines, pos + lyricsOffset)
      if (newIndex != currentLineIndex) {
        currentLineIndex = newIndex
      }
      delay(50)
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

  suspend fun performSmoothPageScroll(targetIndex: Int, duration: Int = 800) {
    val lookUpIndex = if (isLyricsProviderShown) targetIndex + 1 else targetIndex
    val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == lookUpIndex }
    val viewportHeight =
      lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
    if (itemInfo != null) {
      val anchor =
        if (compact) lazyListState.layoutInfo.viewportStartOffset
        else lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
      val itemAnchor = if (compact) itemInfo.offset else itemInfo.offset + itemInfo.size / 2
      val offset = itemAnchor - anchor
      if (kotlin.math.abs(offset) > 4) {
        lazyListState.animateScrollBy(
          value = offset.toFloat(),
          animationSpec =
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        )
      }
    } else {
      val centerOffset = if (viewportHeight > 0) -(viewportHeight / 3) else 0
      lazyListState.scrollToItem(lookUpIndex, scrollOffset = centerOffset)
    }
  }
  LaunchedEffect(currentLineIndex, lastPreviewTime, initialScrollDone, isAutoScrollEnabled) {
    if (!isSynced) return@LaunchedEffect
    if (isAutoScrollEnabled) {
      if ((currentLineIndex == 0 && shouldScrollToFirstLine) || !initialScrollDone) {
        shouldScrollToFirstLine = false

        val initialCenterIndex = kotlin.math.max(0, currentLineIndex)
        performSmoothPageScroll(initialCenterIndex, 800)
        if (!isAppMinimized) {
          initialScrollDone = true
        }
      } else if (currentLineIndex != -1) {
        deferredCurrentLineIndex = currentLineIndex
        if (isSeeking) {

          val seekCenterIndex = kotlin.math.max(0, currentLineIndex)
          performSmoothPageScroll(seekCenterIndex, 500)
        } else if (
          (lastPreviewTime == 0L || currentLineIndex != previousLineIndex) && scrollLyrics
        ) {

          if (currentLineIndex != previousLineIndex) {

            val centerTargetIndex = currentLineIndex
            performSmoothPageScroll(centerTargetIndex, 1500)
          }
        }
      }
    }
    if (currentLineIndex > 0) {
      shouldScrollToFirstLine = true
    }
    previousLineIndex = currentLineIndex
  }

  BoxWithConstraints(
    contentAlignment = Alignment.TopCenter,
    modifier = modifier.fillMaxSize().padding(bottom = 12.dp)
  ) {
    AnimatedVisibility(
      visible =
        !compact &&
          showTranslationBanner &&
          translationStatus !is LyricsTranslationHelper.TranslationStatus.Idle,
      enter = fadeIn() + slideInVertically { -it },
      exit = fadeOut() + slideOutVertically { -it },
      modifier = Modifier.fillMaxWidth().zIndex(1f).padding(top = 56.dp)
    ) {
      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        when (val status = translationStatus) {
          is LyricsTranslationHelper.TranslationStatus.Translating -> {
            Card(
              colors =
                CardDefaults.cardColors(
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
                  text =
                    if (status.logs.isNotEmpty()) status.logs.last()
                    else stringResource(R.string.ai_translating_lyrics),
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }
            }
          }
          is LyricsTranslationHelper.TranslationStatus.Error -> {
            Card(
              colors =
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
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
              colors =
                CardDefaults.cardColors(
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
          is LyricsTranslationHelper.TranslationStatus.Idle -> {}
        }
      }
    }

    if (lyrics == LYRICS_NOT_FOUND && !compact) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
          text = stringResource(R.string.lyrics_not_found),
          fontSize = 20.sp,
          color = MaterialTheme.colorScheme.secondary,
          textAlign = TextAlign.Center,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.alpha(0.5f)
        )
      }
    } else if (!compact || isSynced) {
      LazyColumn(
        state = lazyListState,
        userScrollEnabled = !compact,
        contentPadding =
          if (compact) {
            PaddingValues(bottom = maxHeight)
          } else {
            WindowInsets.systemBars
              .only(WindowInsetsSides.Top)
              .add(WindowInsets(top = maxHeight / 3, bottom = maxHeight / 2))
              .asPaddingValues()
          },
        modifier =
          Modifier.then(
              if (compact) Modifier.fadingEdge(bottom = 64.dp)
              else Modifier.fadingEdge(vertical = 64.dp)
            )
            .nestedScroll(
              remember {
                object : NestedScrollConnection {
                  override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                  ): Offset {
                    if (source == NestedScrollSource.UserInput) {
                      isAutoScrollEnabled = false
                      lastManualScrollTime = System.currentTimeMillis()
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
                    lastManualScrollTime = System.currentTimeMillis()
                    if (!isSelectionModeActive) {
                      lastPreviewTime = System.currentTimeMillis()
                    }
                    return super.onPostFling(consumed, available)
                  }
                }
              }
            )
      ) {
        val displayedCurrentLineIndex =
          if (!isAutoScrollEnabled) {
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
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            )
          }
        }

        if (lyrics == null) {
          item {
            ShimmerHost {
              repeat(10) {
                Box(
                  contentAlignment =
                    when (lyricsTextPosition) {
                      LyricsPosition.LEFT -> Alignment.CenterStart
                      LyricsPosition.CENTER -> Alignment.Center
                      LyricsPosition.RIGHT -> Alignment.CenterEnd
                    },
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)
                ) {
                  TextPlaceholder()
                }
              }
            }
          }
        } else {
          val lyricsOffset = currentSong?.song?.lyricsOffset?.toLong() ?: 0L

          itemsIndexed(items = lines, key = { index, item -> "$index-${item.time}" }) { index, item ->
            val currentLineTime =
              if (displayedCurrentLineIndex >= 0 && displayedCurrentLineIndex < lines.size) {
                lines[displayedCurrentLineIndex].time
              } else -1L
            val isLineAtSameTime = item.time == currentLineTime
            val isActiveByIndex = index == displayedCurrentLineIndex
            val isActiveByTime = isLineAtSameTime && displayedCurrentLineIndex >= 0
            val isActiveLine = isActiveByIndex || isActiveByTime
            val linePosition =
              if (isActiveLine) 0L
              else if (index < displayedCurrentLineIndex) Long.MAX_VALUE
              else 0L
            val isSelected = selectedIndices.contains(index)

            MetroLyricsLine(
              entry = item,
              nextEntryTime = lines.getOrNull(index + 1)?.time,
              effectivePlaybackPosition = linePosition,
              lyricsOffset = lyricsOffset,
              isSynced = isSynced,
              isActive = isActiveLine,
              distanceFromCurrent = kotlin.math.abs(index - displayedCurrentLineIndex),
              lyricsTextPosition = lyricsTextPosition,
              textColor = textColor,
              showRomanized =
                currentSongRomanize &&
                  (romanizeJapaneseLyrics ||
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
              lyricsTextSize = lyricsTextSize,
              lyricsLineSpacing = lyricsLineSpacing,
              animationStyle = lyricsAnimationStyle,
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
                  val offset = currentSong?.song?.lyricsOffset ?: 0
                  playerConnection.seekTo((item.time - offset).coerceAtLeast(0))
                  scope.launch {
                    lazyListState.scrollToItem(index = index)
                    val itemInfo =
                      lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                    if (itemInfo != null) {
                      val viewportHeight =
                        lazyListState.layoutInfo.viewportEndOffset -
                          lazyListState.layoutInfo.viewportStartOffset
                      val center =
                        lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                      val itemCenter = itemInfo.offset + itemInfo.size / 2
                      val scrollOffset = itemCenter - center
                      if (kotlin.math.abs(scrollOffset) > 10) {
                        lazyListState.animateScrollBy(
                          value = scrollOffset.toFloat(),
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
          }
        }
      }
    }

    Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
      AnimatedVisibility(
        visible = !isAutoScrollEnabled && isSynced && !isSelectionModeActive,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
      ) {
        FilledTonalButton(
          onClick = {
            scope.launch { performSmoothPageScroll(currentLineIndex, 1500) }
            isAutoScrollEnabled = true
          }
        ) {
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
                val selectedLyricsText =
                  sortedIndices.mapNotNull { lines.getOrNull(it)?.text }.joinToString("\n")

                if (selectedLyricsText.isNotBlank()) {
                  shareDialogData =
                    Triple(
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
      BasicAlertDialog(onDismissRequest = {}) {
        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
          Box(modifier = Modifier.padding(32.dp)) {
            Text(
              text =
                stringResource(R.string.generating_image) +
                  "\n" +
                  stringResource(R.string.please_wait),
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
          shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
          modifier = Modifier.fillMaxWidth(0.9f)
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Text(
              text = stringResource(R.string.share_lyrics),
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            androidx.compose.material3.FilledTonalButton(
              onClick = {
                val shareIntent =
                  Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    val songLink = "https://share.echomusic.fun/watch?v=${mediaMetadata?.id}"
                    putExtra(
                      Intent.EXTRA_TEXT,
                      "\"$lyricsText\"\n\n$songTitle - $artists\n$songLink"
                    )
                  }
                context.startActivity(
                  Intent.createChooser(shareIntent, context.getString(R.string.share_lyrics))
                )
                showShareDialog = false
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(text = stringResource(R.string.share_as_text), fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            androidx.compose.material3.FilledTonalButton(
              onClick = {
                shareDialogData = Triple(lyricsText, songTitle, artists)
                showColorPickerDialog = true
                showShareDialog = false
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(text = stringResource(R.string.share_as_image), fontSize = 16.sp)
            }

            Row(
              modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 0.dp),
              horizontalArrangement = Arrangement.End,
            ) {
              androidx.compose.material3.TextButton(
                onClick = { showShareDialog = false },
                modifier = Modifier.padding(end = 4.dp)
              ) {
                Text(
                  text = stringResource(R.string.cancel),
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.error
                )
              }
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

      val lyricsTextAlign =
        when (lyricsTextPosition) {
          LyricsPosition.LEFT -> TextAlign.Left
          LyricsPosition.CENTER -> TextAlign.Center
          LyricsPosition.RIGHT -> TextAlign.Right
        }

      val textStyleForMeasurement =
        TextStyle(
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
              val loader = context.imageLoader
              val req = ImageRequest.Builder(context).data(coverUrl).allowHardware(false).build()
              val result = loader.execute(req)
              val bmp = result.image?.toBitmap()
              if (bmp != null) {
                val palette = Palette.from(bmp).generate()
                val swatches = palette.swatches.sortedByDescending { it.population }
                val colors =
                  swatches
                    .map { Color(it.rgb) }
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
        properties =
          androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
          )
      ) {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .padding(WindowInsets.systemBars.asPaddingValues())
              .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          Card(
            shape = RoundedCornerShape(28.dp),
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
              ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier =
              Modifier.fillMaxWidth()
                // Ensure the card is constrained so the inner column can scroll
                .heightIn(max = 650.dp)
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier =
                Modifier.fillMaxWidth()
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

              Text(
                text = stringResource(id = R.string.player_background_style),
                style = MaterialTheme.typography.titleMedium
              )
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
              ) {
                LyricsBackgroundStyle.entries.forEach { style ->
                  val label =
                    when (style) {
                      LyricsBackgroundStyle.SOLID ->
                        stringResource(R.string.player_background_solid)
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
                modifier =
                  Modifier.fillMaxWidth()
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

              Text(
                text = stringResource(id = R.string.background_color),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                textAlign = TextAlign.Start
              )
              Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier =
                  Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp)
              ) {
                (paletteColors +
                    listOf(
                      Color(0xFF242424),
                      Color(0xFF121212),
                      Color.White,
                      Color.Black,
                      Color(0xFFF5F5F5),
                      Color(0xFFEC5464),
                      Color(0xFF039BE5),
                      Color(0xFF43A047),
                      Color(0xFF8E24AA)
                    ))
                  .distinct()
                  .take(12)
                  .forEach { color ->
                    Box(
                      modifier =
                        Modifier.size(40.dp)
                          .clip(androidx.compose.foundation.shape.CircleShape)
                          .background(color)
                          .clickable { previewBackgroundColor = color }
                          .border(
                            width = if (previewBackgroundColor == color) 3.dp else 1.dp,
                            color =
                              if (previewBackgroundColor == color) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = androidx.compose.foundation.shape.CircleShape
                          )
                    )
                  }
              }

              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = stringResource(id = R.string.text_color),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                textAlign = TextAlign.Start
              )
              Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier =
                  Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp)
              ) {
                (paletteColors +
                    listOf(
                      Color.White,
                      Color.Black,
                      Color(0xFF1DB954),
                      Color(0xFFEC5464),
                      Color(0xFF039BE5),
                      Color(0xFFFFB300)
                    ))
                  .distinct()
                  .take(12)
                  .forEach { color ->
                    Box(
                      modifier =
                        Modifier.size(40.dp)
                          .clip(androidx.compose.foundation.shape.CircleShape)
                          .background(color)
                          .clickable { previewTextColor = color }
                          .border(
                            width = if (previewTextColor == color) 3.dp else 1.dp,
                            color =
                              if (previewTextColor == color) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = androidx.compose.foundation.shape.CircleShape
                          )
                    )
                  }
              }

              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = stringResource(id = R.string.secondary_text_color),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                textAlign = TextAlign.Start
              )
              Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier =
                  Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp)
              ) {
                (paletteColors.map { it.copy(alpha = 0.7f) } +
                    listOf(
                      Color.White.copy(alpha = 0.7f),
                      Color.Black.copy(alpha = 0.7f),
                      Color(0xFF1DB954).copy(alpha = 0.7f),
                      Color(0xFFEC5464).copy(alpha = 0.7f),
                      Color(0xFF039BE5).copy(alpha = 0.7f)
                    ))
                  .distinct()
                  .take(12)
                  .forEach { color ->
                    Box(
                      modifier =
                        Modifier.size(40.dp)
                          .clip(androidx.compose.foundation.shape.CircleShape)
                          .background(color)
                          .clickable { previewSecondaryTextColor = color }
                          .border(
                            width = if (previewSecondaryTextColor == color) 3.dp else 1.dp,
                            color =
                              if (previewSecondaryTextColor == color)
                                MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
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

                      val image =
                        ComposeToImage.createLyricsImage(
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
                          lyricsAlignment =
                            when (lyricsTextPosition) {
                              LyricsPosition.LEFT -> Layout.Alignment.ALIGN_NORMAL
                              LyricsPosition.CENTER -> Layout.Alignment.ALIGN_CENTER
                              LyricsPosition.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
                            }
                        )
                      val timestamp = System.currentTimeMillis()
                      val filename = "lyrics_$timestamp"
                      val uri = ComposeToImage.saveBitmapAsFile(context, image, filename)
                      val shareIntent =
                        Intent(Intent.ACTION_SEND).apply {
                          type = "image/png"
                          putExtra(Intent.EXTRA_STREAM, uri)
                          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                      context.startActivity(
                        Intent.createChooser(shareIntent, context.getString(R.string.share_lyrics))
                      )
                    } catch (e: Exception) {
                      Toast.makeText(
                          context,
                          context.getString(R.string.failed_to_create_image, e.message),
                          Toast.LENGTH_SHORT
                        )
                        .show()
                    } finally {
                      showProgressDialog = false
                    }
                  }
                },
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(stringResource(id = R.string.share))
              }

              Spacer(
                modifier =
                  Modifier.padding(
                    bottom =
                      androidx.compose.foundation.layout.WindowInsets.systemBars
                        .asPaddingValues()
                        .calculateBottomPadding()
                  )
              )
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
