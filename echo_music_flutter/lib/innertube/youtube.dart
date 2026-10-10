import 'dart:convert';
import 'dart:math';

import 'innertube_client.dart';
import 'json_utils.dart';
import 'models/yt_item.dart';
import 'pages/pages.dart';
import 'renderers.dart';
import 'youtube_client.dart';

/// High-level YouTube Music API — port of the Kotlin `YouTube` object.
/// Parses InnerTube responses into page models.
class YouTube {
  YouTube._();
  static final YouTube instance = YouTube._();

  final InnerTubeClient innerTube = InnerTubeClient();

  static const _webRemix = YouTubeClient.webRemix;

  YouTubeLocale get locale => innerTube.locale;
  set locale(YouTubeLocale v) => innerTube.locale = v;
  String? get visitorData => innerTube.visitorData;
  set visitorData(String? v) => innerTube.visitorData = v;
  String? get dataSyncId => innerTube.dataSyncId;
  set dataSyncId(String? v) => innerTube.dataSyncId = v;
  String? get cookie => innerTube.cookie;
  set cookie(String? v) => innerTube.cookie = v;
  bool get isLoggedIn => innerTube.isLoggedIn;

  static const filterSong = 'EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D';
  static const filterVideo = 'EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D';
  static const filterAlbum = 'EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D';
  static const filterArtist = 'EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D';
  static const filterFeaturedPlaylist = 'EgeKAQQoADgBagwQDhAKEAMQBRAJEAQ%3D';
  static const filterCommunityPlaylist = 'EgeKAQQoAEABagoQAxAEEAoQCRAF';

  static const libraryFilterRecentActivity =
      '4qmFsgIrEhdGRW11c2ljX2xpYnJhcnlfbGFuZGluZxoQZ2dNR0tnUUlCaEFCb0FZQg%3D%3D';

  static final _visitorDataRegex = RegExp(r'^Cg[t|s]');

  // ---------------------------------------------------------------------
  // Session
  // ---------------------------------------------------------------------

  Future<String> fetchVisitorData() async {
    final text = await innerTube.getSwJsData();
    final json = jsonDecode(text.substring(5));
    final list = jp(json, [0, 2]) as List;
    for (final e in list) {
      if (e is String && _visitorDataRegex.hasMatch(e)) return e;
    }
    throw StateError('visitorData not found');
  }

  Future<String> refreshVisitorData() async {
    final v = await fetchVisitorData();
    visitorData = v;
    return v;
  }

  Future<AccountInfo> accountInfo() async {
    final res = await innerTube.accountMenu(_webRemix);
    final header = jm(res, [
      'actions',
      0,
      'openPopupAction',
      'popup',
      'multiPageMenuRenderer',
      'header',
      'activeAccountHeaderRenderer',
    ]);
    if (header == null) throw StateError('No account header');
    return AccountInfo(
      name: firstRunText(jm(header, ['accountName'])) ?? '',
      email: firstRunText(jm(header, ['email'])),
      channelHandle: firstRunText(jm(header, ['channelHandle'])),
      thumbnailUrl: thumbnailUrl(jm(header, ['accountPhoto'])),
    );
  }

  // ---------------------------------------------------------------------
  // Search
  // ---------------------------------------------------------------------

  Future<SearchSuggestions> searchSuggestions(String query) async {
    final res = await innerTube.getSearchSuggestions(_webRemix, query);
    final contents = jml(res, ['contents']);
    final queries = <String>[];
    final items = <YTItem>[];
    if (contents.isNotEmpty) {
      for (final c in jml(contents[0], [
        'searchSuggestionsSectionRenderer',
        'contents',
      ])) {
        final s = runsText(jm(c, ['searchSuggestionRenderer', 'suggestion']));
        if (s != null) queries.add(s);
      }
    }
    if (contents.length > 1) {
      for (final c in jml(contents[1], [
        'searchSuggestionsSectionRenderer',
        'contents',
      ])) {
        final r = jm(c, ['musicResponsiveListItemRenderer']);
        if (r != null) {
          final item = searchSuggestionItem(ListItemRenderer(r));
          if (item != null) items.add(item);
        }
      }
    }
    return SearchSuggestions(queries: queries, recommendedItems: items);
  }

  Future<SearchSummaryPage> searchSummary(String query) async {
    final res = await innerTube.search(_webRemix, query: query);
    final contents = jml(res, [
      'contents',
      'tabbedSearchResultsRenderer',
      'tabs',
      0,
      'tabRenderer',
      'content',
      'sectionListRenderer',
      'contents',
    ]);
    final summaries = <SearchSummary>[];
    final flatItems = <YTItem>[];
    for (final c in contents) {
      final card = jm(c, ['musicCardShelfRenderer']);
      final shelf = jm(c, ['musicShelfRenderer']);
      final itemSection = jm(c, ['itemSectionRenderer']);
      if (card != null) {
        final title =
            firstRunText(
              jm(card, [
                'header',
                'musicCardShelfHeaderBasicRenderer',
                'title',
              ]),
            ) ??
            'Top result';
        final items = <YTItem>[];
        final top = searchCardShelfItem(card);
        if (top != null) items.add(top);
        for (final cc in jml(card, ['contents'])) {
          final r = jm(cc, ['musicResponsiveListItemRenderer']);
          if (r != null) {
            final it = searchSummaryItem(ListItemRenderer(r));
            if (it != null) items.add(it);
          }
        }
        final distinct = _distinctById(items);
        if (distinct.isNotEmpty) {
          summaries.add(SearchSummary(title: title, items: distinct));
        }
      } else if (shelf != null) {
        final title = firstRunText(jm(shelf, ['title'])) ?? 'Other';
        final items = <YTItem>[];
        for (final cc in jml(shelf, ['contents'])) {
          final r = jm(cc, ['musicResponsiveListItemRenderer']);
          if (r != null) {
            final it = searchSummaryItem(ListItemRenderer(r));
            if (it != null) items.add(it);
          }
        }
        final distinct = _distinctById(items);
        if (distinct.isNotEmpty) {
          summaries.add(SearchSummary(title: title, items: distinct));
        }
      } else if (itemSection != null) {
        for (final cc in jml(itemSection, ['contents'])) {
          final r = jm(cc, ['musicResponsiveListItemRenderer']);
          if (r != null) {
            final it = searchSummaryItem(ListItemRenderer(r));
            if (it != null) flatItems.add(it);
          }
        }
      }
    }
    void addGroup(String title, List<YTItem> items) {
      if (items.isNotEmpty) {
        summaries.add(SearchSummary(title: title, items: items));
      }
    }

    addGroup(
      'Songs',
      flatItems.whereType<SongItem>().where((s) => !s.isVideoSong).toList(),
    );
    addGroup(
      'Videos',
      flatItems.whereType<SongItem>().where((s) => s.isVideoSong).toList(),
    );
    addGroup('Albums', flatItems.whereType<AlbumItem>().toList());
    addGroup('Artists', flatItems.whereType<ArtistItem>().toList());
    addGroup('Playlists', flatItems.whereType<PlaylistItem>().toList());
    return SearchSummaryPage(summaries);
  }

