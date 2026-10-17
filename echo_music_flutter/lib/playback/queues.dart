import '../innertube/models/yt_item.dart';
import '../innertube/youtube.dart';
import 'media_metadata.dart';

class QueueStatus {
  final String? title;
  final List<MediaMetadata> items;
  final int mediaItemIndex;
  final Duration position;
  const QueueStatus({
    this.title,
    required this.items,
    required this.mediaItemIndex,
    this.position = Duration.zero,
  });

  /// Re-point [mediaItemIndex] after filtering (port of `withFilteredItems`).
  QueueStatus filtered(bool Function(MediaMetadata) keep) {
    final current = mediaItemIndex >= 0 && mediaItemIndex < items.length
        ? items[mediaItemIndex]
        : null;
    final newItems = items.where(keep).toList();
    var newIndex = -1;
    if (current != null) {
      final occurrence =
          items
              .take(mediaItemIndex + 1)
              .where((i) => i.id == current.id)
              .length -
          1;
      var seen = -1;
      for (var i = 0; i < newItems.length; i++) {
        if (newItems[i].id == current.id) {
          seen++;
          if (seen == occurrence) {
            newIndex = i;
            break;
          }
        }
      }
    }
    return QueueStatus(
      title: title,
      items: newItems,
      mediaItemIndex: newIndex >= 0
          ? newIndex
          : mediaItemIndex.clamp(0, newItems.isEmpty ? 0 : newItems.length - 1),
      position: position,
    );
  }
}

/// Port of `playback/queues/Queue.kt`.
abstract class PlayQueue {
  MediaMetadata? get preloadItem;
  Future<QueueStatus> getInitialStatus();
  bool hasNextPage();
  Future<List<MediaMetadata>> nextPage();
}

class ListQueue implements PlayQueue {
  final String? title;
  final List<MediaMetadata> items;
  final int startIndex;
  final Duration position;
  ListQueue({
    this.title,
    required this.items,
    this.startIndex = 0,
    this.position = Duration.zero,
  });

  @override
  MediaMetadata? get preloadItem => null;
  @override
  Future<QueueStatus> getInitialStatus() async => QueueStatus(
    title: title,
    items: items,
    mediaItemIndex: startIndex,
    position: position,
  );
  @override
  bool hasNextPage() => false;
  @override
  Future<List<MediaMetadata>> nextPage() async => const [];
}

/// Port of `YouTubeQueue.kt` — a watch endpoint (song radio, playlist,
/// album playlist) that keeps loading via `next` continuations.
class YouTubeQueue implements PlayQueue {
  WatchEndpoint endpoint;
  @override
  final MediaMetadata? preloadItem;
  String? _continuation;

  YouTubeQueue(this.endpoint, {this.preloadItem});

  static YouTubeQueue radio(MediaMetadata song) =>
      YouTubeQueue(WatchEndpoint(videoId: song.id), preloadItem: song);

  @override
  Future<QueueStatus> getInitialStatus() async {
    Object? lastError;
    for (var attempt = 0; attempt <= 3; attempt++) {
      try {
        final result = await YouTube.instance.next(
          endpoint,
          continuation: _continuation,
        );
        endpoint = result.endpoint;
        _continuation = result.continuation;
        return QueueStatus(
          title: result.title,
          items: result.items.map(MediaMetadata.fromSongItem).toList(),
          mediaItemIndex: result.currentIndex ?? 0,
        );
      } catch (e) {
        lastError = e;
        if (attempt == 0 &&
            endpoint.videoId != null &&
            endpoint.playlistId == null) {
          endpoint = WatchEndpoint(
            videoId: endpoint.videoId,
            playlistId: 'RDAMVM${endpoint.videoId}',
          );
        }
      }
    }
    throw lastError ?? StateError('Failed to load queue');
  }

  @override
  bool hasNextPage() => _continuation != null;

  @override
  Future<List<MediaMetadata>> nextPage() async {
    Object? lastError;
    for (var attempt = 0; attempt < 3; attempt++) {
      try {
        final result = await YouTube.instance.next(
          endpoint,
          continuation: _continuation,
        );
        endpoint = result.endpoint;
        _continuation = result.continuation;
        return result.items.map(MediaMetadata.fromSongItem).toList();
      } catch (e) {
        lastError = e;
      }
    }
    _continuation = null;
    throw lastError ?? StateError('Failed to load next page');
  }
}

/// Port of `YouTubeAlbumRadio.kt` — all album songs first, then radio.
class YouTubeAlbumRadio implements PlayQueue {
  final String playlistId;
  final String? albumTitle;
  int _albumSongCount = 0;
  String? _continuation;
  bool _firstTimeLoaded = false;

  YouTubeAlbumRadio(this.playlistId, {this.albumTitle});

  WatchEndpoint get _endpoint => WatchEndpoint(playlistId: playlistId);

  @override
  MediaMetadata? get preloadItem => null;

  @override
  Future<QueueStatus> getInitialStatus() async {
    final songs = await YouTube.instance.albumSongs(playlistId);
    _albumSongCount = songs.length;
    return QueueStatus(
      title: albumTitle ?? songs.firstOrNull?.album?.name,
      items: songs.map(MediaMetadata.fromSongItem).toList(),
      mediaItemIndex: 0,
    );
  }

  @override
  bool hasNextPage() => !_firstTimeLoaded || _continuation != null;

  @override
  Future<List<MediaMetadata>> nextPage() async {
    final result = await YouTube.instance.next(
      _endpoint,
      continuation: _continuation,
    );
    _continuation = result.continuation;
    final items = result.items.map(MediaMetadata.fromSongItem).toList();
    if (!_firstTimeLoaded) {
      _firstTimeLoaded = true;
      return items.length > _albumSongCount
          ? items.sublist(_albumSongCount)
          : const [];
    }
    return items;
  }
}

extension _FirstOrNull<E> on List<E> {
  E? get firstOrNull => isEmpty ? null : first;
}
