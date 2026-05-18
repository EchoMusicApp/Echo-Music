




package iad1tya.echo.music.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iad1tya.echo.music.innertube.YouTube
import iad1tya.echo.music.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import iad1tya.echo.music.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import iad1tya.echo.music.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import iad1tya.echo.music.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import iad1tya.echo.music.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import iad1tya.echo.music.innertube.models.filterExplicit
import iad1tya.echo.music.innertube.models.filterVideo
import iad1tya.echo.music.innertube.pages.SearchSummary
import iad1tya.echo.music.innertube.pages.SearchSummaryPage
import iad1tya.echo.music.constants.HideExplicitKey
import iad1tya.echo.music.constants.HideVideoKey
import iad1tya.echo.music.models.ItemsPage
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.get
import iad1tya.echo.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = savedStateHandle.get<String>("query")!!
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    init {
        viewModelScope.launch {
            filter.collect { filter ->
                if (filter == null) {
                    if (summaryPage == null) {
                        loadAllSections()
                    }
                } else {
                    if (viewStateMap[filter.value] == null) {
                        YouTube
                            .search(query, filter)
                            .onSuccess { result ->
                                viewStateMap[filter.value] =
                                    ItemsPage(
                                        result.items
                                            .distinctBy { it.id }
                                            .filterExplicit(
                                                context.dataStore.get(
                                                    HideExplicitKey,
                                                    false
                                                )
                                            ).filterVideo(context.dataStore.get(HideVideoKey, false)),
                                        result.continuation,
                                    )
                            }.onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

    /**
     * Fetches top results + each category (Songs, Videos, Albums, Artists, Community Playlists)
     * in parallel using the proven individual search filter API, then assembles a synthetic
     * SearchSummaryPage with up to 3 items per section.
     */
    private fun loadAllSections() {
        viewModelScope.launch {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideo = context.dataStore.get(HideVideoKey, false)

            // Fetch all categories in parallel
            val topResultDeferred = async { YouTube.searchSummary(query) }
            val songsDeferred     = async { YouTube.search(query, FILTER_SONG) }
            val videosDeferred    = async { YouTube.search(query, FILTER_VIDEO) }
            val albumsDeferred    = async { YouTube.search(query, FILTER_ALBUM) }
            val artistsDeferred   = async { YouTube.search(query, FILTER_ARTIST) }
            val communityDeferred = async { YouTube.search(query, FILTER_COMMUNITY_PLAYLIST) }

            val summaries = mutableListOf<SearchSummary>()

            // Top result: use the summary API just for the first section
            topResultDeferred.await().getOrNull()?.summaries?.firstOrNull()?.let { topSection ->
                if (topSection.items.isNotEmpty()) {
                    summaries.add(SearchSummary(title = topSection.title, items = topSection.items.take(3)))
                }
            }

            // Songs
            songsDeferred.await().getOrNull()?.items
                ?.distinctBy { it.id }
                ?.filterExplicit(hideExplicit)
                ?.filterVideo(hideVideo)
                ?.take(3)
                ?.takeIf { it.isNotEmpty() }
                ?.let { summaries.add(SearchSummary(title = "Songs", items = it)) }

            // Videos
            videosDeferred.await().getOrNull()?.items
                ?.distinctBy { it.id }
                ?.filterExplicit(hideExplicit)
                ?.take(3)
                ?.takeIf { it.isNotEmpty() }
                ?.let { summaries.add(SearchSummary(title = "Videos", items = it)) }

            // Albums
            albumsDeferred.await().getOrNull()?.items
                ?.distinctBy { it.id }
                ?.filterExplicit(hideExplicit)
                ?.take(3)
                ?.takeIf { it.isNotEmpty() }
                ?.let { summaries.add(SearchSummary(title = "Albums", items = it)) }

            // Artists
            artistsDeferred.await().getOrNull()?.items
                ?.distinctBy { it.id }
                ?.take(3)
                ?.takeIf { it.isNotEmpty() }
                ?.let { summaries.add(SearchSummary(title = "Artists", items = it)) }

            // Community Playlists
            communityDeferred.await().getOrNull()?.items
                ?.distinctBy { it.id }
                ?.take(3)
                ?.takeIf { it.isNotEmpty() }
                ?.let { summaries.add(SearchSummary(title = "Community playlists", items = it)) }

            if (summaries.isNotEmpty()) {
                summaryPage = SearchSummaryPage(summaries = summaries)
            } else {
                // Fallback: empty page so UI shows "No results"
                summaryPage = SearchSummaryPage(summaries = emptyList())
            }
        }
    }

    fun loadMore() {
        val filter = filter.value?.value
        viewModelScope.launch {
            if (filter == null) return@launch
            val viewState = viewStateMap[filter] ?: return@launch
            val continuation = viewState.continuation
            if (continuation != null) {
                val searchResult =
                    YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
                viewStateMap[filter] = ItemsPage(
                    (viewState.items + searchResult.items).distinctBy { it.id },
                    searchResult.continuation
                )
            }
        }
    }
}
