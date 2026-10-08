import 'dart:async';
import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:youtube_explode_dart/youtube_explode_dart.dart' as yt;

import '../innertube/json_utils.dart';
import '../innertube/youtube.dart';
import '../innertube/youtube_client.dart';

/// A resolved, playable audio stream for a video.
class ResolvedStream {
  final String videoId;
  final String url;
  final String mimeType;
  final int bitrate;
  final int? contentLength;
  final double? loudnessDb;
  final String clientName;
  final Map<String, String> headers;
  final DateTime expiresAt;
  final int? durationSeconds;

  const ResolvedStream({
    required this.videoId,
    required this.url,
    required this.mimeType,
    required this.bitrate,
    this.contentLength,
    this.loudnessDb,
    required this.clientName,
    required this.headers,
    required this.expiresAt,
    this.durationSeconds,
  });

  bool get isExpired =>
      DateTime.now().isAfter(expiresAt.subtract(const Duration(minutes: 10)));
}

enum AudioQualityPref { auto, high, low }

class StreamResolveException implements Exception {
  final String message;
  const StreamResolveException(this.message);
  @override
  String toString() => 'StreamResolveException: $message';
}

/// Resolves googlevideo stream URLs.
///
/// Port of the decisive parts of `YTPlayerUtils.kt` / `InnerTubeXResolver`:
/// try the InnerTube `/player` endpoint with clients that hand back direct
/// (un-ciphered) URLs, ordered by measured ability to serve a whole file,
/// validate the URL with a ranged HEAD probe, and fall back to
/// `youtube_explode_dart` (which solves the signature cipher in Dart) and the
/// Piped API as a last resort.
///
/// iOS AVPlayer cannot decode WebM/Opus, so on Apple platforms only
/// `audio/mp4` (AAC) formats are ever selected.
class StreamResolver {
  StreamResolver._();
  static final StreamResolver instance = StreamResolver._();

  final _cache = <String, ResolvedStream>{};
  final _inFlight = <String, Future<ResolvedStream>>{};
  final _excluded = <String, Map<String, DateTime>>{};

  AudioQualityPref quality = AudioQualityPref.auto;

  final Dio _dio = Dio(
    BaseOptions(
      connectTimeout: const Duration(seconds: 15),
      receiveTimeout: const Duration(seconds: 20),
      validateStatus: (_) => true,
    ),
  );

  /// Ordered by measured ability to serve a whole file (see YTPlayerUtils).
  /// VISIONOS leads: measured (here and in the Android app) to answer a
  /// ranged probe with 206 and to serve whole files. ANDROID_VR/IOS answer
  /// GET 206 but HEAD 403, so the probe below is a 1-byte ranged GET.
  static const List<YouTubeClient> _directUrlClients = [
    YouTubeClient.visionOs,
    YouTubeClient.androidVr,
    YouTubeClient.ios,
    YouTubeClient.ipadOs,
  ];

  bool get _requireM4a => Platform.isIOS || Platform.isMacOS;

  Future<ResolvedStream> resolve(String videoId, {bool forceRefresh = false}) {
    final cached = _cache[videoId];
    if (!forceRefresh && cached != null && !cached.isExpired) {
      return Future.value(cached);
    }
    final existing = _inFlight[videoId];
    if (existing != null) return existing;
    // Block body on purpose: Map.remove returns the removed future and an
    // expression body would make whenComplete wait on that future (itself).
    final future = _resolve(videoId).whenComplete(() {
      _inFlight.remove(videoId);
    });
    _inFlight[videoId] = future;
    return future;
  }

  void invalidate(String videoId) => _cache.remove(videoId);

  void clearCache() {
    _cache.clear();
    _excluded.clear();
  }

  /// Record that googlevideo refused [url] mid-playback so the minting client
  /// is skipped for this track for a while.
  void onRefused(String videoId, String url) {
    final client = YouTubeClient.forStreamUrl(url);
    _excluded.putIfAbsent(videoId, () => {})[client.clientName +
        client.clientVersion] = DateTime.now().add(
      const Duration(minutes: 10),
    );
    _cache.remove(videoId);
  }

  Set<String> _excludedFor(String videoId) {
    final map = _excluded[videoId];
    if (map == null) return const {};
    final now = DateTime.now();
    map.removeWhere((_, until) => until.isBefore(now));
    return map.keys.toSet();
  }