  Future<SearchResult> search(String query, String filter) async {
    final res = await innerTube.search(_webRemix, query: query, params: filter);
    final contents = jml(res, [
      'contents',
      'tabbedSearchResultsRenderer',
      'tabs',
      0,
      'tabRenderer',
      'content',
      'sectionListRenderer',
      'contents',
    ]);
    JsonMap? shelf;
    for (final c in contents) {
      shelf = jm(c, ['musicShelfRenderer']);
      if (shelf != null) break;
    }
    final items = <YTItem>[];
    for (final c in jml(shelf, ['contents'])) {
      final r = jm(c, ['musicResponsiveListItemRenderer']);
      if (r != null) {
        final it = searchItem(ListItemRenderer(r));
        if (it != null) items.add(it);
      }
    }
    return SearchResult(
      items: items,
      continuation: continuationOf(jl(shelf, ['continuations'])),
    );
  }

  Future<SearchResult> searchContinuation(String continuation) async {
    final res = await innerTube.search(_webRemix, continuation: continuation);
    final cont = jm(res, ['continuationContents', 'musicShelfContinuation']);
    final items = <YTItem>[];
    for (final c in jml(cont, ['contents'])) {
      final r = jm(c, ['musicResponsiveListItemRenderer']);
      if (r != null) {
        final it = searchItem(ListItemRenderer(r));
        if (it != null) items.add(it);
      }
    }
    return SearchResult(
      items: items,
      continuation: items.isEmpty
          ? null
          : continuationOf(jl(cont, ['continuations'])),
    );
  }

  // ---------------------------------------------------------------------
  // Album
  // ---------------------------------------------------------------------

  Future<AlbumPage> album(String browseId, {bool withSongs = true}) async {
    final res = await innerTube.browse(
      _webRemix,
      browseId: browseId,
      setLogin: true,
    );
    final description = _findDescription(res);

    if (browseId.contains('FEmusic_library_privately_owned_release_detail')) {
      final header = jm(res, ['header', 'musicDetailHeaderRenderer']);
      final playlistId = js(header, [
        'menu',
        'menuRenderer',
        'topLevelButtons',
        0,
        'buttonRenderer',
        'navigationEndpoint',
        'watchPlaylistEndpoint',
        'playlistId',
      ]);
      final subtitle = runs(jm(header, ['subtitle'])) ?? const [];
      final albumItem = AlbumItem(
        browseId: browseId,
        playlistId: playlistId ?? '',
        title: firstRunText(jm(header, ['title'])) ?? '',
        artists: subtitle
            .where((r) => r.navigationEndpoint != null)
            .map((r) => Artist(name: r.text, id: r.browseId))
            .toList(),
        year: int.tryParse(subtitle.lastOrNull?.text ?? ''),
        thumbnail:
            thumbnailUrl(
              jm(header, [
                'thumbnail',
                'croppedSquareThumbnailRenderer',
                'thumbnail',
              ]),
            ) ??
            '',
        description: description,
      );
      final songs = <SongItem>[];
      for (final c in jml(res, [
        'contents',
        'singleColumnBrowseResultsRenderer',
        'tabs',
        0,
        'tabRenderer',
        'content',
        'sectionListRenderer',
        'contents',
        0,
        'musicShelfRenderer',
        'contents',
      ])) {
        final r = jm(c, ['musicResponsiveListItemRenderer']);
        if (r != null) {
          final s = albumSong(ListItemRenderer(r), albumItem);
          if (s != null) songs.add(s);
        }
      }
      return AlbumPage(
        album: albumItem,
        songs: songs,
        description: description,
      );
    }

    final playlistId = js(res, [
      'microformat',
      'microformatDataRenderer',
      'urlCanonical',
    ])?.split('=').last;
    final tabs = jml(res, [
      'contents',
      'twoColumnBrowseResultsRenderer',
      'tabs',
    ]);
    final header = jm(tabs.firstOrNull, [
      'tabRenderer',
      'content',
      'sectionListRenderer',
      'contents',
      0,
      'musicResponsiveHeaderRenderer',
    ]);
    if (header == null || playlistId == null) {
      throw StateError('Album header not found');
    }
    final albumItem = AlbumItem(
      browseId: browseId,
      playlistId: playlistId,
      title: firstRunText(jm(header, ['title'])) ?? '',
      artists:
          (runs(jm(header, ['straplineTextOne']))?.oddElements() ?? const [])
              .map((r) => Artist(name: r.text, id: r.browseId))
              .toList(),
      year: int.tryParse(lastRunText(jm(header, ['subtitle'])) ?? ''),
      thumbnail: rendererThumbnail(jm(header, ['thumbnail'])) ?? '',
      description: description,
    );

    final secondary = jml(res, [
      'contents',
      'twoColumnBrowseResultsRenderer',
      'secondaryContents',
      'sectionListRenderer',
      'contents',
    ]);
    List<AlbumItem> carouselMatching(bool Function(String) titleMatch) {
      for (final c in secondary) {
        final carousel = jm(c, ['musicCarouselShelfRenderer']);
        if (carousel == null) continue;
        final title = runs(
          jm(carousel, [
            'header',
            'musicCarouselShelfBasicHeaderRenderer',
            'title',
          ]),
        );
        if (title != null && title.any((r) => titleMatch(r.text))) {
          return jml(carousel, ['contents'])
              .map((cc) => jm(cc, ['musicTwoRowItemRenderer']))
              .whereType<JsonMap>()
              .map((r) => newReleaseAlbum(TwoRowItemRenderer(r)))
              .whereType<AlbumItem>()
              .toList();
        }
      }
      return const [];
    }

    List<SongItem> songs = const [];
    if (withSongs) {
      // Songs are on the secondary column of this very response; fall back to
      // the playlist browse when they are absent.
      final shelf =
          jm(secondary.firstOrNull, ['musicPlaylistShelfRenderer']) ??
          jm(secondary.firstOrNull, ['musicShelfRenderer']);
      if (shelf != null) {
        songs = _parseAlbumShelf(shelf, albumItem);
        var cont = contentsContinuation(jml(shelf, ['contents']));
        songs = await _continueAlbumSongs(songs, cont, albumItem);
      } else {
        songs = await albumSongs(playlistId, album: albumItem);
      }
    }

    return AlbumPage(
      album: albumItem,
      songs: songs,
      otherVersions: carouselMatching(
        (t) => t.toLowerCase().contains('versions'),
      ),
      releasesForYou: carouselMatching(
        (t) =>
            t.toLowerCase().contains('releases') ||
            t.toLowerCase().contains('more from'),
      ),
      description: description,
    );
  }

  List<SongItem> _parseAlbumShelf(JsonMap shelf, AlbumItem album) {
    final songs = <SongItem>[];
    for (final c in jml(shelf, ['contents'])) {
      final r = jm(c, ['musicResponsiveListItemRenderer']);
      if (r != null) {
        final s = albumSong(ListItemRenderer(r), album);
        if (s != null) songs.add(s);
      }
    }
    return songs;
  }

  Future<List<SongItem>> _continueAlbumSongs(
    List<SongItem> initial,
    String? continuation,
    AlbumItem album,
  ) async {
    final songs = [...initial];
    final seen = <String>{};
    var count = 0;
    var cont = continuation;
    while (cont != null && count < 50 && seen.add(cont)) {
      count++;
      final res = await innerTube.browse(_webRemix, continuation: cont);
      final items = jml(res, [
        'onResponseReceivedActions',
        0,
        'appendContinuationItemsAction',
        'continuationItems',
      ]);
      for (final c in items) {
        final r = jm(c, ['musicResponsiveListItemRenderer']);
        if (r != null) {
          final s = albumSong(ListItemRenderer(r), album);
          if (s != null) songs.add(s);
        }
      }
      cont =
          contentsContinuation(items) ??
          continuationOf(
            jl(res, [
              'continuationContents',
              'musicPlaylistShelfContinuation',
              'continuations',
            ]),
          );
    }
    return songs;
  }

