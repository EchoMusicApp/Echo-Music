/// Core InnerTube item models, ported from the Kotlin `:innertube` module
/// (`models/YTItem.kt`, `models/Endpoint.kt`).
library;

const musicVideoTypeAtv = 'MUSIC_VIDEO_TYPE_ATV';
const musicVideoTypeOmv = 'MUSIC_VIDEO_TYPE_OMV';
const musicVideoTypeUgc = 'MUSIC_VIDEO_TYPE_UGC';

class WatchEndpoint {
  final String? videoId;
  final String? playlistId;
  final String? playlistSetVideoId;
  final String? params;
  final int? index;
  final String? musicVideoType;

  const WatchEndpoint({
    this.videoId,
    this.playlistId,
    this.playlistSetVideoId,
    this.params,
    this.index,
    this.musicVideoType,
  });

  static WatchEndpoint? fromJson(Map<String, dynamic>? json) {
    if (json == null) return null;
    return WatchEndpoint(
      videoId: json['videoId'] as String?,
      playlistId: json['playlistId'] as String?,
      playlistSetVideoId: json['playlistSetVideoId'] as String?,
      params: json['params'] as String?,
      index: json['index'] as int?,
      musicVideoType:
          (json['watchEndpointMusicSupportedConfigs']
                  as Map<
                    String,
                    dynamic
                  >?)?['watchEndpointMusicConfig']?['musicVideoType']
              as String?,
    );
  }

  Map<String, dynamic> toJson() => {
    'videoId': videoId,
    'playlistId': playlistId,
    'playlistSetVideoId': playlistSetVideoId,
    'params': params,
    'index': index,
  };

  WatchEndpoint copyWith({
    String? videoId,
    String? playlistId,
    String? params,
  }) => WatchEndpoint(
    videoId: videoId ?? this.videoId,
    playlistId: playlistId ?? this.playlistId,
    playlistSetVideoId: playlistSetVideoId,
    params: params ?? this.params,
    index: index,
    musicVideoType: musicVideoType,
  );
}

class BrowseEndpoint {
  static const pageTypeAlbum = 'MUSIC_PAGE_TYPE_ALBUM';
  static const pageTypeAudiobook = 'MUSIC_PAGE_TYPE_AUDIOBOOK';
  static const pageTypePlaylist = 'MUSIC_PAGE_TYPE_PLAYLIST';
  static const pageTypeArtist = 'MUSIC_PAGE_TYPE_ARTIST';
  static const pageTypeLibraryArtist = 'MUSIC_PAGE_TYPE_LIBRARY_ARTIST';
  static const pageTypeUserChannel = 'MUSIC_PAGE_TYPE_USER_CHANNEL';

  final String browseId;
  final String? params;
  final String? pageType;

  const BrowseEndpoint({required this.browseId, this.params, this.pageType});

  static BrowseEndpoint? fromJson(Map<String, dynamic>? json) {
    if (json == null) return null;
    final browseId = json['browseId'] as String?;
    if (browseId == null) return null;
    return BrowseEndpoint(
      browseId: browseId,
      params: json['params'] as String?,
      pageType:
          (json['browseEndpointContextSupportedConfigs']
                  as Map<
                    String,
                    dynamic
                  >?)?['browseEndpointContextMusicConfig']?['pageType']
              as String?,
    );
  }

  bool get isArtistEndpoint =>
      pageType == pageTypeArtist || pageType == pageTypeLibraryArtist;
  bool get isAlbumEndpoint =>
      pageType == pageTypeAlbum || pageType == pageTypeAudiobook;
  bool get isPlaylistEndpoint => pageType == pageTypePlaylist;

  Map<String, dynamic> toJson() => {
    'browseId': browseId,
    'params': params,
    'pageType': pageType,
  };
}

class Artist {
  final String name;
  final String? id;
  const Artist({required this.name, this.id});

  Map<String, dynamic> toJson() => {'name': name, 'id': id};
  static Artist fromJson(Map<String, dynamic> j) =>
      Artist(name: j['name'] as String? ?? '', id: j['id'] as String?);
}

class Album {
  final String name;
  final String id;
  const Album({required this.name, required this.id});

  Map<String, dynamic> toJson() => {'name': name, 'id': id};
  static Album fromJson(Map<String, dynamic> j) =>
      Album(name: j['name'] as String? ?? '', id: j['id'] as String? ?? '');
}

abstract class YTItem {
  String get id;
  String get title;
  String? get thumbnail;
  bool get explicit;
  String get shareLink;
}

