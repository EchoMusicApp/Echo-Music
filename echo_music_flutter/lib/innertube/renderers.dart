import 'json_utils.dart';
import 'models/yt_item.dart';

/// Wrapper over a `musicResponsiveListItemRenderer` JSON map.
/// Port of `MusicResponsiveListItemRenderer.kt` computed properties.
class ListItemRenderer {
  final JsonMap j;
  ListItemRenderer(this.j);

  JsonMap? get navigationEndpoint => jm(j, ['navigationEndpoint']);
  String? get _pageType => js(navigationEndpoint, [
    'browseEndpoint',
    'browseEndpointContextSupportedConfigs',
    'browseEndpointContextMusicConfig',
    'pageType',
  ]);

  bool get isSong =>
      navigationEndpoint == null ||
      jm(navigationEndpoint, ['watchEndpoint']) != null ||
      jm(navigationEndpoint, ['watchPlaylistEndpoint']) != null;
  bool get isPlaylist => _pageType == BrowseEndpoint.pageTypePlaylist;
  bool get isAlbum =>
      _pageType == BrowseEndpoint.pageTypeAlbum ||
      _pageType == BrowseEndpoint.pageTypeAudiobook;
  bool get isArtist =>
      _pageType == BrowseEndpoint.pageTypeArtist ||
      _pageType == BrowseEndpoint.pageTypeLibraryArtist;

  List<JsonMap> get flexColumns => jml(j, ['flexColumns']);
  List<JsonMap> get fixedColumns => jml(j, ['fixedColumns']);

  List<Run>? flexRuns(int index) {
    if (index >= flexColumns.length) return null;
    final col = flexColumns[index];
    final r =
        jm(col, ['musicResponsiveListItemFlexColumnRenderer']) ??
        jm(col, ['musicResponsiveListItemFixedColumnRenderer']);
    return runs(jm(r, ['text']));
  }

  List<Run>? get lastFlexRuns =>
      flexColumns.isEmpty ? null : flexRuns(flexColumns.length - 1);

  String? get fixedColumnText {
    if (fixedColumns.isEmpty) return null;
    final col = fixedColumns.first;
    final r =
        jm(col, ['musicResponsiveListItemFixedColumnRenderer']) ??
        jm(col, ['musicResponsiveListItemFlexColumnRenderer']);
    return firstRunText(jm(r, ['text']));
  }

  String? get title => flexRuns(0)?.firstOrNull?.text;
  List<Run>? get secondaryRuns => flexRuns(1);

  String? get thumbnail => rendererThumbnail(jm(j, ['thumbnail']));

  String? get playlistVideoId => js(j, ['playlistItemData', 'videoId']);
  String? get playlistSetVideoId =>
      js(j, ['playlistItemData', 'playlistSetVideoId']);

  JsonMap? get overlayPlayEndpoint => jm(j, [
    'overlay',
    'musicItemThumbnailOverlayRenderer',
    'content',
    'musicPlayButtonRenderer',
    'playNavigationEndpoint',
  ]);

  WatchEndpoint? get overlayWatchEndpoint =>
      WatchEndpoint.fromJson(jm(overlayPlayEndpoint, ['watchEndpoint']));
  WatchEndpoint? get overlayWatchPlaylistEndpoint => WatchEndpoint.fromJson(
    jm(overlayPlayEndpoint, ['watchPlaylistEndpoint']),
  );
  WatchEndpoint? get overlayAnyWatchEndpoint =>
      overlayWatchEndpoint ?? overlayWatchPlaylistEndpoint;

  String? get musicVideoType =>
      js(overlayPlayEndpoint, [
        'watchEndpoint',
        'watchEndpointMusicSupportedConfigs',
        'watchEndpointMusicConfig',
        'musicVideoType',
      ]) ??
      js(overlayPlayEndpoint, [
        'watchPlaylistEndpoint',
        'watchEndpointMusicSupportedConfigs',
        'watchEndpointMusicConfig',
        'musicVideoType',
      ]) ??
      js(navigationEndpoint, [
        'watchEndpoint',
        'watchEndpointMusicSupportedConfigs',
        'watchEndpointMusicConfig',
        'musicVideoType',
      ]);

  /// Resolve the video id from any of the known locations.
  String? get videoId =>
      playlistVideoId ??
      js(navigationEndpoint, ['watchEndpoint', 'videoId']) ??
      js(overlayPlayEndpoint, ['watchEndpoint', 'videoId']) ??
      flexRuns(0)?.firstOrNull?.watchVideoId;