  Future<List<SongItem>> albumSongs(
    String playlistId, {
    AlbumItem? album,
  }) async {
    final res = await innerTube.browse(_webRemix, browseId: 'VL$playlistId');
    final shelf = jm(res, [
      'contents',
      'twoColumnBrowseResultsRenderer',
      'secondaryContents',
      'sectionListRenderer',
      'contents',
      0,
      'musicPlaylistShelfRenderer',
    ]);
    final songs = <SongItem>[];
    for (final c in jml(shelf, ['contents'])) {
      final r = jm(c, ['musicResponsiveListItemRenderer']);
      if (r != null) {
        final s = albumSong(ListItemRenderer(r), album);
        if (s != null) songs.add(s);
      }
    }
    final cont = contentsContinuation(jml(shelf, ['contents']));
    if (album == null) return songs;
    return _continueAlbumSongs(songs, cont, album);
  }

  String? _findDescription(JsonMap res) {
    String? fromContents(List<JsonMap> tabs) {
      for (final tab in tabs) {
        for (final c in jml(tab, [
          'tabRenderer',
          'content',
          'sectionListRenderer',
          'contents',
        ])) {
          final d =
              runsText(
                jm(c, ['musicDescriptionShelfRenderer', 'description']),
              ) ??
              runsText(jm(c, ['musicResponsiveHeaderRenderer', 'description']));
          if (d != null) return d;
        }
      }
      return null;
    }

    return fromContents(
          jml(res, ['contents', 'twoColumnBrowseResultsRenderer', 'tabs']),
        ) ??
        fromContents(
          jml(res, ['contents', 'singleColumnBrowseResultsRenderer', 'tabs']),
        ) ??
        runsText(
          jm(res, ['header', 'musicDetailHeaderRenderer', 'description']),
        ) ??
        runsText(
          jm(res, ['header', 'musicImmersiveHeaderRenderer', 'description']),
        );
  }

  // ---------------------------------------------------------------------
  // Artist
  // ---------------------------------------------------------------------

  Future<ArtistPage> artist(String browseId) async {
    final res = await innerTube.browse(_webRemix, browseId: browseId);
    final header = jm(res, ['header']);
    final immersive = jm(header, ['musicImmersiveHeaderRenderer']);
    final visual = jm(header, ['musicVisualHeaderRenderer']);
    final plain = jm(header, ['musicHeaderRenderer']);
    final sectionContents = jml(res, [
      'contents',
      'singleColumnBrowseResultsRenderer',
      'tabs',
      0,
      'tabRenderer',
      'content',
      'sectionListRenderer',
      'contents',
    ]);
    String? description;
    for (final c in jml(res, ['contents', 'sectionListRenderer', 'contents'])) {
      description ??= runsText(
        jm(c, ['musicDescriptionShelfRenderer', 'description']),
      );
    }
    for (final c in sectionContents) {
      description ??= runsText(
        jm(c, ['musicDescriptionShelfRenderer', 'description']),
      );
    }
    description ??= runsText(jm(immersive, ['description']));

    final firstShelfItem = jm(sectionContents.firstOrNull, [
      'musicShelfRenderer',
      'contents',
      0,
      'musicResponsiveListItemRenderer',
    ]);
    final firstItem = firstShelfItem != null
        ? ListItemRenderer(firstShelfItem)
        : null;

    final artistItem = ArtistItem(
      id: browseId,
      title:
          firstRunText(jm(immersive, ['title'])) ??
          firstRunText(jm(visual, ['title'])) ??
          firstRunText(jm(plain, ['title'])) ??
          '',
      thumbnail:
          rendererThumbnail(jm(immersive, ['thumbnail'])) ??
          rendererThumbnail(jm(visual, ['foregroundThumbnail'])) ??
          rendererThumbnail(
            jm(header, ['musicDetailHeaderRenderer', 'thumbnail']),
          ),
      channelId: js(immersive, [
        'subscriptionButton',
        'subscribeButtonRenderer',
        'channelId',
      ]),
      playEndpoint: firstItem?.overlayWatchEndpoint,
      shuffleEndpoint:
          WatchEndpoint.fromJson(
            jm(immersive, [
              'playButton',
              'buttonRenderer',
              'navigationEndpoint',
              'watchEndpoint',
            ]),
          ) ??
          WatchEndpoint.fromJson(
            jm(firstItem?.navigationEndpoint, ['watchPlaylistEndpoint']),
          ),
      radioEndpoint: WatchEndpoint.fromJson(
        jm(immersive, [
          'startRadioButton',
          'buttonRenderer',
          'navigationEndpoint',
          'watchEndpoint',
        ]),
      ),
    );

    final sections = <ArtistSection>[];
    for (final c in sectionContents) {
      final shelf = jm(c, ['musicShelfRenderer']);
      final carousel =
          jm(c, ['musicCarouselShelfRenderer']) ??
          jm(c, ['musicImmersiveCarouselShelfRenderer']);
      if (shelf != null) {
        final items = <YTItem>[];
        for (final cc in jml(shelf, ['contents'])) {
          final r = jm(cc, ['musicResponsiveListItemRenderer']);
          if (r != null) {
            final s = artistSong(ListItemRenderer(r));
            if (s != null) items.add(s);
          }
        }
        if (items.isNotEmpty) {
          final titleRuns = runs(jm(shelf, ['title']));
          sections.add(
            ArtistSection(
              title: titleRuns?.firstOrNull?.text ?? '',
              items: items,
              moreEndpoint: BrowseEndpoint.fromJson(
                jm(titleRuns?.firstOrNull?.navigationEndpoint, [
                  'browseEndpoint',
                ]),
              ),
            ),
          );
        }
      } else if (carousel != null) {
        final title = firstRunText(
          jm(carousel, [
            'header',
            'musicCarouselShelfBasicHeaderRenderer',
            'title',
          ]),
        );
        if (title == null) continue;
        final items = <YTItem>[];
        for (final cc in jml(carousel, ['contents'])) {
          final two = jm(cc, ['musicTwoRowItemRenderer']);
          final list = jm(cc, ['musicResponsiveListItemRenderer']);
          YTItem? it;
          if (two != null) it = gridTwoRowItem(TwoRowItemRenderer(two));
          it ??= list != null ? artistSong(ListItemRenderer(list)) : null;
          if (it != null) items.add(it);
        }
        if (items.isNotEmpty) {
          sections.add(
            ArtistSection(
              title: title,
              items: items,
              moreEndpoint: BrowseEndpoint.fromJson(
                jm(carousel, [
                  'header',
                  'musicCarouselShelfBasicHeaderRenderer',
                  'moreContentButton',
                  'buttonRenderer',
                  'navigationEndpoint',
                  'browseEndpoint',
                ]),
              ),
            ),
          );
        }
      }
    }

    return ArtistPage(
      artist: artistItem,
      sections: sections,
      description: description,
      subscriberCountText:
          extractCountText(
            jm(immersive, [
              'subscriptionButton2',
              'subscribeButtonRenderer',
              'subscriberCountWithSubscribeText',
            ]),
          ) ??
          extractCountText(
            jm(immersive, [
              'subscriptionButton',
              'subscribeButtonRenderer',
              'longSubscriberCountText',
            ]),
          ) ??
          extractCountText(
            jm(immersive, [
              'subscriptionButton',
              'subscribeButtonRenderer',
              'shortSubscriberCountText',
            ]),
          ),
      monthlyListenerCount: extractCountText(
        jm(immersive, ['monthlyListenerCount']),
      ),
    );
  }

