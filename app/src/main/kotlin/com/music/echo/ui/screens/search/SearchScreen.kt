package echo.music.iad1tya.ui.screens.search

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.utils.YouTubeUrlParser
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.LocalIsPlayerExpanded
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.PauseSearchHistoryKey
import echo.music.iad1tya.constants.SearchSource
import echo.music.iad1tya.constants.SearchSourceKey
import echo.music.iad1tya.db.entities.SearchHistory
import echo.music.iad1tya.playback.queues.YouTubeQueue
import echo.music.iad1tya.utils.rememberEnumPreference
import echo.music.iad1tya.utils.rememberPreference
import echo.music.iad1tya.viewmodels.HomeViewModel
import echo.music.iad1tya.viewmodels.MoodAndGenresViewModel
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AppleMusicCategory(
  val title: String,
  val searchQuery: String,
  val startColor: Color,
  val endColor: Color,
  val imageUrl: String,
)

private val APPLE_MUSIC_CATEGORIES =
  listOf(
    AppleMusicCategory(
      title = "Apple Music Live",
      searchQuery = "Live Concert Music",
      startColor = Color(0xFF1E2124),
      endColor = Color(0xFF0F1012),
      imageUrl =
        "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Hits",
      searchQuery = "Top Hits",
      startColor = Color(0xFFF59E0B),
      endColor = Color(0xFFD97706),
      imageUrl =
        "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Alternative",
      searchQuery = "Alternative Rock",
      startColor = Color(0xFF84CC16),
      endColor = Color(0xFF65A30D),
      imageUrl =
        "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Pop",
      searchQuery = "Pop Hits",
      startColor = Color(0xFFEC4899),
      endColor = Color(0xFFDB2777),
      imageUrl =
        "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Hip-Hop",
      searchQuery = "Hip Hop",
      startColor = Color(0xFF3B82F6),
      endColor = Color(0xFF1D4ED8),
      imageUrl =
        "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Country",
      searchQuery = "Country Music",
      startColor = Color(0xFFD97706),
      endColor = Color(0xFFB45309),
      imageUrl =
        "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Dance",
      searchQuery = "Dance Electronic",
      startColor = Color(0xFF10B981),
      endColor = Color(0xFF047857),
      imageUrl =
        "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Rock",
      searchQuery = "Classic Rock",
      startColor = Color(0xFFEF4444),
      endColor = Color(0xFFB91C1C),
      imageUrl =
        "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Latin",
      searchQuery = "Latin Music",
      startColor = Color(0xFFF43F5E),
      endColor = Color(0xFFBE123C),
      imageUrl =
        "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "R&B",
      searchQuery = "R&B Soul",
      startColor = Color(0xFF8B5CF6),
      endColor = Color(0xFF6D28D9),
      imageUrl =
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Indie",
      searchQuery = "Indie Pop Folk",
      startColor = Color(0xFF06B6D4),
      endColor = Color(0xFF0E7490),
      imageUrl =
        "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Workout",
      searchQuery = "Workout Motivation Music",
      startColor = Color(0xFFF97316),
      endColor = Color(0xFFC2410C),
      imageUrl =
        "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Chill",
      searchQuery = "Chill Vibes",
      startColor = Color(0xFF6366F1),
      endColor = Color(0xFF4338CA),
      imageUrl =
        "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Party",
      searchQuery = "Party Hits",
      startColor = Color(0xFFA855F7),
      endColor = Color(0xFF7E22CE),
      imageUrl =
        "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Focus",
      searchQuery = "Deep Focus Study",
      startColor = Color(0xFF14B8A6),
      endColor = Color(0xFF0F766E),
      imageUrl =
        "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Electronic",
      searchQuery = "Electronic EDM",
      startColor = Color(0xFF0284C7),
      endColor = Color(0xFF0369A1),
      imageUrl =
        "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Classical",
      searchQuery = "Classical Essentials",
      startColor = Color(0xFFCA8A04),
      endColor = Color(0xFFA16207),
      imageUrl =
        "https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Jazz",
      searchQuery = "Jazz Classics",
      startColor = Color(0xFF4338CA),
      endColor = Color(0xFF312E81),
      imageUrl =
        "https://images.unsplash.com/photo-1511192336575-5a79af67a629?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "K-Pop",
      searchQuery = "K-Pop Hits",
      startColor = Color(0xFFD946EF),
      endColor = Color(0xFFA21CAF),
      imageUrl =
        "https://images.unsplash.com/photo-1516280440614-37939bbacd81?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Metal",
      searchQuery = "Heavy Metal",
      startColor = Color(0xFF991B1B),
      endColor = Color(0xFF7F1D1D),
      imageUrl =
        "https://images.unsplash.com/photo-1526478806334-5fd488fcaabc?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Soul",
      searchQuery = "Soul Classics",
      startColor = Color(0xFFC2410C),
      endColor = Color(0xFF9A3412),
      imageUrl =
        "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Bollywood",
      searchQuery = "Bollywood Hits",
      startColor = Color(0xFFF59E0B),
      endColor = Color(0xFFB45309),
      imageUrl =
        "https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Sleep",
      searchQuery = "Sleep Relaxing Music",
      startColor = Color(0xFF334155),
      endColor = Color(0xFF1E293B),
      imageUrl =
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=450&auto=format&fit=crop&q=80",
    ),
    AppleMusicCategory(
      title = "Acoustic",
      searchQuery = "Acoustic Chill",
      startColor = Color(0xFF78350F),
      endColor = Color(0xFF451A03),
      imageUrl =
        "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=450&auto=format&fit=crop&q=80",
    ),
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
  navController: NavController,
  pureBlack: Boolean,
  homeViewModel: HomeViewModel = hiltViewModel(),
  moodAndGenresViewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
  val database = LocalDatabase.current
  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current
  val isPlayerExpanded = LocalIsPlayerExpanded.current
  val playerConnection = LocalPlayerConnection.current
  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

  val moodAndGenresList by moodAndGenresViewModel.moodAndGenres.collectAsState()

  var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)
  var query by
    rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
  val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)

  var searchActive by rememberSaveable { mutableStateOf(false) }
  var showSearchContent by remember { mutableStateOf(false) }

  val speechRecognizerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val spokenText =
          result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spokenText.isNullOrBlank()) {
          query = TextFieldValue(spokenText)
          if (spokenText.isNotEmpty()) {
            focusManager.clearFocus()
            navController.navigate("search/${URLEncoder.encode(spokenText, "UTF-8")}")
            if (!pauseSearchHistory) {
              coroutineScope.launch(Dispatchers.IO) {
                database.query { insert(SearchHistory(query = spokenText)) }
              }
            }
          }
          searchActive = false
        }
      }
    }

  LaunchedEffect(searchActive) {
    if (searchActive) {
      delay(80)
      showSearchContent = true
    } else {
      showSearchContent = false
    }
  }

  BackHandler(enabled = searchActive) {
    searchActive = false
    query = TextFieldValue("")
    focusManager.clearFocus()
    keyboardController?.hide()
  }

  val onSearch: (String) -> Unit = remember {
    { searchQuery ->
      if (searchQuery.isNotEmpty()) {
        searchActive = false
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        when (val parsedUrl = YouTubeUrlParser.parse(searchQuery)) {
          is YouTubeUrlParser.ParsedUrl.Video -> {
            playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = parsedUrl.id)))
          }
          is YouTubeUrlParser.ParsedUrl.Artist -> {
            navController.navigate("artist/${parsedUrl.id}")
          }
          null -> {
            navController.navigate("search/${URLEncoder.encode(searchQuery, "UTF-8")}")
          }
        }

        if (!pauseSearchHistory) {
          coroutineScope.launch(Dispatchers.IO) {
            database.query { insert(SearchHistory(query = searchQuery)) }
          }
        }
      }
    }
  }

  val onCategoryClick: (AppleMusicCategory) -> Unit = { category ->
    val matchingItem =
      moodAndGenresList
        ?.flatMap { it.items }
        ?.firstOrNull { item ->
          item.title.equals(category.title, ignoreCase = true) ||
            item.title.contains(category.title, ignoreCase = true) ||
            category.title.contains(item.title, ignoreCase = true)
        }

    if (matchingItem != null) {
      navController.navigate(
        "youtube_browse/${matchingItem.endpoint.browseId}?params=${matchingItem.endpoint.params}"
      )
    } else {
      navController.navigate("search/${URLEncoder.encode(category.searchQuery, "UTF-8")}")
    }
  }

  val bottomPadding =
    LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

  Scaffold(
    topBar = {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
      ) {
        // Top Header: Title "Search"
        if (!searchActive) {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.search),
              style =
                MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 24.sp,
                ),
            )
          }
        }

        // Material You Stadium-Pill SearchBar
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp)
              .padding(top = if (searchActive) 10.dp else 0.dp, bottom = 12.dp)
              .height(56.dp)
              .clip(CircleShape)
              .background(
                if (pureBlack) Color.White.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceContainerHigh
              )
              .clickable {
                if (!searchActive) {
                  searchActive = true
                  focusRequester.requestFocus()
                }
              }
              .padding(horizontal = 12.dp),
          contentAlignment = Alignment.CenterStart,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            IconButton(
              onClick = {
                if (searchActive) {
                  searchActive = false
                  query = TextFieldValue("")
                  focusManager.clearFocus()
                  keyboardController?.hide()
                } else {
                  searchActive = true
                  focusRequester.requestFocus()
                }
              },
              modifier = Modifier.size(36.dp),
            ) {
              Icon(
                painter =
                  painterResource(if (searchActive) R.drawable.arrow_back else R.drawable.search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
              )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(modifier = Modifier.weight(1f)) {
              if (query.text.isEmpty()) {
                Text(
                  text =
                    if (searchSource == SearchSource.ONLINE) "Artists, Songs, Lyrics & More"
                    else stringResource(R.string.search_library),
                  style =
                    TextStyle(
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                      fontSize = 16.sp,
                    ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier =
                  Modifier.fillMaxWidth().focusRequester(focusRequester).onFocusChanged {
                    if (it.isFocused) searchActive = true
                  },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions =
                  KeyboardActions(
                    onSearch = {
                      focusManager.clearFocus(force = true)
                      keyboardController?.hide()
                      searchActive = false
                      onSearch(query.text)
                    }
                  ),
                textStyle =
                  TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                  ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
              )
            }

            // Right-side actions: Clear / Mic / Source Toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (query.text.isNotEmpty()) {
                IconButton(
                  onClick = { query = TextFieldValue("") },
                  modifier = Modifier.size(36.dp),
                ) {
                  Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                  )
                }
              } else {
                IconButton(
                  onClick = {
                    try {
                      val intent =
                        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                          putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                          )
                          putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search...")
                        }
                      speechRecognizerLauncher.launch(intent)
                    } catch (e: Exception) {
                      navController.navigate("recognition")
                    }
                  },
                  modifier = Modifier.size(36.dp),
                ) {
                  Icon(
                    painter = painterResource(R.drawable.mic),
                    contentDescription = "Voice Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(21.dp),
                  )
                }
              }

              IconButton(
                onClick = {
                  searchSource =
                    if (searchSource == SearchSource.ONLINE) SearchSource.LOCAL
                    else SearchSource.ONLINE
                },
                modifier = Modifier.size(36.dp),
              ) {
                Icon(
                  painter =
                    painterResource(
                      when (searchSource) {
                        SearchSource.LOCAL -> R.drawable.library_music
                        SearchSource.ONLINE -> R.drawable.globe_search
                      }
                    ),
                  contentDescription = "Search Source",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(21.dp),
                )
              }
            }
          }
        }
      }
    },
    containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.background,
  ) { paddingValues ->
    Box(modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())) {
      if (searchActive && showSearchContent) {
        when (searchSource) {
          SearchSource.LOCAL ->
            LocalSearchScreen(
              query = query.text,
              navController = navController,
              onDismiss = {
                searchActive = false
                focusManager.clearFocus()
                keyboardController?.hide()
              },
              pureBlack = pureBlack,
            )
          SearchSource.ONLINE ->
            OnlineSearchScreen(
              query = query.text,
              onQueryChange = { query = it },
              navController = navController,
              onSearch = {
                searchActive = false
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
                onSearch(it)
              },
              onDismiss = {
                searchActive = false
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
              },
              pureBlack = pureBlack,
            )
        }
      } else if (!searchActive) {
        // Apple Music Browse Categories Grid (2 Columns)
        LazyVerticalGrid(
          columns = GridCells.Fixed(2),
          contentPadding =
            PaddingValues(
              start = 16.dp,
              end = 16.dp,
              top = 4.dp,
              bottom = bottomPadding + 24.dp,
            ),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize(),
        ) {
          items(items = APPLE_MUSIC_CATEGORIES, key = { it.title }) { category ->
            AppleMusicCategoryCard(
              category = category,
              onClick = { onCategoryClick(category) },
            )
          }
        }
      }
    }
  }

  DisposableEffect(lifecycleOwner, isPlayerExpanded) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> {
          if (isPlayerExpanded) {
            keyboardController?.hide()
            focusManager.clearFocus()
          }
        }
        Lifecycle.Event.ON_PAUSE -> {
          focusManager.clearFocus()
          keyboardController?.hide()
        }
        else -> {}
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)

    if (isPlayerExpanded) {
      keyboardController?.hide()
      focusManager.clearFocus()
    }

    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }
}