class SongItem implements YTItem {
  @override
  final String id;
  @override
  final String title;
  final List<Artist> artists;
  final Album? album;
  final int? duration;
  final String? musicVideoType;
  final int? chartPosition;
  final String? chartChange;
  @override
  final String thumbnail;
  @override
  final bool explicit;
  final WatchEndpoint? endpoint;
  final String? setVideoId;
  final String? libraryAddToken;
  final String? libraryRemoveToken;
  final String? historyRemoveToken;

  const SongItem({
    required this.id,
    required this.title,
    required this.artists,
    this.album,
    this.duration,
    this.musicVideoType,
    this.chartPosition,
    this.chartChange,
    required this.thumbnail,
    this.explicit = false,
    this.endpoint,
    this.setVideoId,
    this.libraryAddToken,
    this.libraryRemoveToken,
    this.historyRemoveToken,
  });

  bool get isVideoSong =>
      musicVideoType != null && musicVideoType != musicVideoTypeAtv;

  String get artistsText => artists.map((a) => a.name).join(', ');

  @override
  String get shareLink => 'https://music.youtube.com/watch?v=$id';

  SongItem copyWith({
    List<Artist>? artists,
    Album? album,
    int? duration,
    String? thumbnail,
    WatchEndpoint? endpoint,
  }) => SongItem(
    id: id,
    title: title,
    artists: artists ?? this.artists,
    album: album ?? this.album,
    duration: duration ?? this.duration,
    musicVideoType: musicVideoType,
    chartPosition: chartPosition,
    chartChange: chartChange,
    thumbnail: thumbnail ?? this.thumbnail,
    explicit: explicit,
    endpoint: endpoint ?? this.endpoint,
    setVideoId: setVideoId,
    libraryAddToken: libraryAddToken,
    libraryRemoveToken: libraryRemoveToken,
    historyRemoveToken: historyRemoveToken,
  );

  Map<String, dynamic> toJson() => {
    'type': 'song',
    'id': id,
    'title': title,
    'artists': artists.map((a) => a.toJson()).toList(),
    'album': album?.toJson(),
    'duration': duration,
    'musicVideoType': musicVideoType,
    'thumbnail': thumbnail,
    'explicit': explicit,
    'setVideoId': setVideoId,
  };

  static SongItem fromJson(Map<String, dynamic> j) => SongItem(
    id: j['id'] as String,
    title: j['title'] as String? ?? '',
    artists: ((j['artists'] as List?) ?? const [])
        .map((e) => Artist.fromJson(Map<String, dynamic>.from(e as Map)))
        .toList(),
    album: j['album'] == null
        ? null
        : Album.fromJson(Map<String, dynamic>.from(j['album'] as Map)),
    duration: j['duration'] as int?,
    musicVideoType: j['musicVideoType'] as String?,
    thumbnail: j['thumbnail'] as String? ?? '',
    explicit: j['explicit'] as bool? ?? false,
    setVideoId: j['setVideoId'] as String?,
  );
}

class AlbumItem implements YTItem {
  final String browseId;
  final String playlistId;
  @override
  final String title;
  final List<Artist>? artists;
  final int? year;
  @override
  final String thumbnail;
  @override
  final bool explicit;
  final String? description;

  const AlbumItem({
    required this.browseId,
    required this.playlistId,
    required this.title,
    this.artists,
    this.year,
    required this.thumbnail,
    this.explicit = false,
    this.description,
  });

  @override
  String get id => browseId;

  @override
  String get shareLink => 'https://music.youtube.com/playlist?list=$playlistId';

  String get artistsText => artists?.map((a) => a.name).join(', ') ?? '';

  Map<String, dynamic> toJson() => {
    'type': 'album',
    'browseId': browseId,
    'playlistId': playlistId,
    'title': title,
    'artists': artists?.map((a) => a.toJson()).toList(),
    'year': year,
    'thumbnail': thumbnail,
    'explicit': explicit,
  };

  static AlbumItem fromJson(Map<String, dynamic> j) => AlbumItem(
    browseId: j['browseId'] as String,
    playlistId: j['playlistId'] as String? ?? '',
    title: j['title'] as String? ?? '',
    artists: (j['artists'] as List?)
        ?.map((e) => Artist.fromJson(Map<String, dynamic>.from(e as Map)))
        .toList(),
    year: j['year'] as int?,
    thumbnail: j['thumbnail'] as String? ?? '',
    explicit: j['explicit'] as bool? ?? false,
  );
}