  Future<ArtistItemsPage> artistItems(BrowseEndpoint endpoint) async {
    final res = await innerTube.browse(
      _webRemix,
      browseId: endpoint.browseId,
      params: endpoint.params,
    );
    final section = jm(res, [
      'contents',
      'singleColumnBrowseResultsRenderer',
      'tabs',
      0,
      'tabRenderer',
      'content',
      'sectionListRenderer',
      'contents',
      0,
    ]);
    final grid = jm(section, ['gridRenderer']);
    final carousel = jm(section, ['musicCarouselShelfRenderer']);
    final playlistShelf = jm(section, ['musicPlaylistShelfRenderer']);
    final shelf = jm(section, ['musicShelfRenderer']);
    final headerTitle = firstRunText(
      jm(res, ['header', 'musicHeaderRenderer', 'title']),
    );

    if (grid != null) {
      final items = jml(grid, ['items'])
          .map((i) => jm(i, ['musicTwoRowItemRenderer']))
          .whereType<JsonMap>()
          .map((r) => gridTwoRowItem(TwoRowItemRenderer(r)))
          .whereType<YTItem>()
          .toList();
      return ArtistItemsPage(
        title:
            firstRunText(jm(grid, ['header', 'gridHeaderRenderer', 'title'])) ??
            headerTitle ??
            '',
        items: items,
        continuation: continuationOf(jl(grid, ['continuations'])),
      );
    }
    if (carousel != null) {
      final items = <YTItem>[];
      for (final cc in jml(carousel, ['contents'])) {
        final two = jm(cc, ['musicTwoRowItemRenderer']);
        final list = jm(cc, ['musicResponsiveListItemRenderer']);
        YTItem? it;
        if (two != null) it = gridTwoRowItem(TwoRowItemRenderer(two));
        it ??= list != null ? artistSong(ListItemRenderer(list)) : null;
        if (it != null) items.add(it);
      }
      return ArtistItemsPage(
        title:
            firstRunText(
              jm(carousel, [
                'header',
                'musicCarouselShelfBasicHeaderRenderer',
                'title',
              ]),
            ) ??
            '',
        items: items,
      );
    }
    final target = shelf ?? playlistShelf;
    final items = <YTItem>[];
    for (final cc in jml(target, ['contents'])) {
      final r = jm(cc, ['musicResponsiveListItemRenderer']);
      if (r != null) {
        final s = artistSong(ListItemRenderer(r));
        if (s != null) items.add(s);
      }
    }
    return ArtistItemsPage(
      title: firstRunText(jm(shelf, ['title'])) ?? headerTitle ?? '',
      items: items,
      continuation:
          continuationOf(jl(shelf, ['continuations'])) ??
          contentsContinuation(jml(target, ['contents'])),
    );
  }

  Future<ArtistItemsPage> artistItemsContinuation(String continuation) async {
    final res = await innerTube.browse(_webRemix, continuation: continuation);
    final grid = jm(res, ['continuationContents', 'gridContinuation']);
    final shelf = jm(res, [
      'continuationContents',
      'musicPlaylistShelfContinuation',
    ]);
    if (grid != null) {
      final items = jml(grid, ['items'])
          .map((i) => jm(i, ['musicTwoRowItemRenderer']))
          .whereType<JsonMap>()
          .map((r) => gridTwoRowItem(TwoRowItemRenderer(r)))
          .whereType<YTItem>()
          .toList();
      return ArtistItemsPage(
        title: '',
        items: items,
        continuation: items.isEmpty
            ? null
            : continuationOf(jl(grid, ['continuations'])),
      );
    }
    if (shelf != null) {
      final items = <YTItem>[];
      for (final cc in jml(shelf, ['contents'])) {
        final r = jm(cc, ['musicResponsiveListItemRenderer']);
        if (r != null) {
          final s = artistSong(ListItemRenderer(r));
          if (s != null) items.add(s);
        }
      }
      return ArtistItemsPage(
        title: '',
        items: items,
        continuation: items.isEmpty
            ? null
            : continuationOf(jl(shelf, ['continuations'])),
      );
    }
    final appended = jml(res, [
      'onResponseReceivedActions',
      0,
      'appendContinuationItemsAction',
      'continuationItems',
    ]);
    final items = <YTItem>[];
    for (final cc in appended) {
      final r = jm(cc, ['musicResponsiveListItemRenderer']);
      if (r != null) {
        final s = artistSong(ListItemRenderer(r));
        if (s != null) items.add(s);
      }
    }
    return ArtistItemsPage(
      title: '',
      items: items,
      continuation: items.isEmpty ? null : contentsContinuation(appended),
    );
  }

  // ---------------------------------------------------------------------
  // Playlist
  // ---------------------------------------------------------------------

  Future<PlaylistPage> playlist(String playlistId) async {
    final res = await innerTube.browse(
      _webRemix,
      browseId: 'VL$playlistId',
      setLogin: true,
    );
    final base = jm(res, [
      'contents',
      'twoColumnBrowseResultsRenderer',
      'tabs',
      0,
      'tabRenderer',
      'content',
      'sectionListRenderer',
      'contents',
      0,
    ]);
    final header =
        jm(base, ['musicResponsiveHeaderRenderer']) ??
        jm(base, [
          'musicEditablePlaylistDetailHeaderRenderer',
          'header',
          'musicResponsiveHeaderRenderer',
        ]);
    final editable =
        jm(base, ['musicEditablePlaylistDetailHeaderRenderer']) != null;
    final secondaryList = jm(res, [
      'contents',
      'twoColumnBrowseResultsRenderer',
      'secondaryContents',
      'sectionListRenderer',
    ]);
    final secondaryContents = jml(secondaryList, ['contents']);

    var related = secondaryContents.length > 1
        ? _parseRelatedItems(secondaryContents.sublist(1))
        : <YTItem>[];
    if (related.isEmpty) {
      final token = continuationOf(jl(secondaryList, ['continuations']));
      if (token != null) {
        try {
          final cres = await innerTube.browse(
            _webRemix,
            continuation: token,
            setLogin: true,
          );
          final parsed = _parseRelatedItems(
            jml(cres, [
              'continuationContents',
              'sectionListContinuation',
              'contents',
            ]),
          );
          if (parsed.isNotEmpty) related = parsed;
        } catch (_) {}
      }
    }

    final buttons = jml(header, ['buttons']);
    WatchEndpoint? shuffle;
    if (buttons.isNotEmpty) {
      shuffle = WatchEndpoint.fromJson(
        jm(buttons.last, [
          'menuRenderer',
          'items',
          0,
          'menuNavigationItemRenderer',
          'navigationEndpoint',
          'watchPlaylistEndpoint',
        ]),
      );
    }
    WatchEndpoint? radio;
    if (buttons.length > 2) {
      radio = menuWatchPlaylist(
        jml(buttons[2], ['menuRenderer', 'items']),
        'MIX',
      );
    }
    final authorRun = runs(jm(header, ['straplineTextOne']))?.firstOrNull;

    final firstContent = secondaryContents.firstOrNull;
    final shelfContents = jml(firstContent, [
      'musicPlaylistShelfRenderer',
      'contents',
    ]);
    final plainShelfContents = jml(firstContent, [
      'musicShelfRenderer',
      'contents',
    ]);
    final listContents = shelfContents.isNotEmpty
        ? shelfContents
        : plainShelfContents;
    final songs = <SongItem>[];
    for (final c in listContents) {
      final r = jm(c, ['musicResponsiveListItemRenderer']);
      if (r != null) {
        final s = playlistSong(ListItemRenderer(r));
        if (s != null) songs.add(s);
      }
    }

    return PlaylistPage(
      playlist: PlaylistItem(
        id: playlistId,
        title: firstRunText(jm(header, ['title'])) ?? '',
        author: authorRun != null
            ? Artist(name: authorRun.text, id: authorRun.browseId)
            : null,
        songCountText: firstRunText(jm(header, ['secondSubtitle'])),
        thumbnail: rendererThumbnail(jm(header, ['thumbnail'])) ?? '',
        shuffleEndpoint: shuffle,
        radioEndpoint: radio,
        isEditable: editable,
      ),
      songs: songs,
      songsContinuation:
          contentsContinuation(shelfContents) ??
          continuationOf(
            jl(firstContent, ['musicPlaylistShelfRenderer', 'continuations']),
          ) ??
          contentsContinuation(plainShelfContents) ??
          continuationOf(
            jl(firstContent, ['musicShelfRenderer', 'continuations']),
          ),
      continuation: continuationOf(jl(secondaryList, ['continuations'])),
      related: related.isEmpty ? null : related,
    );
  }