  String? get setVideoId =>
      playlistSetVideoId ??
      js(navigationEndpoint, ['watchEndpoint', 'playlistSetVideoId']) ??
      js(overlayPlayEndpoint, ['watchEndpoint', 'playlistSetVideoId']) ??
      js(flexRuns(0)?.firstOrNull?.navigationEndpoint, [
        'watchEndpoint',
        'playlistSetVideoId',
      ]);

  bool get explicit => hasExplicitBadge(jml(j, ['badges']));

  List<JsonMap> get menuItems => jml(j, ['menu', 'menuRenderer', 'items']);
  WatchEndpoint? menuWatchPlaylistEndpoint(String iconType) =>
      menuWatchPlaylist(menuItems, iconType);
  LibraryTokens get libraryTokens => extractLibraryTokens(menuItems);

  String? get historyRemoveToken {
    for (final item in menuItems) {
      if (js(item, ['menuServiceItemRenderer', 'icon', 'iconType']) ==
          'REMOVE_FROM_HISTORY') {
        return js(item, [
          'menuServiceItemRenderer',
          'serviceEndpoint',
          'feedbackEndpoint',
          'feedbackToken',
        ]);
      }
    }
    return null;
  }

  String? get browseId =>
      js(navigationEndpoint, ['browseEndpoint', 'browseId']);
}

/// Wrapper over a `musicTwoRowItemRenderer` JSON map.
class TwoRowItemRenderer {
  final JsonMap j;
  TwoRowItemRenderer(this.j);

  JsonMap get navigationEndpoint => jm(j, ['navigationEndpoint']) ?? const {};
  String? get _pageType => js(navigationEndpoint, [
    'browseEndpoint',
    'browseEndpointContextSupportedConfigs',
    'browseEndpointContextMusicConfig',
    'pageType',
  ]);

  bool get isSong =>
      jm(navigationEndpoint, ['watchEndpoint']) != null ||
      jm(navigationEndpoint, ['watchPlaylistEndpoint']) != null;
  bool get isPlaylist => _pageType == BrowseEndpoint.pageTypePlaylist;
  bool get isAlbum =>
      _pageType == BrowseEndpoint.pageTypeAlbum ||
      _pageType == BrowseEndpoint.pageTypeAudiobook;
  bool get isArtist => _pageType == BrowseEndpoint.pageTypeArtist;

  List<Run>? get titleRuns => runs(jm(j, ['title']));
  List<Run>? get subtitleRuns => runs(jm(j, ['subtitle']));
  String? get title => titleRuns?.firstOrNull?.text;

  String? get thumbnail => rendererThumbnail(jm(j, ['thumbnailRenderer']));

  JsonMap? get overlayPlayEndpoint => jm(j, [
    'thumbnailOverlay',
    'musicItemThumbnailOverlayRenderer',
    'content',
    'musicPlayButtonRenderer',
    'playNavigationEndpoint',
  ]);

  String? get overlayPlaylistId =>
      js(overlayPlayEndpoint, ['watchPlaylistEndpoint', 'playlistId']) ??
      js(overlayPlayEndpoint, ['watchEndpoint', 'playlistId']);

  WatchEndpoint? get overlayWatchPlaylistEndpoint => WatchEndpoint.fromJson(
    jm(overlayPlayEndpoint, ['watchPlaylistEndpoint']),
  );

  String? get musicVideoType =>
      js(overlayPlayEndpoint, [
        'watchEndpoint',
        'watchEndpointMusicSupportedConfigs',
        'watchEndpointMusicConfig',
        'musicVideoType',
      ]) ??
      js(navigationEndpoint, [
        'watchEndpoint',
        'watchEndpointMusicSupportedConfigs',
        'watchEndpointMusicConfig',
        'musicVideoType',
      ]);

  String? get watchVideoId =>
      js(navigationEndpoint, ['watchEndpoint', 'videoId']);
  WatchEndpoint? get watchEndpoint =>
      WatchEndpoint.fromJson(jm(navigationEndpoint, ['watchEndpoint']));
  String? get browseId =>
      js(navigationEndpoint, ['browseEndpoint', 'browseId']);

  bool get explicit => hasExplicitBadge(jml(j, ['subtitleBadges']));

  List<JsonMap> get menuItems => jml(j, ['menu', 'menuRenderer', 'items']);
  WatchEndpoint? menuWatchPlaylistEndpoint(String iconType) =>
      menuWatchPlaylist(menuItems, iconType);
}

