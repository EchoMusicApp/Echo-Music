package echo.music.iad1tya.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.YTItem
import com.music.innertube.models.filterExplicit
import com.music.innertube.models.filterVideoSongs
import com.music.innertube.utils.YouTubeUrlParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import echo.music.iad1tya.constants.DislikedSongsKey
import echo.music.iad1tya.constants.HideExplicitKey
import echo.music.iad1tya.constants.HideVideoSongsKey
import echo.music.iad1tya.db.MusicDatabase
import echo.music.iad1tya.db.entities.SearchHistory
import echo.music.iad1tya.utils.dataStore
import echo.music.iad1tya.utils.get
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OnlineSearchSuggestionViewModel
@Inject
constructor(
  @ApplicationContext val context: Context,
  database: MusicDatabase,
) : ViewModel() {
  val query = MutableStateFlow("")
  private val _viewState = MutableStateFlow(SearchSuggestionViewState())
  val viewState = _viewState.asStateFlow()

  init {
    viewModelScope.launch {
      combine(query, context.dataStore.data.map { it[DislikedSongsKey].orEmpty() }) { q, disliked ->
          q to
            disliked
              .mapNotNull { it.split("||").getOrNull(1)?.trim()?.lowercase() }
              .filter { it.isNotBlank() }
              .toSet()
        }
        .flatMapLatest { (query, dislikedTitles) ->
          val filterHistory: (List<SearchHistory>) -> List<SearchHistory> = { history ->
            if (dislikedTitles.isEmpty()) history
            else
              history.filterNot { h ->
                val qLower = h.query.trim().lowercase()
                dislikedTitles.any { title ->
                  qLower == title ||
                    (title.length >= 3 && (qLower.contains(title) || title.contains(qLower)))
                }
              }
          }
          if (query.isEmpty()) {
            database.searchHistory().map { history ->
              SearchSuggestionViewState(history = filterHistory(history))
            }
          } else {
            val parsedUrl = YouTubeUrlParser.parse(query)
            val parsedItem = if (parsedUrl != null) fetchParsedUrlItem(parsedUrl) else null

            val result =
              if (parsedUrl != null) null else YouTube.searchSuggestions(query).getOrNull()
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)

            database
              .searchHistory(query)
              .map { it.take(3) }
              .map { history ->
                val filtered = filterHistory(history)
                SearchSuggestionViewState(
                  history = filtered,
                  suggestions =
                    result
                      ?.queries
                      ?.filter { suggestionQuery -> filtered.none { it.query == suggestionQuery } }
                      .orEmpty(),
                  items =
                    listOfNotNull(parsedItem) +
                      result
                        ?.recommendedItems
                        ?.distinctBy { it.id }
                        ?.filter { it.id != parsedItem?.id }
                        ?.filterExplicit(hideExplicit)
                        ?.filterVideoSongs(hideVideoSongs)
                        .orEmpty(),
                  isFromLink = parsedUrl != null,
                )
              }
          }
        }
        .collect { _viewState.value = it }
    }
  }

  private suspend fun fetchParsedUrlItem(parsedUrl: YouTubeUrlParser.ParsedUrl): YTItem? {
    return try {
      when (parsedUrl) {
        is YouTubeUrlParser.ParsedUrl.Video -> {
          YouTube.queue(listOf(parsedUrl.id)).getOrNull()?.firstOrNull()
        }
        is YouTubeUrlParser.ParsedUrl.Artist -> {
          YouTube.artist(parsedUrl.id).getOrNull()?.artist
        }
      }
    } catch (e: Exception) {
      null
    }
  }
}

data class SearchSuggestionViewState(
  val history: List<SearchHistory> = emptyList(),
  val suggestions: List<String> = emptyList(),
  val items: List<YTItem> = emptyList(),
  val isFromLink: Boolean = false,
)