  List<YTItem> _parseRelatedItems(List<JsonMap> contents) {
    final out = <YTItem>[];
    for (final c in contents) {
      final carousel = jm(c, ['musicCarouselShelfRenderer']);
      final shelf = jm(c, ['musicShelfRenderer']);
      if (carousel != null) {
        for (final cc in jml(carousel, ['contents'])) {
          final r = jm(cc, ['musicTwoRowItemRenderer']);
          if (r != null) {
            final it = gridTwoRowItem(TwoRowItemRenderer(r));
            if (it != null) out.add(it);
          }
        }
      } else if (shelf != null) {
        for (final cc in jml(shelf, ['contents'])) {
          final r = jm(cc, ['musicResponsiveListItemRenderer']);
          if (r != null) {
            final it = searchSummaryItem(ListItemRenderer(r));
            if (it != null) out.add(it);
          }
        }
      }
    }
    return out;
  }

  Future<PlaylistContinuationPage> playlistContinuation(
    String continuation,
  ) async {
    final res = await innerTube.browse(
      _webRemix,
      continuation: continuation,
      setLogin: true,
    );
    final shelf = jml(res, [
      'continuationContents',
      'musicPlaylistShelfContinuation',
      'contents',
    ]);
    final appended = jml(res, [
      'onResponseReceivedActions',
      0,
      'appendContinuationItemsAction',
      'continuationItems',
    ]);
    final songs = <SongItem>[];
    for (final c in [...shelf, ...appended]) {
      final r = jm(c, ['musicResponsiveListItemRenderer']);
      if (r != null) {
        final s = playlistSong(ListItemRenderer(r));
        if (s != null) songs.add(s);
      }
    }
    final next = songs.isEmpty
        ? null
        : continuationOf(
                jl(res, [
                  'continuationContents',
                  'musicPlaylistShelfContinuation',
                  'continuations',
                ]),
              ) ??
              continuationOf(
                jl(res, [
                  'continuationContents',
                  'musicShelfContinuation',
                  'continuations',
                ]),
              ) ??
              contentsContinuation(appended);
    return PlaylistContinuationPage(songs: songs, continuation: next);
  }

  /// Fetch every page of a playlist.
  Future<PlaylistPage> playlistCompleted(String playlistId) async {
    final page = await playlist(playlistId);
    final songs = [...page.songs];
    var cont = page.songsContinuation;
    final seen = <String>{};
    var count = 0;
    var emptyStreak = 0;
    while (cont != null && count < 50 && seen.add(cont)) {
      count++;
      final next = await playlistContinuation(cont);
      if (next.songs.isEmpty) {
        if (++emptyStreak >= 2) break;
      } else {
        emptyStreak = 0;
        songs.addAll(next.songs);
      }
      cont = next.continuation;
    }
    return PlaylistPage(
      playlist: page.playlist,
      songs: songs,
      continuation: page.continuation,
      related: page.related,
    );
  }

  // ---------------------------------------------------------------------
  // Home / Explore / Charts / Browse
  // ---------------------------------------------------------------------

  Future<HomePage> home({
    String? continuation,
    String browseId = 'FEmusic_home',
    String? params,
  }) async {
    if (continuation != null) return _homeContinuation(continuation);
    final res = await innerTube.browse(
      _webRemix,
      browseId: browseId,
      params: params,
    );
    final sectionList = jm(res, [
      'contents',
      'singleColumnBrowseResultsRenderer',
      'tabs',
      0,
      'tabRenderer',
      'content',
      'sectionListRenderer',
    ]);
    final sections = _homeSections(jml(sectionList, ['contents']));
    final chips = jml(sectionList, ['header', 'chipCloudRenderer', 'chips'])
        .map((c) {
          final r = jm(c, ['chipCloudChipRenderer']);
          final title = firstRunText(jm(r, ['text']));
          if (title == null) return null;
          return HomeChip(
            title: title,
            endpoint: BrowseEndpoint.fromJson(
              jm(r, ['navigationEndpoint', 'browseEndpoint']),
            ),
            deselectEndpoint: BrowseEndpoint.fromJson(
              jm(r, ['onDeselectedCommand', 'browseEndpoint']),
            ),
          );
        })
        .whereType<HomeChip>()
        .toList();
    return HomePage(
      chips: chips,
      sections: sections,
      continuation: continuationOf(jl(sectionList, ['continuations'])),
    );
  }

  Future<HomePage> _homeContinuation(String continuation) async {
    final res = await innerTube.browse(_webRemix, continuation: continuation);
    final cont = jm(res, ['continuationContents', 'sectionListContinuation']);
    return HomePage(
      sections: _homeSections(jml(cont, ['contents'])),
      continuation: continuationOf(jl(cont, ['continuations'])),
    );
  }

  List<HomeSection> _homeSections(List<JsonMap> contents) {
    final sections = <HomeSection>[];
    for (final c in contents) {
      final carousel =
          jm(c, ['musicCarouselShelfRenderer']) ??
          jm(c, ['musicImmersiveCarouselShelfRenderer']);
      if (carousel == null) continue;
      final header = jm(carousel, [
        'header',
        'musicCarouselShelfBasicHeaderRenderer',
      ]);
      final title = firstRunText(jm(header, ['title']));
      if (title == null) continue;
      final items = jml(carousel, ['contents'])
          .map((cc) => jm(cc, ['musicTwoRowItemRenderer']))
          .whereType<JsonMap>()
          .map((r) => homeTwoRowItem(TwoRowItemRenderer(r)))
          .whereType<YTItem>()
          .toList();
      if (items.isEmpty) continue;
      sections.add(
        HomeSection(
          title: title,
          label: firstRunText(jm(header, ['strapline'])),
          thumbnail: rendererThumbnail(jm(header, ['thumbnail'])),
          endpoint: BrowseEndpoint.fromJson(
            jm(header, [
              'moreContentButton',
              'buttonRenderer',
              'navigationEndpoint',
              'browseEndpoint',
            ]),
          ),
          items: items,
        ),
      );
    }
    return sections;
  }