@Composable
fun AppleMusicCategoryCard(
  category: AppleMusicCategory,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .height(104.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(
          Brush.horizontalGradient(colors = listOf(category.startColor, category.endColor))
        )
        .clickable(onClick = onClick)
  ) {
    // Artist / Genre photo spanning the full card with a smooth gradient fade mask
    AsyncImage(
      model = category.imageUrl,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      alignment = Alignment.CenterEnd,
      modifier = Modifier.fillMaxSize(),
    )

    // Smooth multi-stop horizontal gradient fade mask
    // Completely solid on the left (so title sits on clean vibrant background)
    // and seamlessly transitions to transparent on the right (so artist photo reveals smoothly)
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.horizontalGradient(
              colorStops =
                arrayOf(
                  0.0f to category.startColor,
                  0.25f to category.startColor,
                  0.48f to category.startColor.copy(alpha = 0.85f),
                  0.70f to category.startColor.copy(alpha = 0.40f),
                  1.0f to Color.Transparent,
                )
            )
          )
    )

    // Subtle bottom vertical scrim for crisp text readability
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors =
                listOf(
                  Color.Transparent,
                  Color.Black.copy(alpha = 0.38f),
                ),
              startY = 60f,
            )
          )
    )

    // Category title at bottom left
    Text(
      text = category.title,
      color = Color.White,
      fontSize = 17.sp,
      fontWeight = FontWeight.Bold,
      lineHeight = 21.sp,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier =
        Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 12.dp, end = 24.dp),
    )
  }
}