WatchEndpoint? menuWatchPlaylist(List<JsonMap> menuItems, String iconType) {
  for (final item in menuItems) {
    if (js(item, ['menuNavigationItemRenderer', 'icon', 'iconType']) ==
        iconType) {
      return WatchEndpoint.fromJson(
        jm(item, [
          'menuNavigationItemRenderer',
          'navigationEndpoint',
          'watchPlaylistEndpoint',
        ]),
      );
    }
  }
  return null;
}

class LibraryTokens {
  final String? addToken;
  final String? removeToken;
  const LibraryTokens(this.addToken, this.removeToken);
}

const _libraryAddIcons = {'LIBRARY_ADD', 'BOOKMARK_BORDER'};
const _librarySavedIcons = {'LIBRARY_SAVED', 'BOOKMARK', 'LIBRARY_REMOVE'};

/// Port of `PageHelper.extractLibraryTokensFromMenuItems`.
LibraryTokens extractLibraryTokens(List<JsonMap> menuItems) {
  String? addToken;
  String? removeToken;
  for (final item in menuItems) {
    final toggle = jm(item, ['toggleMenuServiceItemRenderer']);
    if (toggle == null) continue;
    final icon = js(toggle, ['defaultIcon', 'iconType']);
    if (icon == null || icon == 'KEEP' || icon == 'KEEP_OFF') continue;
    final defaultToken = js(toggle, [
      'defaultServiceEndpoint',
      'feedbackEndpoint',
      'feedbackToken',
    ]);
    final toggledToken = js(toggle, [
      'toggledServiceEndpoint',
      'feedbackEndpoint',
      'feedbackToken',
    ]);
    if (_libraryAddIcons.contains(icon)) {
      addToken ??= defaultToken;
      removeToken ??= toggledToken;
    } else if (_librarySavedIcons.contains(icon)) {
      removeToken ??= defaultToken;
      addToken ??= toggledToken;
    }
  }
  return LibraryTokens(addToken, removeToken);
}

List<Artist> _artistsFromRuns(List<Run>? r) =>
    (r ?? const []).map((e) => Artist(name: e.text, id: e.browseId)).toList();

// ---------------------------------------------------------------------------
// Page-specific converters
// ---------------------------------------------------------------------------

/// Generic "song-like" list item parser used by Search / Playlist / Related /
/// Library / History (they differ only in small ways; parameters capture it).
SongItem? songFromListItem(
  ListItemRenderer r, {
  bool requireArtists = true,
  int albumColumn = 2,
  bool durationFromSecondary = false,
  bool albumFromLastColumn = false,
}) {
  final id = r.videoId;
  final title = r.title;
  if (id == null || title == null) return null;
  final secondary = r.secondaryRuns?.splitBySeparator();
  final artistRuns = secondary?.firstOrNull?.oddElements();
  if (requireArtists && artistRuns == null) return null;

  Album? album;
  if (albumFromLastColumn) {
    final run = r.lastFlexRuns?.firstOrNull;
    if (run?.browseId != null) {
      album = Album(name: run!.text, id: run.browseId!);
    }
  } else {
    final run = r.flexRuns(albumColumn)?.firstOrNull;
    if (run?.browseId != null) {
      album = Album(name: run!.text, id: run.browseId!);
    }
  }

  int? duration = parseTime(r.fixedColumnText);
  if (durationFromSecondary && duration == null) {
    duration = parseTime(secondary?.lastOrNull?.firstOrNull?.text);
  }
  final thumb = r.thumbnail;
  if (thumb == null) return null;
  final tokens = r.libraryTokens;
  return SongItem(
    id: id,
    title: title,
    artists: _artistsFromRuns(artistRuns),
    album: album,
    duration: duration,
    musicVideoType: r.musicVideoType,
    thumbnail: thumb,
    explicit: r.explicit,
    endpoint: r.overlayWatchEndpoint,
    setVideoId: r.setVideoId,
    libraryAddToken: tokens.addToken,
    libraryRemoveToken: tokens.removeToken,
    historyRemoveToken: r.historyRemoveToken,
  );
}