  Future<ExplorePage> explore() async {
    final res = await innerTube.browse(_webRemix, browseId: 'FEmusic_explore');
    final contents = jml(res, [
      'contents',
      'singleColumnBrowseResultsRenderer',
      'tabs',
      0,
      'tabRenderer',
      'content',
      'sectionListRenderer',
      'contents',
    ]);
    JsonMap? carouselWithMore(String browseId) {
      for (final c in contents) {
        final carousel = jm(c, ['musicCarouselShelfRenderer']);
        final id = js(carousel, [
          'header',
          'musicCarouselShelfBasicHeaderRenderer',
          'moreContentButton',
          'buttonRenderer',
          'navigationEndpoint',
          'browseEndpoint',
          'browseId',
        ]);
        if (id == browseId) return carousel;
      }
      return null;
    }

    final albums =
        jml(carouselWithMore('FEmusic_new_releases_albums'), ['contents'])
            .map((cc) => jm(cc, ['musicTwoRowItemRenderer']))
            .whereType<JsonMap>()
            .map((r) => newReleaseAlbum(TwoRowItemRenderer(r)))
            .whereType<AlbumItem>()
            .toList();
    final moods =
        jml(carouselWithMore('FEmusic_moods_and_genres'), ['contents'])
            .map((cc) => jm(cc, ['musicNavigationButtonRenderer']))
            .whereType<JsonMap>()
            .map(_moodItem)
            .whereType<MoodAndGenresItem>()
            .toList();
    return ExplorePage(newReleaseAlbums: albums, moodAndGenres: moods);
  }

  MoodAndGenresItem? _moodItem(JsonMap r) {
    final title = firstRunText(jm(r, ['buttonText']));
    final color = ji(r, ['solid', 'leftStripeColor']);
    final endpoint = BrowseEndpoint.fromJson(
      jm(r, ['clickCommand', 'browseEndpoint']),
    );
    if (title == null || color == null || endpoint == null) return null;
    return MoodAndGenresItem(
      title: title,
      stripeColor: color,
      endpoint: endpoint,
    );
  }

  Future<List<AlbumItem>> newReleaseAlbums() async {
    final res = await innerTube.browse(
      _webRemix,
      browseId: 'FEmusic_new_releases_albums',
    );
    return jml(res, [
          'contents',
          'singleColumnBrowseResultsRenderer',
          'tabs',
          0,
          'tabRenderer',
          'content',
          'sectionListRenderer',
          'contents',
          0,
          'gridRenderer',
          'items',
        ])
        .map((i) => jm(i, ['musicTwoRowItemRenderer']))
        .whereType<JsonMap>()
        .map((r) => newReleaseAlbum(TwoRowItemRenderer(r)))
        .whereType<AlbumItem>()
        .toList();
  }

  Future<List<MoodAndGenres>> moodAndGenres() async {
    final res = await innerTube.browse(
      _webRemix,
      browseId: 'FEmusic_moods_and_genres',
    );
    final out = <MoodAndGenres>[];
    for (final c in jml(res, [
      'contents',
      'singleColumnBrowseResultsRenderer',
      'tabs',
      0,
      'tabRenderer',
      'content',
      'sectionListRenderer',
      'contents',
    ])) {
      final grid = jm(c, ['gridRenderer']);
      final title = firstRunText(
        jm(grid, ['header', 'gridHeaderRenderer', 'title']),
      );
      if (grid == null || title == null) continue;
      final items = jml(grid, ['items'])
          .map((i) => jm(i, ['musicNavigationButtonRenderer']))
          .whereType<JsonMap>()
          .map(_moodItem)
          .whereType<MoodAndGenresItem>()
          .toList();
      out.add(MoodAndGenres(title: title, items: items));
    }
    return out;
  }

  Future<BrowseResult> browse(String browseId, String? params) async {
    final res = await innerTube.browse(
      _webRemix,
      browseId: browseId,
      params: params,
    );
    final items = <BrowseResultItem>[];
    for (final c in jml(res, [
      'contents',
      'singleColumnBrowseResultsRenderer',
      'tabs',
      0,
      'tabRenderer',
      'content',
      'sectionListRenderer',
      'contents',
    ])) {
      final grid = jm(c, ['gridRenderer']);
      final carousel = jm(c, ['musicCarouselShelfRenderer']);
      if (grid != null) {
        items.add(
          BrowseResultItem(
            title: firstRunText(
              jm(grid, ['header', 'gridHeaderRenderer', 'title']),
            ),
            items: jml(grid, ['items'])
                .map((i) => jm(i, ['musicTwoRowItemRenderer']))
                .whereType<JsonMap>()
                .map((r) => gridTwoRowItem(TwoRowItemRenderer(r)))
                .whereType<YTItem>()
                .toList(),
          ),
        );
      } else if (carousel != null) {
        items.add(
          BrowseResultItem(
            title: firstRunText(
              jm(carousel, [
                'header',
                'musicCarouselShelfBasicHeaderRenderer',
                'title',
              ]),
            ),
            items: jml(carousel, ['contents'])
                .map((i) => jm(i, ['musicTwoRowItemRenderer']))
                .whereType<JsonMap>()
                .map((r) => gridTwoRowItem(TwoRowItemRenderer(r)))
                .whereType<YTItem>()
                .toList(),
          ),
        );
      }
    }
    return BrowseResult(
      title: firstRunText(jm(res, ['header', 'musicHeaderRenderer', 'title'])),
      items: items.where((i) => i.items.isNotEmpty).toList(),
    );
  }

  Future<ChartsPage> charts({String? continuation}) async {
    final res = await innerTube.browse(
      _webRemix,
      browseId: 'FEmusic_charts',
      params: 'ggMGCgQIgAQ%3D',
      continuation: continuation,
    );
    final sections = <ChartSection>[];
    for (final c in jml(res, [
      'contents',
      'singleColumnBrowseResultsRenderer',
      'tabs',
      0,
      'tabRenderer',
      'content',
      'sectionListRenderer',
      'contents',
    ])) {
      final carousel = jm(c, ['musicCarouselShelfRenderer']);
      final grid = jm(c, ['gridRenderer']);
      if (carousel != null) {
        final title = firstRunText(
          jm(carousel, [
            'header',
            'musicCarouselShelfBasicHeaderRenderer',
            'title',
          ]),
        );
        if (title == null) continue;
        final items = <YTItem>[];
        for (final cc in jml(carousel, ['contents'])) {
          final list = jm(cc, ['musicResponsiveListItemRenderer']);
          final two = jm(cc, ['musicTwoRowItemRenderer']);
          YTItem? it;
          if (list != null) {
            final lr = ListItemRenderer(list);
            it = lr.isArtist || lr.isAlbum || lr.isPlaylist
                ? searchItem(lr)
                : chartListItem(lr);
            it ??= libraryListItem(lr);
          }
          if (it == null && two != null) {
            it = gridTwoRowItem(TwoRowItemRenderer(two));
          }
          if (it != null) items.add(it);
        }
        if (items.isNotEmpty) {
          final lower = title.toLowerCase();
          sections.add(
            ChartSection(
              title: title,
              items: items,
              chartType: lower.contains('trending')
                  ? ChartType.trending
                  : lower.contains('top')
                  ? ChartType.top
                  : ChartType.genre,
            ),
          );
        }
      } else if (grid != null) {
        final title = firstRunText(
          jm(grid, ['header', 'gridHeaderRenderer', 'title']),
        );
        if (title == null) continue;
        final items = jml(grid, ['items'])
            .map((i) => jm(i, ['musicTwoRowItemRenderer']))
            .whereType<JsonMap>()
            .map((r) => gridTwoRowItem(TwoRowItemRenderer(r)))
            .whereType<YTItem>()
            .toList();
        if (items.isNotEmpty) {
          sections.add(
            ChartSection(
              title: title,
              items: items,
              chartType: ChartType.newReleases,
            ),
          );
        }
      }
    }
    return ChartsPage(
      sections: sections,
      continuation: continuationOf(
        jl(res, [
          'continuationContents',
          'sectionListContinuation',
          'continuations',
        ]),
      ),
    );
  }