class PlaylistItem implements YTItem {
  @override
  final String id;
  @override
  final String title;
  final Artist? author;
  final String? songCountText;
  @override
  final String? thumbnail;
  final WatchEndpoint? playEndpoint;
  final WatchEndpoint? shuffleEndpoint;
  final WatchEndpoint? radioEndpoint;
  final bool isEditable;

  const PlaylistItem({
    required this.id,
    required this.title,
    this.author,
    this.songCountText,
    this.thumbnail,
    this.playEndpoint,
    this.shuffleEndpoint,
    this.radioEndpoint,
    this.isEditable = false,
  });

  @override
  bool get explicit => false;

  @override
  String get shareLink => 'https://music.youtube.com/playlist?list=$id';

  Map<String, dynamic> toJson() => {
    'type': 'playlist',
    'id': id,
    'title': title,
    'author': author?.toJson(),
    'songCountText': songCountText,
    'thumbnail': thumbnail,
  };

  static PlaylistItem fromJson(Map<String, dynamic> j) => PlaylistItem(
    id: j['id'] as String,
    title: j['title'] as String? ?? '',
    author: j['author'] == null
        ? null
        : Artist.fromJson(Map<String, dynamic>.from(j['author'] as Map)),
    songCountText: j['songCountText'] as String?,
    thumbnail: j['thumbnail'] as String?,
  );
}

class ArtistItem implements YTItem {
  @override
  final String id;
  @override
  final String title;
  @override
  final String? thumbnail;
  final String? channelId;
  final WatchEndpoint? playEndpoint;
  final WatchEndpoint? shuffleEndpoint;
  final WatchEndpoint? radioEndpoint;

  const ArtistItem({
    required this.id,
    required this.title,
    this.thumbnail,
    this.channelId,
    this.playEndpoint,
    this.shuffleEndpoint,
    this.radioEndpoint,
  });

  @override
  bool get explicit => false;

  @override
  String get shareLink => 'https://music.youtube.com/channel/$id';

  ArtistItem copyWith({String? thumbnail}) => ArtistItem(
    id: id,
    title: title,
    thumbnail: thumbnail ?? this.thumbnail,
    channelId: channelId,
    playEndpoint: playEndpoint,
    shuffleEndpoint: shuffleEndpoint,
    radioEndpoint: radioEndpoint,
  );

  Map<String, dynamic> toJson() => {
    'type': 'artist',
    'id': id,
    'title': title,
    'thumbnail': thumbnail,
    'channelId': channelId,
  };

  static ArtistItem fromJson(Map<String, dynamic> j) => ArtistItem(
    id: j['id'] as String,
    title: j['title'] as String? ?? '',
    thumbnail: j['thumbnail'] as String?,
    channelId: j['channelId'] as String?,
  );
}

YTItem? ytItemFromJson(Map<String, dynamic> j) {
  switch (j['type']) {
    case 'song':
      return SongItem.fromJson(j);
    case 'album':
      return AlbumItem.fromJson(j);
    case 'playlist':
      return PlaylistItem.fromJson(j);
    case 'artist':
      return ArtistItem.fromJson(j);
  }
  return null;
}

Map<String, dynamic> ytItemToJson(YTItem item) {
  if (item is SongItem) return item.toJson();
  if (item is AlbumItem) return item.toJson();
  if (item is PlaylistItem) return item.toJson();
  if (item is ArtistItem) return item.toJson();
  throw ArgumentError('Unknown item');
}

extension YTItemListFilters<T extends YTItem> on List<T> {
  List<T> filterExplicit(bool enabled) =>
      enabled ? where((i) => !i.explicit).toList() : this;

  List<T> filterVideoSongs(bool disableVideos) => disableVideos
      ? where((i) => !(i is SongItem && i.isVideoSong)).toList()
      : this;

  List<T> filterYoutubeShorts(bool enabled) => enabled
      ? where((i) => !(i is PlaylistItem && i.id.startsWith('SS'))).toList()
      : this;
}

class AccountInfo {
  final String name;
  final String? email;
  final String? channelHandle;
  final String? thumbnailUrl;
  const AccountInfo({
    required this.name,
    this.email,
    this.channelHandle,
    this.thumbnailUrl,
  });
}

class SearchSuggestions {
  final List<String> queries;
  final List<YTItem> recommendedItems;
  const SearchSuggestions({
    required this.queries,
    required this.recommendedItems,
  });
}