/// Port of `SearchPage.toYTItem`.
YTItem? searchItem(ListItemRenderer r) {
  final secondary = r.secondaryRuns?.splitBySeparator();
  if (secondary == null) return null;
  if (r.isSong) {
    final id = r.videoId;
    final title = r.title;
    final artistRuns = secondary.firstOrNull?.oddElements();
    final thumb = r.thumbnail;
    if (id == null || title == null || artistRuns == null || thumb == null) {
      return null;
    }
    final albumRun = secondary.length > 1 ? secondary[1].firstOrNull : null;
    final tokens = r.libraryTokens;
    return SongItem(
      id: id,
      title: title,
      artists: _artistsFromRuns(artistRuns),
      album: albumRun?.browseId != null
          ? Album(name: albumRun!.text, id: albumRun.browseId!)
          : null,
      duration: parseTime(secondary.lastOrNull?.firstOrNull?.text),
      musicVideoType: r.musicVideoType,
      thumbnail: thumb,
      explicit: r.explicit,
      libraryAddToken: tokens.addToken,
      libraryRemoveToken: tokens.removeToken,
    );
  }
  if (r.isArtist) {
    final id = r.browseId;
    final title = r.title;
    final thumb = r.thumbnail;
    if (id == null || title == null || thumb == null) return null;
    return ArtistItem(
      id: id,
      title: title,
      thumbnail: thumb,
      shuffleEndpoint: r.menuWatchPlaylistEndpoint('MUSIC_SHUFFLE'),
      radioEndpoint: r.menuWatchPlaylistEndpoint('MIX'),
    );
  }
  if (r.isAlbum) {
    final browseId = r.browseId;
    final playlistId =
        r.overlayAnyWatchEndpoint?.playlistId ??
        r.menuWatchPlaylistEndpoint('MUSIC_SHUFFLE')?.playlistId;
    final title = r.title;
    final thumb = r.thumbnail;
    if (browseId == null ||
        playlistId == null ||
        title == null ||
        thumb == null) {
      return null;
    }
    return AlbumItem(
      browseId: browseId,
      playlistId: playlistId,
      title: title,
      artists: _artistsFromRuns(
        secondary.length > 1 ? secondary[1].oddElements() : const [],
      ),
      year: secondary.length > 2
          ? int.tryParse(secondary[2].firstOrNull?.text ?? '')
          : null,
      thumbnail: thumb,
      explicit: r.explicit,
    );
  }
  if (r.isPlaylist) {
    final id = r.browseId?.replaceFirst(RegExp('^VL'), '');
    final title = r.title;
    final thumb = r.thumbnail;
    final authorRun = secondary.firstOrNull?.firstOrNull;
    if (id == null || title == null || thumb == null) return null;
    return PlaylistItem(
      id: id,
      title: title,
      author: authorRun != null
          ? Artist(name: authorRun.text, id: authorRun.browseId)
          : null,
      songCountText: r.secondaryRuns?.lastOrNull?.text,
      thumbnail: thumb,
      playEndpoint: r.overlayWatchPlaylistEndpoint,
      shuffleEndpoint: r.menuWatchPlaylistEndpoint('MUSIC_SHUFFLE'),
      radioEndpoint: r.menuWatchPlaylistEndpoint('MIX'),
    );
  }
  return null;
}

/// Port of `SearchSummaryPage.fromMusicResponsiveListItemRenderer`.
YTItem? searchSummaryItem(ListItemRenderer r) {
  final secondary = r.secondaryRuns?.splitBySeparator().clean();
  if (r.isSong) {
    final id = r.videoId;
    final title = r.title;
    final thumb = r.thumbnail;
    final artistRuns = secondary?.firstOrNull?.oddElements();
    if (id == null || title == null || thumb == null || artistRuns == null) {
      return null;
    }
    final albumRun = (secondary != null && secondary.length > 1)
        ? secondary[1].firstOrNull
        : null;
    final tokens = r.libraryTokens;
    return SongItem(
      id: id,
      title: title,
      artists: _artistsFromRuns(artistRuns),
      album: albumRun?.browseId != null
          ? Album(name: albumRun!.text, id: albumRun.browseId!)
          : null,
      duration: parseTime(secondary?.lastOrNull?.firstOrNull?.text),
      musicVideoType: r.musicVideoType,
      thumbnail: thumb,
      explicit: r.explicit,
      libraryAddToken: tokens.addToken,
      libraryRemoveToken: tokens.removeToken,
    );
  }
  return searchItem(r);
}