  // ---------------------------------------------------------------------
  // Library (logged in)
  // ---------------------------------------------------------------------

  Future<LibraryPage> library(String browseId, {int tabIndex = 0}) async {
    final res = await innerTube.browse(
      _webRemix,
      browseId: browseId,
      setLogin: true,
    );
    final tabs = jml(res, [
      'contents',
      'singleColumnBrowseResultsRenderer',
      'tabs',
    ]);
    final contents = tabs.length > tabIndex
        ? jm(tabs[tabIndex], [
            'tabRenderer',
            'content',
            'sectionListRenderer',
            'contents',
            0,
          ])
        : null;
    final grid = jm(contents, ['gridRenderer']);
    if (grid != null) {
      return LibraryPage(
        items: jml(grid, ['items'])
            .map((i) => jm(i, ['musicTwoRowItemRenderer']))
            .whereType<JsonMap>()
            .map((r) => libraryTwoRowItem(TwoRowItemRenderer(r)))
            .whereType<YTItem>()
            .toList(),
        continuation: continuationOf(jl(grid, ['continuations'])),
      );
    }
    final shelf = jm(contents, ['musicShelfRenderer']);
    if (shelf == null) return const LibraryPage(items: []);
    return LibraryPage(
      items: jml(shelf, ['contents'])
          .map((c) => jm(c, ['musicResponsiveListItemRenderer']))
          .whereType<JsonMap>()
          .map((r) => libraryListItem(ListItemRenderer(r)))
          .whereType<YTItem>()
          .toList(),
      continuation: continuationOf(jl(shelf, ['continuations'])),
    );
  }

  Future<LibraryPage> libraryContinuation(String continuation) async {
    final res = await innerTube.browse(
      _webRemix,
      continuation: continuation,
      setLogin: true,
    );
    final grid = jm(res, ['continuationContents', 'gridContinuation']);
    if (grid != null) {
      return LibraryPage(
        items: jml(grid, ['items'])
            .map((i) => jm(i, ['musicTwoRowItemRenderer']))
            .whereType<JsonMap>()
            .map((r) => libraryTwoRowItem(TwoRowItemRenderer(r)))
            .whereType<YTItem>()
            .toList(),
        continuation: continuationOf(jl(grid, ['continuations'])),
      );
    }
    final shelf = jm(res, ['continuationContents', 'musicShelfContinuation']);
    return LibraryPage(
      items: jml(shelf, ['contents'])
          .map((c) => jm(c, ['musicResponsiveListItemRenderer']))
          .whereType<JsonMap>()
          .map((r) => libraryListItem(ListItemRenderer(r)))
          .whereType<YTItem>()
          .toList(),
      continuation: continuationOf(jl(shelf, ['continuations'])),
    );
  }

  Future<LibraryPage> libraryCompleted(
    String browseId, {
    int tabIndex = 0,
  }) async {
    final page = await library(browseId, tabIndex: tabIndex);
    final items = [...page.items];
    var cont = page.continuation;
    final seen = <String>{};
    var count = 0;
    while (cont != null && count < 50 && seen.add(cont)) {
      count++;
      final next = await libraryContinuation(cont);
      if (next.items.isEmpty) break;
      items.addAll(next.items);
      cont = next.continuation;
    }
    return LibraryPage(items: items);
  }

  Future<HistoryPage> musicHistory() async {
    final res = await innerTube.browse(
      _webRemix,
      browseId: 'FEmusic_history',
      setLogin: true,
    );
    final sections = <HistorySection>[];
    for (final c in jml(res, [
      'contents',
      'singleColumnBrowseResultsRenderer',
      'tabs',
      0,
      'tabRenderer',
      'content',
      'sectionListRenderer',
      'contents',
    ])) {
      final shelf = jm(c, ['musicShelfRenderer']);
      if (shelf == null) continue;
      final songs = jml(shelf, ['contents'])
          .map((cc) => jm(cc, ['musicResponsiveListItemRenderer']))
          .whereType<JsonMap>()
          .map((r) => historySong(ListItemRenderer(r)))
          .whereType<SongItem>()
          .toList();
      sections.add(
        HistorySection(
          title: firstRunText(jm(shelf, ['title'])) ?? '',
          songs: songs,
        ),
      );
    }
    return HistoryPage(sections: sections);
  }

  // ---------------------------------------------------------------------
  // Next / queue / related / lyrics
  // ---------------------------------------------------------------------

  Future<NextResult> next(
    WatchEndpoint endpoint, {
    String? continuation,
  }) async {
    final res = await innerTube.next(
      _webRemix,
      videoId: endpoint.videoId,
      playlistId: endpoint.playlistId,
      playlistSetVideoId: endpoint.playlistSetVideoId,
      index: endpoint.index,
      params: endpoint.params,
      continuation: continuation,
    );
    final tabs = jml(res, [
      'contents',
      'singleColumnMusicWatchNextResultsRenderer',
      'tabbedRenderer',
      'watchNextTabbedResultsRenderer',
      'tabs',
    ]);
    final queueRenderer = jm(tabs.firstOrNull, [
      'tabRenderer',
      'content',
      'musicQueueRenderer',
    ]);
    final panel =
        jm(res, ['continuationContents', 'playlistPanelContinuation']) ??
        jm(queueRenderer, ['content', 'playlistPanelRenderer']);
    if (panel == null) throw StateError('No playlist panel in next response');
    final title = firstRunText(
      jm(queueRenderer, ['header', 'musicQueueHeaderRenderer', 'subtitle']),
    );

    final songs = <SongItem>[];
    int? currentIndex;
    final contents = jml(panel, ['contents']);
    for (final c in contents) {
      final r = jm(c, ['playlistPanelVideoRenderer']);
      if (r == null) continue;
      final s = queueSong(r);
      if (s == null) continue;
      if (jb(r, ['selected']) == true) currentIndex = songs.length;
      songs.add(s);
    }

    BrowseEndpoint? tabEndpoint(int i) => BrowseEndpoint.fromJson(
      jm(tabs.length > i ? tabs[i] : null, [
        'tabRenderer',
        'endpoint',
        'browseEndpoint',
      ]),
    );

    // Automix continuation (radio)
    final automix = WatchEndpoint.fromJson(
      jm(contents.lastOrNull, [
        'automixPreviewVideoRenderer',
        'content',
        'automixPlaylistVideoRenderer',
        'navigationEndpoint',
        'watchPlaylistEndpoint',
      ]),
    );
    if (automix != null) {
      final more = await next(automix);
      return NextResult(
        title: title,
        items: [...songs, ...more.items],
        currentIndex: currentIndex,
        lyricsEndpoint: tabEndpoint(1),
        relatedEndpoint: tabEndpoint(2),
        continuation: more.continuation,
        endpoint: automix,
      );
    }
    return NextResult(
      title: title,
      items: songs,
      currentIndex: currentIndex,
      lyricsEndpoint: tabEndpoint(1),
      relatedEndpoint: tabEndpoint(2),
      continuation: continuationOf(jl(panel, ['continuations'])),
      endpoint: endpoint,
    );
  }