  Future<ResolvedStream> _resolve(String videoId) async {
    final yt0 = YouTube.instance;
    if (yt0.visitorData == null) {
      try {
        await yt0.refreshVisitorData();
      } catch (_) {}
    }
    final excluded = _excludedFor(videoId);
    String? lastReason;

    for (final client in _directUrlClients) {
      if (excluded.contains(client.clientName + client.clientVersion)) continue;
      final sw = Stopwatch()..start();
      try {
        Map<String, dynamic>? res;
        for (var attempt = 0; attempt < 3 && res == null; attempt++) {
          try {
            res = await yt0
                .player(videoId, client: client)
                .timeout(const Duration(seconds: 20));
          } on DioException catch (e) {
            // InnerTube intermittently answers 400 for VISIONOS (observed with a
            // freshly minted visitorData); re-mint it and retry with backoff.
            final code = e.response?.statusCode;
            debugPrint(
              '[stream] $videoId ${client.clientName}: HTTP $code ${e.response?.data.toString().substring(0, 200)}',
            );
            if (code != 400 || attempt == 2) rethrow;
            await Future<void>.delayed(
              Duration(milliseconds: 400 * (attempt + 1)),
            );
            try {
              await yt0.refreshVisitorData();
            } catch (_) {}
          }
        }
        res!;
        final status = js(res, ['playabilityStatus', 'status']);
        debugPrint(
          '[stream] $videoId ${client.clientName}: $status in ${sw.elapsedMilliseconds}ms',
        );
        if (status != 'OK') {
          lastReason =
              '${client.clientName}: $status ${js(res, ['playabilityStatus', 'reason']) ?? ''}';
          continue;
        }
        final format = _pickFormat(
          jml(res, ['streamingData', 'adaptiveFormats']),
        );
        if (format == null) {
          lastReason = '${client.clientName}: no direct audio format';
          continue;
        }
        final url = js(format, ['url'])!;
        final headers = client.mediaHeaders();
        final contentLength = ji(format, ['contentLength']);
        final ok = await _validate(url, headers, contentLength);
        debugPrint(
          '[stream] $videoId ${client.clientName}: ${js(format, ['mimeType'])} valid=$ok (${sw.elapsedMilliseconds}ms)',
        );
        if (!ok) {
          lastReason = '${client.clientName}: stream URL rejected';
          continue;
        }
        final expires = ji(res, ['streamingData', 'expiresInSeconds']) ?? 21600;
        final stream = ResolvedStream(
          videoId: videoId,
          url: url,
          mimeType: js(format, ['mimeType']) ?? 'audio/mp4',
          bitrate: ji(format, ['bitrate']) ?? 0,
          contentLength: contentLength,
          loudnessDb:
              (jp(format, ['loudnessDb']) as num?)?.toDouble() ??
              (jp(res, ['playerConfig', 'audioConfig', 'loudnessDb']) as num?)
                  ?.toDouble(),
          clientName: client.clientName,
          headers: headers,
          expiresAt: DateTime.now().add(Duration(seconds: expires)),
          durationSeconds: ji(res, ['videoDetails', 'lengthSeconds']),
        );
        _cache[videoId] = stream;
        debugPrint('[stream] $videoId resolved via ${client.clientName}');
        return stream;
      } catch (e, st) {
        debugPrint('[stream] $videoId ${client.clientName} failed: $e\n$st');
        lastReason = '${client.clientName}: $e';
      }
    }

    // youtube_explode_dart: solves signature cipher / n-param in Dart.
    try {
      final stream = await _resolveWithExplode(videoId)
          .timeout(const Duration(seconds: 40));
      if (stream != null) {
        _cache[videoId] = stream;
        return stream;
      }
    } catch (e) {
      lastReason = 'explode: $e';
    }

    // Emergency Piped fallback.
    try {
      final stream = await _resolveWithPiped(videoId)
          .timeout(const Duration(seconds: 12));
      if (stream != null) {
        _cache[videoId] = stream;
        return stream;
      }
    } catch (e) {
      lastReason = 'piped: $e';
    }

    throw StreamResolveException(lastReason ?? 'No stream found');
  }

