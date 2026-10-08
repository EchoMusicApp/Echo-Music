import 'package:flutter/foundation.dart';

import '../data/database.dart';
import '../data/settings.dart';
import '../innertube/models/yt_item.dart';
import '../innertube/youtube.dart';
import 'audio_handler.dart';
import 'media_metadata.dart';
import 'queues.dart';

/// Global access to the audio handler plus the play-* helpers the screens
/// use (port of `PlayerConnection` and the queue builders in the UI layer).
class PlayerController {
  PlayerController._();
  static final PlayerController instance = PlayerController._();

  late EchoAudioHandler handler;

  bool get hasQueue => handler.queueItems.value.isNotEmpty;

  /// Play a YouTube song. With a [context] list the list becomes the queue;
  /// otherwise the song's own watch endpoint (playlist/radio) is used.
  Future<void> playSong(
    SongItem song, {
    List<SongItem>? context,
    String? title,
  }) async {
    if (context != null && context.isNotEmpty) {
      final idx = context.indexWhere((s) => s.id == song.id);
      await handler.playQueue(
        ListQueue(
          title: title,
          items: context.map(MediaMetadata.fromSongItem).toList(),
          startIndex: idx < 0 ? 0 : idx,
        ),
      );
      return;
    }
    final endpoint = song.endpoint;
    if (endpoint != null && endpoint.playlistId != null) {
      await handler.playQueue(
        YouTubeQueue(
          WatchEndpoint(
            videoId: song.id,
            playlistId: endpoint.playlistId,
            params: endpoint.params,
          ),
          preloadItem: MediaMetadata.fromSongItem(song),
        ),
      );
    } else {
      await handler.playQueue(
        YouTubeQueue.radio(MediaMetadata.fromSongItem(song)),
      );
    }
  }

  Future<void> playRadio(MediaMetadata m) =>
      handler.playQueue(YouTubeQueue.radio(m));

  Future<void> playEndpoint(WatchEndpoint endpoint, {MediaMetadata? preload}) =>
      handler.playQueue(YouTubeQueue(endpoint, preloadItem: preload));

  Future<void> playLocal(
    Song song, {
    List<Song>? context,
    String? title,
  }) async {
    final list = (context ?? [song]).map((s) => s.toMediaMetadata()).toList();
    final idx = list.indexWhere((m) => m.id == song.id);
    await handler.playQueue(
      ListQueue(title: title, items: list, startIndex: idx < 0 ? 0 : idx),
    );
  }

  Future<void> playMetadataList(
    List<MediaMetadata> items, {
    int startIndex = 0,
    String? title,
    bool shuffle = false,
  }) async {
    if (items.isEmpty) return;
    var list = items;
    if (shuffle) list = List.of(items)..shuffle();
    await handler.playQueue(
      ListQueue(
        title: title,
        items: list,
        startIndex: shuffle ? 0 : startIndex,
      ),
    );
  }

  Future<void> playLocalList(
    List<Song> songs, {
    int startIndex = 0,
    String? title,
    bool shuffle = false,
  }) => playMetadataList(
    songs.map((s) => s.toMediaMetadata()).toList(),
    startIndex: startIndex,
    title: title,
    shuffle: shuffle,
  );

  Future<void> playSongItems(
    List<SongItem> songs, {
    int startIndex = 0,
    String? title,
    bool shuffle = false,
  }) => playMetadataList(
    songs.map(MediaMetadata.fromSongItem).toList(),
    startIndex: startIndex,
    title: title,
    shuffle: shuffle,
  );

  Future<void> playAlbum(
    AlbumItem album, {
    bool shuffle = false,
    List<SongItem>? songs,
  }) async {
    if (songs != null && songs.isNotEmpty) {
      await playSongItems(songs, title: album.title, shuffle: shuffle);
      return;
    }
    if (shuffle) {
      await handler.playQueue(
        YouTubeQueue(
          WatchEndpoint(playlistId: album.playlistId, params: 'wAEB8gECKAE%3D'),
        ),
      );
      return;
    }
    await handler.playQueue(
      YouTubeAlbumRadio(album.playlistId, albumTitle: album.title),
    );
  }

  Future<void> playPlaylist(
    PlaylistItem playlist, {
    bool shuffle = false,
    bool radio = false,
  }) async {
    final endpoint = radio
        ? playlist.radioEndpoint
        : shuffle
        ? playlist.shuffleEndpoint
        : playlist.playEndpoint ?? WatchEndpoint(playlistId: playlist.id);
    await handler.playQueue(
      YouTubeQueue(endpoint ?? WatchEndpoint(playlistId: playlist.id)),
    );
  }

  Future<void> playArtist(
    ArtistItem artist, {
    bool shuffle = false,
    bool radio = false,
  }) async {
    final endpoint = radio
        ? artist.radioEndpoint
        : shuffle
        ? artist.shuffleEndpoint
        : artist.playEndpoint ?? artist.shuffleEndpoint;
    if (endpoint == null) return;
    await handler.playQueue(YouTubeQueue(endpoint));
  }

  Future<void> playNext(List<MediaMetadata> items) => handler.playNext(items);
  Future<void> addToQueue(List<MediaMetadata> items) =>
      handler.addToQueue(items);

  /// Toggle like locally and (when logged in) on YouTube Music.
  Future<bool> toggleLike(MediaMetadata m) async {
    await AppDatabase.instance.toggleLike(m);
    final liked = await AppDatabase.instance.isLiked(m.id);
    if (Settings.instance.isLoggedIn &&
        Settings.instance.ytmSync &&
        !m.isLocal) {
      try {
        await YouTube.instance.likeVideo(m.id, liked);
      } catch (e) {
        debugPrint('remote like failed: $e');
      }
    }
    return liked;
  }
}

PlayerController get player => PlayerController.instance;