  Future<List<SongItem>> queue({
    List<String>? videoIds,
    String? playlistId,
  }) async {
    final res = await innerTube.getQueue(
      _webRemix,
      videoIds: videoIds,
      playlistId: playlistId,
    );
    return jml(res, ['queueDatas'])
        .map((q) => jm(q, ['content', 'playlistPanelVideoRenderer']))
        .whereType<JsonMap>()
        .map(queueSong)
        .whereType<SongItem>()
        .toList();
  }

  Future<String?> lyrics(BrowseEndpoint endpoint) async {
    final res = await innerTube.browse(
      _webRemix,
      browseId: endpoint.browseId,
      params: endpoint.params,
    );
    for (final c in jml(res, ['contents', 'sectionListRenderer', 'contents'])) {
      final d = runsText(
        jm(c, ['musicDescriptionShelfRenderer', 'description']),
      );
      if (d != null) return d;
    }
    return null;
  }

  Future<RelatedPage> related(BrowseEndpoint endpoint) async {
    final res = await innerTube.browse(_webRemix, browseId: endpoint.browseId);
    final songs = <SongItem>[];
    final albums = <AlbumItem>[];
    final artists = <ArtistItem>[];
    final playlists = <PlaylistItem>[];
    void add(YTItem item, ListItemRenderer? r) {
      if (item is SongItem) {
        final type = js(r?.overlayPlayEndpoint, [
          'watchEndpoint',
          'watchEndpointMusicSupportedConfigs',
          'watchEndpointMusicConfig',
          'musicVideoType',
        ]);
        if (type == musicVideoTypeAtv || r == null) songs.add(item);
      } else if (item is AlbumItem) {
        albums.add(item);
      } else if (item is ArtistItem) {
        artists.add(item);
      } else if (item is PlaylistItem) {
        playlists.add(item);
      }
    }

    for (final section in jml(res, [
      'contents',
      'sectionListRenderer',
      'contents',
    ])) {
      for (final c in jml(section, [
        'musicCarouselShelfRenderer',
        'contents',
      ])) {
        final list = jm(c, ['musicResponsiveListItemRenderer']);
        final two = jm(c, ['musicTwoRowItemRenderer']);
        if (list != null) {
          final lr = ListItemRenderer(list);
          final it = relatedSong(lr);
          if (it != null) add(it, lr);
        } else if (two != null) {
          final it = gridTwoRowItem(TwoRowItemRenderer(two));
          if (it != null) add(it, null);
        }
      }
      for (final c in [
        ...jml(section, ['musicShelfRenderer', 'contents']),
        ...jml(section, ['itemSectionRenderer', 'contents']),
      ]) {
        final list = jm(c, ['musicResponsiveListItemRenderer']);
        if (list != null) {
          final lr = ListItemRenderer(list);
          final it = relatedSong(lr);
          if (it != null) add(it, lr);
        }
      }
    }
    return RelatedPage(
      songs: songs,
      albums: albums,
      artists: artists,
      playlists: playlists,
    );
  }

  // ---------------------------------------------------------------------
  // Player / playback registration
  // ---------------------------------------------------------------------

  Future<Map<String, dynamic>> player(
    String videoId, {
    String? playlistId,
    required YouTubeClient client,
    int? signatureTimestamp,
  }) => innerTube.player(
    client,
    videoId,
    playlistId: playlistId,
    signatureTimestamp: signatureTimestamp,
  );

  Future<void> registerPlayback(
    String playbackTrackingUrl, {
    String? playlistId,
  }) async {
    const chars =
        'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_';
    final rnd = Random();
    final cpn = List.generate(16, (_) => chars[rnd.nextInt(64)]).join();
    final url = playbackTrackingUrl.replaceFirst(
      'https://s.youtube.com',
      'https://music.youtube.com',
    );
    await innerTube.registerPlayback(url, cpn, playlistId);
  }

  // ---------------------------------------------------------------------
  // Account actions
  // ---------------------------------------------------------------------

  Future<void> likeVideo(String videoId, bool like) => like
      ? innerTube.likeVideo(_webRemix, videoId)
      : innerTube.unlikeVideo(_webRemix, videoId);

  Future<void> likePlaylist(String playlistId, bool like) => like
      ? innerTube.likePlaylist(_webRemix, playlistId)
      : innerTube.unlikePlaylist(_webRemix, playlistId);

  Future<void> subscribeChannel(String channelId, bool subscribe) =>
      innerTube.subscribeChannel(_webRemix, channelId, subscribe: subscribe);

  Future<bool> feedback(List<String> tokens) async {
    final res = await innerTube.feedback(_webRemix, tokens);
    return jml(res, [
      'feedbackResponses',
    ]).every((r) => jb(r, ['isProcessed']) == true);
  }

  Future<bool> toggleSongLibrary(String videoId, bool add) async {
    final result = await next(WatchEndpoint(videoId: videoId));
    final song = result.items.where((s) => s.id == videoId).firstOrNull;
    final token = add ? song?.libraryAddToken : song?.libraryRemoveToken;
    if (token == null) throw StateError('Library token not available');
    return feedback([token]);
  }

  Future<String> createPlaylist(String title) async {
    final res = await innerTube.createPlaylist(_webRemix, title);
    return js(res, ['playlistId']) ?? '';
  }

  Future<void> deletePlaylist(String playlistId) =>
      innerTube.deletePlaylist(_webRemix, playlistId);

  Future<void> renamePlaylist(String playlistId, String name) =>
      innerTube.editPlaylist(_webRemix, playlistId, [
        {'action': 'ACTION_SET_PLAYLIST_NAME', 'playlistName': name},
      ]);

  Future<void> addToPlaylist(String playlistId, String videoId) => innerTube
      .editPlaylist(_webRemix, playlistId.replaceFirst(RegExp('^VL'), ''), [
        {'action': 'ACTION_ADD_VIDEO', 'addedVideoId': videoId},
      ]);

  Future<void> removeFromPlaylist(
    String playlistId,
    String videoId,
    String setVideoId,
  ) => innerTube.editPlaylist(
    _webRemix,
    playlistId.replaceFirst(RegExp('^VL'), ''),
    [
      {
        'action': 'ACTION_REMOVE_VIDEO',
        'removedVideoId': videoId,
        'setVideoId': setVideoId,
      },
    ],
  );

  List<YTItem> _distinctById(List<YTItem> items) {
    final seen = <String>{};
    return items.where((i) => seen.add(i.id)).toList();
  }
}