  JsonMap? _pickFormat(List<JsonMap> adaptive) {
    final audio = adaptive.where((f) {
      final mime = js(f, ['mimeType']) ?? '';
      if (!mime.startsWith('audio/')) return false;
      if (js(f, ['url']) == null) return false; // ciphered → unusable here
      if (jp(f, ['audioTrack', 'isAutoDubbed']) == true) return false;
      if (_requireM4a && !mime.startsWith('audio/mp4')) return false;
      return true;
    }).toList();
    if (audio.isEmpty) return null;
    int score(JsonMap f) {
      final bitrate = ji(f, ['bitrate']) ?? 0;
      final mime = js(f, ['mimeType']) ?? '';
      // Prefer opus on platforms that can play it, like the Android app.
      return bitrate +
          (!_requireM4a && mime.startsWith('audio/webm') ? 10240 : 0);
    }

    audio.sort((a, b) => score(b).compareTo(score(a)));
    switch (quality) {
      case AudioQualityPref.low:
        return audio.last;
      case AudioQualityPref.high:
      case AudioQualityPref.auto:
        return audio.first;
    }
  }

  /// Mirrors `YTPlayerUtils.validateStatus`: probe the last byte when the
  /// size is known (rejects 1 MiB "preview" URLs), else the first byte.
  /// Uses a ranged GET because googlevideo answers HEAD with 403 for the
  /// ANDROID_VR / IOS clients even though GET succeeds.
  Future<bool> _validate(
    String url,
    Map<String, String> headers,
    int? contentLength,
  ) async {
    final range = (contentLength != null && contentLength > 0)
        ? 'bytes=${contentLength - 1}-${contentLength - 1}'
        : 'bytes=0-0';
    try {
      final res = await _dio
          .get<dynamic>(
            url,
            options: Options(
              headers: {...headers, 'Range': range},
              responseType: ResponseType.bytes,
            ),
          )
          .timeout(const Duration(seconds: 15));
      final code = res.statusCode ?? 0;
      return (code >= 200 && code < 300) || code == 405;
    } on DioException catch (e) {
      // Network hiccup: accept optimistically; the player has its own retry.
      return e.type != DioExceptionType.badResponse;
    } catch (_) {
      return false;
    }
  }

  Future<ResolvedStream?> _resolveWithExplode(String videoId) async {
    final explode = yt.YoutubeExplode();
    try {
      final manifest = await explode.videos.streamsClient.getManifest(
        videoId,
        ytClients: [
          yt.YoutubeApiClient.ios,
          yt.YoutubeApiClient.androidVr,
          yt.YoutubeApiClient.tv,
          yt.YoutubeApiClient.mweb,
        ],
      );
      var audio = manifest.audioOnly.toList();
      if (_requireM4a) {
        audio = audio
            .where((s) => s.container.name.toLowerCase() == 'mp4')
            .toList();
      }
      if (audio.isEmpty) return null;
      audio.sort(
        (a, b) => b.bitrate.bitsPerSecond.compareTo(a.bitrate.bitsPerSecond),
      );
      final chosen = quality == AudioQualityPref.low ? audio.last : audio.first;
      final url = chosen.url.toString();
      final client = YouTubeClient.forStreamUrl(url);
      return ResolvedStream(
        videoId: videoId,
        url: url,
        mimeType: '${chosen.codec.mimeType}; codecs="${chosen.audioCodec}"',
        bitrate: chosen.bitrate.bitsPerSecond,
        contentLength: chosen.size.totalBytes,
        clientName: 'explode:${client.clientName}',
        headers: client.mediaHeaders(),
        expiresAt: DateTime.now().add(const Duration(hours: 5)),
      );
    } finally {
      explode.close();
    }
  }

  Future<ResolvedStream?> _resolveWithPiped(String videoId) async {
    final res = await _dio.get<dynamic>(
      'https://pipedapi.kavin.rocks/streams/$videoId',
      options: Options(responseType: ResponseType.json),
    );
    if (res.statusCode != 200) return null;
    final streams = jml(res.data, ['audioStreams']);
    JsonMap? best;
    var bestBitrate = 0;
    for (final s in streams) {
      final url = js(s, ['url']);
      final mime = js(s, ['mimeType']) ?? '';
      if (url == null || url.isEmpty) continue;
      if (_requireM4a && !mime.contains('mp4')) continue;
      final br = ji(s, ['bitrate']) ?? 0;
      if (br > bestBitrate) {
        bestBitrate = br;
        best = s;
      }
    }
    if (best == null) return null;
    return ResolvedStream(
      videoId: videoId,
      url: js(best, ['url'])!,
      mimeType: js(best, ['mimeType']) ?? 'audio/mp4',
      bitrate: bestBitrate,
      clientName: 'piped',
      headers: const {},
      expiresAt: DateTime.now().add(const Duration(hours: 5)),
    );
  }
}