/// Port of `SearchSummaryPage.fromMusicCardShelfRenderer`.
YTItem? searchCardShelfItem(JsonMap renderer) {
  final onTap = jm(renderer, ['onTap']);
  final subtitle = runs(jm(renderer, ['subtitle']))?.splitBySeparator();
  final title = firstRunText(jm(renderer, ['title']));
  final thumb = rendererThumbnail(jm(renderer, ['thumbnail']));
  final buttons = jml(renderer, ['buttons']);
  final browse = BrowseEndpoint.fromJson(jm(onTap, ['browseEndpoint']));
  WatchEndpoint? buttonEndpoint(String icon) {
    for (final b in buttons) {
      if (js(b, ['buttonRenderer', 'icon', 'iconType']) == icon) {
        return WatchEndpoint.fromJson(
          jm(b, ['buttonRenderer', 'command', 'watchPlaylistEndpoint']),
        );
      }
    }
    return null;
  }

  final watch = jm(onTap, ['watchEndpoint']);
  if (watch != null) {
    final id = js(watch, ['videoId']);
    final artistRuns = subtitle != null && subtitle.length > 1
        ? subtitle[1].oddElements()
        : null;
    if (id == null || title == null || thumb == null || artistRuns == null) {
      return null;
    }
    final albumRun = subtitle!.length > 2 ? subtitle[2].firstOrNull : null;
    return SongItem(
      id: id,
      title: title,
      artists: _artistsFromRuns(artistRuns),
      album: albumRun?.browseId != null
          ? Album(name: albumRun!.text, id: albumRun.browseId!)
          : null,
      duration: parseTime(subtitle.lastOrNull?.firstOrNull?.text),
      musicVideoType: js(watch, [
        'watchEndpointMusicSupportedConfigs',
        'watchEndpointMusicConfig',
        'musicVideoType',
      ]),
      thumbnail: thumb,
      explicit: hasExplicitBadge(jml(renderer, ['subtitleBadges'])),
    );
  }
  if (browse == null || title == null || thumb == null) return null;
  if (browse.isArtistEndpoint) {
    return ArtistItem(
      id: browse.browseId,
      title: title,
      thumbnail: thumb,
      shuffleEndpoint: buttonEndpoint('MUSIC_SHUFFLE'),
      radioEndpoint: buttonEndpoint('MIX'),
    );
  }
  if (browse.isAlbumEndpoint) {
    final firstBtn = buttons.firstOrNull;
    final playlistId =
        js(firstBtn, [
          'buttonRenderer',
          'command',
          'watchPlaylistEndpoint',
          'playlistId',
        ]) ??
        js(firstBtn, [
          'buttonRenderer',
          'command',
          'watchEndpoint',
          'playlistId',
        ]);
    if (playlistId == null) return null;
    return AlbumItem(
      browseId: browse.browseId,
      playlistId: playlistId,
      title: title,
      artists: _artistsFromRuns(
        subtitle != null && subtitle.length > 1
            ? subtitle[1].oddElements()
            : const [],
      ),
      thumbnail: thumb,
      explicit: hasExplicitBadge(jml(renderer, ['subtitleBadges'])),
    );
  }
  if (browse.isPlaylistEndpoint) {
    final headerTitle =
        runsText(
          jm(renderer, [
            'header',
            'musicCardShelfHeaderBasicRenderer',
            'title',
          ]),
        ) ??
        title;
    return PlaylistItem(
      id: browse.browseId.replaceFirst(RegExp('^VL'), ''),
      title: headerTitle,
      author: Artist(
        name:
            runs(jm(renderer, ['subtitle']))?.map((e) => e.text).join(', ') ??
            '',
        id: null,
      ),
      thumbnail: thumb,
      playEndpoint: buttonEndpoint('PLAY_ARROW'),
      shuffleEndpoint: buttonEndpoint('MUSIC_SHUFFLE'),
      radioEndpoint: buttonEndpoint('MIX'),
    );
  }
  return null;
}

/// Port of `SearchSuggestionPage.fromMusicResponsiveListItemRenderer`.
YTItem? searchSuggestionItem(ListItemRenderer r) => searchSummaryItem(r);

