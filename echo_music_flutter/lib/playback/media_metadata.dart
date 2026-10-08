import 'dart:convert';

import 'package:audio_service/audio_service.dart';

import '../innertube/models/yt_item.dart';

/// Port of `models/MediaMetadata.kt` — the playback-side description of a
/// track, carried inside an `audio_service` [MediaItem]'s extras.
class MediaMetadata {
  final String id;
  final String title;
  final List<Artist> artists;
  final int duration; // seconds, -1 when unknown
  final String? thumbnailUrl;
  final Album? album;
  final String? setVideoId;
  final String? musicVideoType;
  final bool explicit;
  final bool isLocal;
  final String? localPath;

  const MediaMetadata({
    required this.id,
    required this.title,
    required this.artists,
    this.duration = -1,
    this.thumbnailUrl,
    this.album,
    this.setVideoId,
    this.musicVideoType,
    this.explicit = false,
    this.isLocal = false,
    this.localPath,
  });

  bool get isVideoSong =>
      musicVideoType != null && musicVideoType != musicVideoTypeAtv;

  String get artistsText => artists.map((a) => a.name).join(', ');

  factory MediaMetadata.fromSongItem(SongItem s) => MediaMetadata(
    id: s.id,
    title: s.title,
    artists: s.artists,
    duration: s.duration ?? -1,
    thumbnailUrl: s.thumbnail,
    album: s.album,
    setVideoId: s.setVideoId,
    musicVideoType: s.musicVideoType,
    explicit: s.explicit,
  );

  SongItem toSongItem() => SongItem(
    id: id,
    title: title,
    artists: artists,
    album: album,
    duration: duration > 0 ? duration : null,
    musicVideoType: musicVideoType,
    thumbnail: thumbnailUrl ?? '',
    explicit: explicit,
    setVideoId: setVideoId,
  );

  Map<String, dynamic> toJson() => {
    'id': id,
    'title': title,
    'artists': artists.map((a) => a.toJson()).toList(),
    'duration': duration,
    'thumbnailUrl': thumbnailUrl,
    'album': album?.toJson(),
    'setVideoId': setVideoId,
    'musicVideoType': musicVideoType,
    'explicit': explicit,
    'isLocal': isLocal,
    'localPath': localPath,
  };

  static MediaMetadata fromJson(Map<String, dynamic> j) => MediaMetadata(
    id: j['id'] as String,
    title: j['title'] as String? ?? '',
    artists: ((j['artists'] as List?) ?? const [])
        .map((e) => Artist.fromJson(Map<String, dynamic>.from(e as Map)))
        .toList(),
    duration: j['duration'] as int? ?? -1,
    thumbnailUrl: j['thumbnailUrl'] as String?,
    album: j['album'] == null
        ? null
        : Album.fromJson(Map<String, dynamic>.from(j['album'] as Map)),
    setVideoId: j['setVideoId'] as String?,
    musicVideoType: j['musicVideoType'] as String?,
    explicit: j['explicit'] as bool? ?? false,
    isLocal: j['isLocal'] as bool? ?? false,
    localPath: j['localPath'] as String?,
  );

  MediaItem toMediaItem() => MediaItem(
    id: id,
    title: title,
    artist: artistsText,
    album: album?.name,
    duration: duration > 0 ? Duration(seconds: duration) : null,
    artUri: (thumbnailUrl != null && thumbnailUrl!.startsWith('http'))
        ? Uri.tryParse(thumbnailUrl!)
        : null,
    extras: {'meta': jsonEncode(toJson())},
  );

  static MediaMetadata? fromMediaItem(MediaItem? item) {
    if (item == null) return null;
    final raw = item.extras?['meta'];
    if (raw is String) {
      try {
        return fromJson(jsonDecode(raw) as Map<String, dynamic>);
      } catch (_) {}
    }
    return MediaMetadata(
      id: item.id,
      title: item.title,
      artists: [Artist(name: item.artist ?? '')],
      duration: item.duration?.inSeconds ?? -1,
      thumbnailUrl: item.artUri?.toString(),
    );
  }

  MediaMetadata copyWith({
    int? duration,
    String? thumbnailUrl,
    List<Artist>? artists,
    Album? album,
  }) => MediaMetadata(
    id: id,
    title: title,
    artists: artists ?? this.artists,
    duration: duration ?? this.duration,
    thumbnailUrl: thumbnailUrl ?? this.thumbnailUrl,
    album: album ?? this.album,
    setVideoId: setVideoId,
    musicVideoType: musicVideoType,
    explicit: explicit,
    isLocal: isLocal,
    localPath: localPath,
  );
}
