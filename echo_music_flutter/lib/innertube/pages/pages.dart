import '../models/yt_item.dart';

class HomeChip {
  final String title;
  final BrowseEndpoint? endpoint;
  final BrowseEndpoint? deselectEndpoint;
  const HomeChip({required this.title, this.endpoint, this.deselectEndpoint});
}

class HomeSection {
  final String title;
  final String? label;
  final String? thumbnail;
  final BrowseEndpoint? endpoint;
  final List<YTItem> items;
  const HomeSection({
    required this.title,
    this.label,
    this.thumbnail,
    this.endpoint,
    required this.items,
  });

  HomeSection copyWith({List<YTItem>? items}) => HomeSection(
    title: title,
    label: label,
    thumbnail: thumbnail,
    endpoint: endpoint,
    items: items ?? this.items,
  );
}

class HomePage {
  final List<HomeChip>? chips;
  final List<HomeSection> sections;
  final String? continuation;
  const HomePage({this.chips, required this.sections, this.continuation});

  HomePage filter({bool hideExplicit = false, bool hideVideos = false}) =>
      HomePage(
        chips: chips,
        sections: sections
            .map(
              (s) => s.copyWith(
                items: s.items
                    .filterExplicit(hideExplicit)
                    .filterVideoSongs(hideVideos),
              ),
            )
            .where((s) => s.items.isNotEmpty)
            .toList(),
        continuation: continuation,
      );
}

class SearchSummary {
  final String title;
  final List<YTItem> items;
  const SearchSummary({required this.title, required this.items});
}

class SearchSummaryPage {
  final List<SearchSummary> summaries;
  const SearchSummaryPage(this.summaries);
}

class SearchResult {
  final List<YTItem> items;
  final String? continuation;
  const SearchResult({required this.items, this.continuation});
}

class AlbumPage {
  final AlbumItem album;
  final List<SongItem> songs;
  final List<AlbumItem> otherVersions;
  final List<AlbumItem> releasesForYou;
  final String? description;
  const AlbumPage({
    required this.album,
    required this.songs,
    this.otherVersions = const [],
    this.releasesForYou = const [],
    this.description,
  });
}

class ArtistSection {
  final String title;
  final List<YTItem> items;
  final BrowseEndpoint? moreEndpoint;
  const ArtistSection({
    required this.title,
    required this.items,
    this.moreEndpoint,
  });
}

class ArtistPage {
  final ArtistItem artist;
  final List<ArtistSection> sections;
  final String? description;
  final String? subscriberCountText;
  final String? monthlyListenerCount;
  const ArtistPage({
    required this.artist,
    required this.sections,
    this.description,
    this.subscriberCountText,
    this.monthlyListenerCount,
  });
}

class ArtistItemsPage {
  final String title;
  final List<YTItem> items;
  final String? continuation;
  const ArtistItemsPage({
    required this.title,
    required this.items,
    this.continuation,
  });
}

class PlaylistPage {
  final PlaylistItem playlist;
  final List<SongItem> songs;
  final String? songsContinuation;
  final String? continuation;
  final List<YTItem>? related;
  const PlaylistPage({
    required this.playlist,
    required this.songs,
    this.songsContinuation,
    this.continuation,
    this.related,
  });
}

class PlaylistContinuationPage {
  final List<SongItem> songs;
  final String? continuation;
  const PlaylistContinuationPage({required this.songs, this.continuation});
}

class NextResult {
  final String? title;
  final List<SongItem> items;
  final int? currentIndex;
  final BrowseEndpoint? lyricsEndpoint;
  final BrowseEndpoint? relatedEndpoint;
  final String? continuation;
  final WatchEndpoint endpoint;
  const NextResult({
    this.title,
    required this.items,
    this.currentIndex,
    this.lyricsEndpoint,
    this.relatedEndpoint,
    this.continuation,
    required this.endpoint,
  });
}

class MoodAndGenresItem {
  final String title;
  final int stripeColor;
  final BrowseEndpoint endpoint;
  const MoodAndGenresItem({
    required this.title,
    required this.stripeColor,
    required this.endpoint,
  });
}

class MoodAndGenres {
  final String title;
  final List<MoodAndGenresItem> items;
  const MoodAndGenres({required this.title, required this.items});
}

class ExplorePage {
  final List<AlbumItem> newReleaseAlbums;
  final List<MoodAndGenresItem> moodAndGenres;
  const ExplorePage({
    required this.newReleaseAlbums,
    required this.moodAndGenres,
  });
}

enum ChartType { trending, top, genre, newReleases }

class ChartSection {
  final String title;
  final List<YTItem> items;
  final ChartType chartType;
  const ChartSection({
    required this.title,
    required this.items,
    required this.chartType,
  });
}

class ChartsPage {
  final List<ChartSection> sections;
  final String? continuation;
  const ChartsPage({required this.sections, this.continuation});
}

class BrowseResultItem {
  final String? title;
  final List<YTItem> items;
  const BrowseResultItem({this.title, required this.items});
}

class BrowseResult {
  final String? title;
  final List<BrowseResultItem> items;
  const BrowseResult({this.title, required this.items});
}

class LibraryPage {
  final List<YTItem> items;
  final String? continuation;
  const LibraryPage({required this.items, this.continuation});
}

class HistorySection {
  final String title;
  final List<SongItem> songs;
  const HistorySection({required this.title, required this.songs});
}

class HistoryPage {
  final List<HistorySection> sections;
  const HistoryPage({required this.sections});
}

class RelatedPage {
  final List<SongItem> songs;
  final List<AlbumItem> albums;
  final List<ArtistItem> artists;
  final List<PlaylistItem> playlists;
  const RelatedPage({
    required this.songs,
    required this.albums,
    required this.artists,
    required this.playlists,
  });
}