/// Port of `HomePage.Section.fromMusicTwoRowItemRenderer`.
YTItem? homeTwoRowItem(TwoRowItemRenderer r) {
  final title = r.title;
  final thumb = r.thumbnail;
  if (title == null || thumb == null) return null;
  if (r.isSong) {
    final id = r.watchVideoId;
    final subtitleRuns = r.subtitleRuns?.oddElements();
    if (id == null || subtitleRuns == null) return null;
    var artists = subtitleRuns
        .where(
          (run) =>
              (run.browseId?.startsWith('UC') ?? false) ||
              (run.navigationEndpoint != null &&
                  !(run.browseId?.startsWith('MPREb_') ?? false)),
        )
        .map((run) => Artist(name: run.text, id: run.browseId))
        .toList();
    if (artists.isEmpty && subtitleRuns.isNotEmpty) {
      artists = [Artist(name: subtitleRuns.first.text)];
    }
    Album? album;
    for (final run in subtitleRuns) {
      if (run.browseId?.startsWith('MPREb_') ?? false) {
        album = Album(name: run.text, id: run.browseId!);
        break;
      }
    }
    return SongItem(
      id: id,
      title: title,
      artists: artists,
      album: album,
      musicVideoType: r.musicVideoType,
      thumbnail: thumb,
      explicit: r.explicit,
      endpoint: r.watchEndpoint,
    );
  }
  if (r.isAlbum) {
    final browseId = r.browseId;
    final playlistId = r.overlayPlaylistId;
    if (browseId == null || playlistId == null) return null;
    final subs = r.subtitleRuns?.oddElements() ?? const [];
    return AlbumItem(
      browseId: browseId,
      playlistId: playlistId,
      title: title,
      artists: _artistsFromRuns(subs.length > 1 ? subs.sublist(1) : const []),
      year: int.tryParse(r.subtitleRuns?.lastOrNull?.text ?? ''),
      thumbnail: thumb,
      explicit: r.explicit,
    );
  }
  if (r.isPlaylist) {
    final id = r.browseId?.replaceFirst(RegExp('^VL'), '');
    if (id == null) return null;
    final sub = r.subtitleRuns;
    return PlaylistItem(
      id: id,
      title: title,
      author: sub != null && sub.isNotEmpty
          ? Artist(name: sub.first.text, id: sub.first.browseId)
          : null,
      songCountText: sub?.lastOrNull?.text,
      thumbnail: thumb,
      playEndpoint: r.overlayWatchPlaylistEndpoint,
      shuffleEndpoint: r.menuWatchPlaylistEndpoint('MUSIC_SHUFFLE'),
      radioEndpoint: r.menuWatchPlaylistEndpoint('MIX'),
    );
  }
  if (r.isArtist) {
    final id = r.browseId;
    if (id == null) return null;
    return ArtistItem(
      id: id,
      title: r.titleRuns?.lastOrNull?.text ?? title,
      thumbnail: thumb,
      shuffleEndpoint: r.menuWatchPlaylistEndpoint('MUSIC_SHUFFLE'),
      radioEndpoint: r.menuWatchPlaylistEndpoint('MIX'),
    );
  }
  return null;
}

/// Port of `NewReleaseAlbumPage.fromMusicTwoRowItemRenderer`.
AlbumItem? newReleaseAlbum(TwoRowItemRenderer r) {
  final browseId = r.browseId;
  final playlistId = r.overlayPlaylistId;
  final title = r.title;
  final thumb = r.thumbnail;
  if (browseId == null ||
      playlistId == null ||
      title == null ||
      thumb == null) {
    return null;
  }
  final split = r.subtitleRuns?.splitBySeparator();
  final artistRuns = split != null && split.length > 1
      ? split[1].oddElements()
      : null;
  return AlbumItem(
    browseId: browseId,
    playlistId: playlistId,
    title: title,
    artists: artistRuns != null
        ? _artistsFromRuns(artistRuns)
        : _artistsFromRuns(r.subtitleRuns?.oddElements()),
    year: int.tryParse(r.subtitleRuns?.lastOrNull?.text ?? ''),
    thumbnail: thumb,
    explicit: r.explicit,
  );
}

/// Port of `RelatedPage.fromMusicTwoRowItemRenderer` / `ArtistItemsPage`
/// / `ArtistPage.fromMusicTwoRowItemRenderer` (generic grid item).
YTItem? gridTwoRowItem(TwoRowItemRenderer r) {
  final title = r.title;
  final thumb = r.thumbnail;
  if (title == null || thumb == null) return null;
  if (r.isAlbum) {
    final browseId = r.browseId;
    final playlistId = r.overlayPlaylistId;
    if (browseId == null || playlistId == null) return null;
    final split = r.subtitleRuns?.splitBySeparator();
    return AlbumItem(
      browseId: browseId,
      playlistId: playlistId,
      title: title,
      artists: split != null && split.length > 1
          ? _artistsFromRuns(split[1].oddElements())
          : null,
      year: int.tryParse(r.subtitleRuns?.lastOrNull?.text ?? ''),
      thumbnail: thumb,
      explicit: r.explicit,
    );
  }
  if (r.isSong) {
    final id = r.watchVideoId;
    if (id == null) return null;
    final split = r.subtitleRuns?.splitBySeparator();
    final artistRuns = split?.firstOrNull?.oddElements();
    var artists = (artistRuns ?? const [])
        .where((run) => run.navigationEndpoint != null)
        .map((run) => Artist(name: run.text, id: run.browseId))
        .toList();
    if (artists.isEmpty && artistRuns != null && artistRuns.isNotEmpty) {
      artists = [Artist(name: artistRuns.first.text)];
    }
    return SongItem(
      id: id,
      title: title,
      artists: artists,
      musicVideoType: r.musicVideoType,
      thumbnail: thumb,
      explicit: r.explicit,
      endpoint: r.watchEndpoint,
    );
  }
  if (r.isPlaylist) {
    final id = r.browseId?.replaceFirst(RegExp('^VL'), '');
    if (id == null) return null;
    final sub = r.subtitleRuns;
    String? songCount;
    if (sub != null) {
      for (final run in sub.reversed) {
        if (run.text.contains(RegExp(r'\d')) &&
            !run.text.toLowerCase().contains('view')) {
          songCount = run.text;
          break;
        }
      }
    }
    return PlaylistItem(
      id: id,
      title: title,
      author: sub != null && sub.isNotEmpty
          ? Artist(name: sub.first.text, id: sub.first.browseId)
          : null,
      songCountText: songCount,
      thumbnail: thumb,
      playEndpoint: r.overlayWatchPlaylistEndpoint,
      shuffleEndpoint: r.menuWatchPlaylistEndpoint('MUSIC_SHUFFLE'),
      radioEndpoint: r.menuWatchPlaylistEndpoint('MIX'),
      isEditable: r.menuItems.any(
        (m) =>
            js(m, ['menuNavigationItemRenderer', 'icon', 'iconType']) == 'EDIT',
      ),
    );
  }
  if (r.isArtist) {
    final id = r.browseId;
    if (id == null) return null;
    return ArtistItem(
      id: id,
      title: r.titleRuns?.lastOrNull?.text ?? title,
      thumbnail: thumb,
      shuffleEndpoint: r.menuWatchPlaylistEndpoint('MUSIC_SHUFFLE'),
      radioEndpoint: r.menuWatchPlaylistEndpoint('MIX'),
    );
  }
  return null;
}

