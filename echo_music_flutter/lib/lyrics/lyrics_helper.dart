import 'dart:async';

import '../data/database.dart';
import '../data/settings.dart';
import '../innertube/models/yt_item.dart';
import '../innertube/youtube.dart';
import '../playback/media_metadata.dart';
import 'lyrics_utils.dart';
import 'providers/better_lyrics.dart';
import 'providers/kugou.dart';
import 'providers/lrclib.dart';

const lyricsNotFound = 'LYRICS_NOT_FOUND';

class LyricsWithProvider {
  final String lyrics;
  final String provider;
  const LyricsWithProvider(this.lyrics, this.provider);
  bool get isNotFound => lyrics == lyricsNotFound;
  bool get isSynced => !isNotFound && LyricsUtils.isSynced(lyrics);
}

typedef _ProviderFn = Future<String?> Function(MediaMetadata m);

/// Port of `LyricsHelper.kt`: queries the enabled providers (in parallel),
/// prefers synced results, caches in the `lyrics` table.
class LyricsHelper {
  LyricsHelper._();
  static final instance = LyricsHelper._();

  final _memory = <String, LyricsWithProvider>{};

  List<(String, _ProviderFn)> _providers() {
    final s = Settings.instance;
    final list = <(String, _ProviderFn)>[];
    if (s.enableBetterLyrics) {
      list.add((
        'BetterLyrics',
        (m) => BetterLyricsProvider.instance.getLyrics(
          m.title,
          m.artistsText,
          m.duration,
          album: m.album?.name,
        ),
      ));
    }
    if (s.enableLrcLib) {
      list.add((
        'LrcLib',
        (m) => LrcLibProvider.instance.getLyrics(
          m.title,
          m.artistsText,
          m.duration,
          album: m.album?.name,
        ),
      ));
    }
    if (s.enableKugou) {
      list.add((
        'KuGou',
        (m) => KuGouProvider.instance.getLyrics(
          m.title,
          m.artistsText,
          m.duration,
          album: m.album?.name,
        ),
      ));
    }
    if (s.enableYouTubeLyrics) {
      list.add(('YouTube', _youtubeLyrics));
    }
    return list;
  }

  Future<String?> _youtubeLyrics(MediaMetadata m) async {
    if (m.isLocal) return null;
    final next = await YouTube.instance.next(WatchEndpoint(videoId: m.id));
    final ep = next.lyricsEndpoint;
    if (ep == null) return null;
    return YouTube.instance.lyrics(ep);
  }

  Future<LyricsWithProvider> getLyrics(
    MediaMetadata m, {
    bool refresh = false,
  }) async {
    if (!refresh) {
      final cached = _memory[m.id];
      if (cached != null) return cached;
      final db = await AppDatabase.instance.getLyrics(m.id);
      if (db != null && db.lyrics != lyricsNotFound) {
        final r = LyricsWithProvider(db.lyrics, db.provider);
        _memory[m.id] = r;
        return r;
      }
    }
    final providers = _providers();
    if (providers.isEmpty) {
      return const LyricsWithProvider(lyricsNotFound, 'Unknown');
    }

    final results = await Future.wait(
      providers.map((p) async {
        try {
          final r = await p.$2(m).timeout(const Duration(seconds: 20));
          if (r == null || r.trim().isEmpty || r == lyricsNotFound) return null;
          return LyricsWithProvider(r, p.$1);
        } catch (_) {
          return null;
        }
      }),
    );

    LyricsWithProvider? best;
    for (final r in results) {
      if (r == null) continue;
      if (r.isSynced) {
        best = r;
        break;
      }
      best ??= r;
    }
    final out = best ?? const LyricsWithProvider(lyricsNotFound, 'Unknown');
    _memory[m.id] = out;
    if (!out.isNotFound) {
      await AppDatabase.instance.putLyrics(m.id, out.lyrics, out.provider);
    }
    return out;
  }

  /// Fetch results from every provider (for the "choose lyrics" sheet).
  Future<List<LyricsWithProvider>> getAllLyrics(MediaMetadata m) async {
    final providers = _providers();
    final results = await Future.wait(
      providers.map((p) async {
        try {
          final r = await p.$2(m).timeout(const Duration(seconds: 20));
          if (r == null || r.trim().isEmpty) return null;
          return LyricsWithProvider(r, p.$1);
        } catch (_) {
          return null;
        }
      }),
    );
    return results.whereType<LyricsWithProvider>().toList();
  }

  Future<void> setLyrics(String id, LyricsWithProvider l) async {
    _memory[id] = l;
    await AppDatabase.instance.putLyrics(id, l.lyrics, l.provider);
  }

  void invalidate(String id) => _memory.remove(id);
}
