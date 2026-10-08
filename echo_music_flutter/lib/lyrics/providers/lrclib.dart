import 'package:dio/dio.dart';

import '../lyrics_utils.dart';

/// Port of `lrclib/LrcLib.kt`.
class LrcLibProvider {
  LrcLibProvider._();
  static final instance = LrcLibProvider._();
  final _dio = Dio(
    BaseOptions(
      baseUrl: 'https://lrclib.net',
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 15),
      headers: {
        'User-Agent': 'EchoMusic/1.0 (https://github.com/iad1tya/Echo-Music)',
      },
    ),
  );

  Future<List<_Track>> _query({
    String? track,
    String? artist,
    String? album,
    String? q,
  }) async {
    try {
      final res = await _dio.get<dynamic>(
        '/api/search',
        queryParameters: {
          'q': ?q,
          'track_name': ?track,
          'artist_name': ?artist,
          'album_name': ?album,
        },
      );
      final data = res.data;
      if (data is! List) return const [];
      return data
          .whereType<Map>()
          .map(
            (m) => _Track(
              trackName: m['trackName'] as String? ?? '',
              artistName: m['artistName'] as String? ?? '',
              duration: (m['duration'] as num?)?.toDouble() ?? 0,
              plain: m['plainLyrics'] as String?,
              synced: m['syncedLyrics'] as String?,
            ),
          )
          .where((t) => t.synced != null || t.plain != null)
          .toList();
    } catch (_) {
      return const [];
    }
  }

  Future<List<_Track>> _search(
    String title,
    String artist,
    String? album,
  ) async {
    final cleanedTitle = MetadataCleaner.cleanTitle(title);
    final cleanedArtist = MetadataCleaner.cleanArtist(artist);
    final primary = MetadataCleaner.primaryArtist(artist);
    var r = await _query(track: cleanedTitle, artist: primary, album: album);
    if (r.isNotEmpty) return r;
    if (primary != cleanedArtist) {
      r = await _query(
        track: cleanedTitle,
        artist: cleanedArtist,
        album: album,
      );
      if (r.isNotEmpty) return r;
    }
    r = await _query(track: cleanedTitle);
    if (r.isNotEmpty) return r;
    r = await _query(q: '$cleanedArtist $cleanedTitle');
    if (r.isNotEmpty) return r;
    r = await _query(q: cleanedTitle);
    if (r.isNotEmpty) return r;
    if (cleanedTitle != title.trim()) {
      r = await _query(track: title.trim(), artist: artist.trim());
    }
    return r;
  }

  Future<String?> getLyrics(
    String title,
    String artist,
    int duration, {
    String? album,
  }) async {
    final tracks = await _search(title, artist, album);
    if (tracks.isEmpty) return null;
    _Track? best;
    if (duration <= 0) {
      final cleanedTitle = MetadataCleaner.cleanTitle(title).toLowerCase();
      final cleanedArtist = MetadataCleaner.cleanArtist(artist).toLowerCase();
      double score(_Track t) {
        var s =
            (_sim(cleanedTitle, t.trackName.toLowerCase()) +
                _sim(cleanedArtist, t.artistName.toLowerCase())) /
            2;
        if (t.synced != null) s += 0.1;
        return s;
      }

      tracks.sort((a, b) => score(b).compareTo(score(a)));
      best = tracks.first;
    } else {
      final synced = tracks.where((t) => t.synced != null).toList()
        ..sort(
          (a, b) => (a.duration - duration).abs().compareTo(
            (b.duration - duration).abs(),
          ),
        );
      if (synced.isNotEmpty && (synced.first.duration - duration).abs() <= 5) {
        best = synced.first;
      } else {
        tracks.sort(
          (a, b) => (a.duration - duration).abs().compareTo(
            (b.duration - duration).abs(),
          ),
        );
        if ((tracks.first.duration - duration).abs() <= 5) best = tracks.first;
      }
    }
    return best?.synced ?? best?.plain;
  }

  static double _sim(String a, String b) {
    if (a == b) return 1;
    if (a.isEmpty || b.isEmpty) return 0;
    if (a.contains(b) || b.contains(a)) return 0.8;
    final maxLen = a.length > b.length ? a.length : b.length;
    return 1 - _lev(a, b) / maxLen;
  }

  static int _lev(String s, String t) {
    final m = List.generate(s.length + 1, (i) => List.filled(t.length + 1, 0));
    for (var i = 0; i <= s.length; i++) {
      m[i][0] = i;
    }
    for (var j = 0; j <= t.length; j++) {
      m[0][j] = j;
    }
    for (var i = 1; i <= s.length; i++) {
      for (var j = 1; j <= t.length; j++) {
        final cost = s[i - 1] == t[j - 1] ? 0 : 1;
        m[i][j] = [
          m[i - 1][j] + 1,
          m[i][j - 1] + 1,
          m[i - 1][j - 1] + cost,
        ].reduce((a, b) => a < b ? a : b);
      }
    }
    return m[s.length][t.length];
  }
}

class _Track {
  final String trackName;
  final String artistName;
  final double duration;
  final String? plain;
  final String? synced;
  _Track({
    required this.trackName,
    required this.artistName,
    required this.duration,
    this.plain,
    this.synced,
  });
}