/// Port of `AlbumPage.getSong`.
SongItem? albumSong(ListItemRenderer r, AlbumItem? album) {
  final id = r.videoId;
  if (id == null) return null;
  String? title;
  final artists = <Artist>[];
  for (final col in r.flexColumns) {
    final rr = runs(
      jm(jm(col, ['musicResponsiveListItemFlexColumnRenderer']), ['text']),
    );
    if (rr == null) continue;
    for (final run in rr) {
      final type = run.watchMusicVideoType ?? run.pageType;
      if (type == null) continue;
      if (type.contains('MUSIC_VIDEO')) {
        title ??= run.text;
      } else if (type.contains('MUSIC_PAGE_TYPE_ARTIST')) {
        artists.add(Artist(name: run.text, id: run.browseId));
      }
    }
  }
  title ??= r.title;
  if (title == null) return null;
  Album? albumRef;
  if (album != null) {
    albumRef = Album(name: album.title, id: album.browseId);
  } else {
    final run = r.flexRuns(2)?.firstOrNull;
    if (run?.browseId != null) {
      albumRef = Album(name: run!.text, id: run.browseId!);
    }
  }
  final duration = parseTime(r.fixedColumnText);
  final thumb = r.thumbnail ?? album?.thumbnail;
  if (thumb == null) return null;
  final tokens = r.libraryTokens;
  return SongItem(
    id: id,
    title: title,
    artists: artists.isNotEmpty ? artists : (album?.artists ?? const []),
    album: albumRef,
    duration: duration,
    musicVideoType: r.musicVideoType,
    thumbnail: thumb,
    explicit: r.explicit,
    libraryAddToken: tokens.addToken,
    libraryRemoveToken: tokens.removeToken,
  );
}

/// Port of `NextPage.fromPlaylistPanelVideoRenderer`.
SongItem? queueSong(JsonMap renderer) {
  final longByLine = runs(jm(renderer, ['longBylineText']))?.splitBySeparator();
  if (longByLine == null) return null;
  final id = js(renderer, ['videoId']);
  final title = firstRunText(jm(renderer, ['title']));
  final artistRuns = longByLine.firstOrNull?.oddElements();
  final duration = parseTime(firstRunText(jm(renderer, ['lengthText'])));
  final thumb = thumbnailUrl(jm(renderer, ['thumbnail']));
  if (id == null || title == null || artistRuns == null || thumb == null) {
    return null;
  }
  final albumRun = longByLine.length > 1 ? longByLine[1].firstOrNull : null;
  final tokens = extractLibraryTokens(
    jml(renderer, ['menu', 'menuRenderer', 'items']),
  );
  return SongItem(
    id: id,
    title: title,
    artists: _artistsFromRuns(artistRuns),
    album: albumRun?.browseId != null
        ? Album(name: albumRun!.text, id: albumRun.browseId!)
        : null,
    duration: duration,
    musicVideoType: js(renderer, [
      'navigationEndpoint',
      'watchEndpoint',
      'watchEndpointMusicSupportedConfigs',
      'watchEndpointMusicConfig',
      'musicVideoType',
    ]),
    thumbnail: thumb,
    explicit: hasExplicitBadge(jml(renderer, ['badges'])),
    setVideoId: js(renderer, ['playlistSetVideoId']),
    libraryAddToken: tokens.addToken,
    libraryRemoveToken: tokens.removeToken,
  );
}

/// Port of `LibraryPage.fromMusicResponsiveListItemRenderer`.
YTItem? libraryListItem(ListItemRenderer r) {
  if (r.isSong) {
    final id = r.videoId;
    final title = r.title;
    final thumb = r.thumbnail;
    if (id == null || title == null || thumb == null) return null;
    final artistRuns = r.flexRuns(1)?.oddElements() ?? const [];
    final albumRun = r.flexRuns(2)?.firstOrNull;
    final tokens = r.libraryTokens;
    return SongItem(
      id: id,
      title: title,
      artists: artistRuns
          .map((e) => Artist(name: e.text, id: e.browseId ?? ''))
          .toList(),
      album: albumRun != null
          ? Album(name: albumRun.text, id: albumRun.browseId ?? '')
          : null,
      duration: parseTime(r.fixedColumnText),
      musicVideoType: r.musicVideoType,
      thumbnail: thumb,
      explicit: r.explicit,
      endpoint: r.overlayWatchEndpoint,
      libraryAddToken: tokens.addToken,
      libraryRemoveToken: tokens.removeToken,
    );
  }
  if (r.isArtist) {
    final id = r.browseId;
    final title = r.title;
    final thumb = r.thumbnail;
    if (id == null || title == null || thumb == null) return null;
    return ArtistItem(
      id: id,
      title: title,
      thumbnail: thumb,
      shuffleEndpoint: r.menuWatchPlaylistEndpoint('MUSIC_SHUFFLE'),
      radioEndpoint: r.menuWatchPlaylistEndpoint('MIX'),
    );
  }
  return null;
}

/// Port of `LibraryPage.fromMusicTwoRowItemRenderer`.
YTItem? libraryTwoRowItem(TwoRowItemRenderer r) {
  if (r.isAlbum) {
    final browseId = r.browseId;
    final playlistId = r.overlayPlaylistId;
    final title = r.title;
    final thumb = r.thumbnail;
    if (browseId == null ||
        playlistId == null ||
        title == null ||
        thumb == null) {
      return null;
    }
    return AlbumItem(
      browseId: browseId,
      playlistId: playlistId,
      title: title,
      artists: (r.subtitleRuns ?? const [])
          .where((run) => run.browseId != null)
          .map((run) => Artist(name: run.text, id: run.browseId))
          .toList(),
      year: int.tryParse(r.subtitleRuns?.lastOrNull?.text ?? ''),
      thumbnail: thumb,
      explicit: r.explicit,
    );
  }
  return gridTwoRowItem(r);
}

/// Port of `HistoryPage.fromMusicResponsiveListItemRenderer` (album in col 3).
SongItem? historySong(ListItemRenderer r) =>
    songFromListItem(r, requireArtists: false, albumColumn: 3);

/// Port of `ArtistPage` / `ArtistItemsPage` list item parsing.
SongItem? artistSong(ListItemRenderer r) =>
    songFromListItem(r, albumFromLastColumn: true);

/// Port of `PlaylistPage.fromMusicResponsiveListItemRenderer`.
SongItem? playlistSong(ListItemRenderer r) =>
    songFromListItem(r, requireArtists: false);

/// Port of `RelatedPage.fromMusicResponsiveListItemRenderer`.
SongItem? relatedSong(ListItemRenderer r) => songFromListItem(r);

/// Port of `YouTube.convertToChartItem`.
SongItem? chartListItem(ListItemRenderer r) {
  if (r.flexColumns.length < 3 || r.playlistVideoId == null) return null;
  final title = r.title;
  final artists = (r.flexRuns(1) ?? const [])
      .where((run) => run.text.trim().isNotEmpty)
      .map((run) => Artist(name: run.text, id: run.browseId))
      .toList();
  final third = r.flexRuns(2);
  final thumb = r.thumbnail;
  if (title == null || thumb == null) return null;
  return SongItem(
    id: r.playlistVideoId!,
    title: title,
    artists: artists,
    musicVideoType: r.musicVideoType,
    thumbnail: thumb,
    explicit: r.explicit,
    chartPosition: int.tryParse(third?.firstOrNull?.text ?? ''),
    chartChange: third != null && third.length > 1 ? third[1].text : null,
  );
}
