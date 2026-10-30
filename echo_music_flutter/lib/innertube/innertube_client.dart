import 'dart:convert';

import 'package:crypto/crypto.dart';
import 'package:dio/dio.dart';

import 'youtube_client.dart';

/// Low-level InnerTube HTTP access. Port of `InnerTube.kt` — sends requests,
/// does not parse responses.
class InnerTubeClient {
  InnerTubeClient() {
    _dio = Dio(
      BaseOptions(
        baseUrl: YouTubeClient.apiUrlYouTubeMusic,
        connectTimeout: const Duration(seconds: 30),
        receiveTimeout: const Duration(seconds: 60),
        sendTimeout: const Duration(seconds: 60),
        responseType: ResponseType.json,
        headers: {
          'Accept': 'application/json',
          'Accept-Encoding': 'gzip, deflate',
          'Cache-Control': 'no-cache',
        },
        validateStatus: (s) => s != null && s >= 200 && s < 300,
      ),
    );
  }

  late final Dio _dio;

  YouTubeLocale locale = const YouTubeLocale(gl: 'US', hl: 'en');
  String? visitorData;
  String? dataSyncId;
  bool useLoginForBrowse = true;

  String? _cookie;
  Map<String, String> _cookieMap = const {};

  String? get cookie => _cookie;
  set cookie(String? value) {
    _cookie = value;
    _cookieMap = value == null ? const {} : parseCookieString(value);
  }

  bool get isLoggedIn => _cookie != null && _cookie!.contains('SAPISID');

  static Map<String, String> parseCookieString(String cookie) {
    final map = <String, String>{};
    for (final part in cookie.split('; ')) {
      if (part.isEmpty) continue;
      final i = part.indexOf('=');
      if (i == -1) continue;
      map[part.substring(0, i)] = part.substring(i + 1);
    }
    return map;
  }

  Map<String, String> _ytHeaders(
    YouTubeClient client, {
    bool setLogin = false,
  }) {
    final headers = <String, String>{
      'Content-Type': 'application/json',
      'X-Goog-Api-Format-Version': '1',
      'X-YouTube-Client-Name': client.clientId,
      'X-YouTube-Client-Version': client.clientVersion,
      'X-Origin': YouTubeClient.originYouTubeMusic,
      'Referer': YouTubeClient.refererYouTubeMusic,
      'User-Agent': client.userAgent,
      'Accept-Language': '${locale.hl},${locale.gl};q=0.9,en;q=0.8',
    };
    if (visitorData != null) headers['X-Goog-Visitor-Id'] = visitorData!;
    if (setLogin && client.loginSupported && _cookie != null) {
      headers['Cookie'] = _cookie!;
      final sapisid = _cookieMap['SAPISID'];
      if (sapisid != null) {
        final now = DateTime.now().millisecondsSinceEpoch ~/ 1000;
        final hash = sha1
            .convert(
              utf8.encode('$now $sapisid ${YouTubeClient.originYouTubeMusic}'),
            )
            .toString();
        headers['Authorization'] = 'SAPISIDHASH ${now}_$hash';
      }
    }
    return headers;
  }

  Future<T> _withRetry<T>(
    Future<T> Function() block, {
    int maxAttempts = 3,
  }) async {
    var delay = const Duration(milliseconds: 500);
    var attempt = 0;
    while (true) {
      try {
        return await block();
      } on DioException catch (e) {
        final transient =
            e.type == DioExceptionType.connectionTimeout ||
            e.type == DioExceptionType.receiveTimeout ||
            e.type == DioExceptionType.sendTimeout ||
            e.type == DioExceptionType.connectionError;
        attempt++;
        if (!transient || attempt >= maxAttempts) rethrow;
        await Future.delayed(delay);
        delay *= 2;
      }
    }
  }

  Future<Map<String, dynamic>> _post(
    String path,
    Map<String, dynamic> body,
    YouTubeClient client, {
    bool setLogin = false,
    Map<String, dynamic>? query,
  }) => _withRetry(() async {
    final res = await _dio.post<dynamic>(
      path,
      data: jsonEncode(body),
      queryParameters: {'prettyPrint': 'false', ...?query},
      options: Options(headers: _ytHeaders(client, setLogin: setLogin)),
    );
    final data = res.data;
    if (data is Map<String, dynamic>) return data;
    if (data is Map) return Map<String, dynamic>.from(data);
    if (data is String) return jsonDecode(data) as Map<String, dynamic>;
    return <String, dynamic>{};
  });

  Map<String, dynamic> _ctx(YouTubeClient client, {bool login = false}) =>
      client.toContext(locale, visitorData, login ? dataSyncId : null);

  Future<Map<String, dynamic>> search(
    YouTubeClient client, {
    String? query,
    String? params,
    String? continuation,
    bool? setLogin,
  }) {
    final effectiveLogin = (setLogin ?? useLoginForBrowse) && isLoggedIn;
    return _post(
      'search',
      {
        'context': _ctx(client, login: effectiveLogin),
        'query': ?query,
        'params': ?params,
      },
      client,
      setLogin: effectiveLogin,
      query: continuation != null
          ? {'continuation': continuation, 'ctoken': continuation}
          : null,
    );
  }

  Future<Map<String, dynamic>> player(
    YouTubeClient client,
    String videoId, {
    String? playlistId,
    int? signatureTimestamp,
    bool setLogin = true,
  }) {
    final ctx = _ctx(client, login: true);
    if (client.isEmbedded) {
      ctx['thirdParty'] = {
        'embedUrl': 'https://www.youtube.com/watch?v=$videoId',
      };
    }
    return _post(
      'player',
      {
        'context': ctx,
        'videoId': videoId,
        'playlistId': ?playlistId,
        if (client.useSignatureTimestamp && signatureTimestamp != null)
          'playbackContext': {
            'contentPlaybackContext': {
              'signatureTimestamp': signatureTimestamp,
            },
          },
        'contentCheckOk': true,
        'racyCheckOk': true,
      },
      client,
      setLogin: setLogin,
    );
  }

  Future<Map<String, dynamic>> browse(
    YouTubeClient client, {
    String? browseId,
    String? params,
    String? continuation,
    bool setLogin = false,
  }) {
    final effectiveLogin = (setLogin || useLoginForBrowse) && isLoggedIn;
    return _post(
      'browse',
      {
        'context': _ctx(client, login: effectiveLogin),
        'browseId': ?browseId,
        'params': ?params,
        'continuation': ?continuation,
      },
      client,
      setLogin: effectiveLogin,
    );
  }

  Future<Map<String, dynamic>> next(
    YouTubeClient client, {
    String? videoId,
    String? playlistId,
    String? playlistSetVideoId,
    int? index,
    String? params,
    String? continuation,
  }) => _post(
    'next',
    {
      'context': _ctx(client, login: true),
      'videoId': ?videoId,
      'playlistId': ?playlistId,
      'playlistSetVideoId': ?playlistSetVideoId,
      'index': ?index,
      'params': ?params,
      'continuation': ?continuation,
    },
    client,
    setLogin: true,
  );

  Future<Map<String, dynamic>> getSearchSuggestions(
    YouTubeClient client,
    String input,
  ) => _post('music/get_search_suggestions', {
    'context': _ctx(client),
    'input': input,
  }, client);

  Future<Map<String, dynamic>> getQueue(
    YouTubeClient client, {
    List<String>? videoIds,
    String? playlistId,
  }) => _post('music/get_queue', {
    'context': _ctx(client),
    'videoIds': ?videoIds,
    'playlistId': ?playlistId,
  }, client);

  Future<Map<String, dynamic>> accountMenu(YouTubeClient client) => _post(
    'account/account_menu',
    {'context': _ctx(client, login: true)},
    client,
    setLogin: true,
  );

  Future<Map<String, dynamic>> feedback(
    YouTubeClient client,
    List<String> tokens,
  ) => _post(
    'feedback',
    {'context': _ctx(client, login: true), 'feedbackTokens': tokens},
    client,
    setLogin: true,
  );

  Future<Map<String, dynamic>> _like(
    YouTubeClient client,
    String path,
    Map<String, dynamic> target,
  ) => _post(
    path,
    {'context': _ctx(client, login: true), 'target': target},
    client,
    setLogin: true,
  );

  Future<void> likeVideo(YouTubeClient c, String videoId) =>
      _like(c, 'like/like', {'videoId': videoId});
  Future<void> unlikeVideo(YouTubeClient c, String videoId) =>
      _like(c, 'like/removelike', {'videoId': videoId});
  Future<void> likePlaylist(YouTubeClient c, String playlistId) =>
      _like(c, 'like/like', {'playlistId': playlistId});
  Future<void> unlikePlaylist(YouTubeClient c, String playlistId) =>
      _like(c, 'like/removelike', {'playlistId': playlistId});

  Future<void> subscribeChannel(
    YouTubeClient c,
    String channelId, {
    bool subscribe = true,
  }) => _post(
    subscribe ? 'subscription/subscribe' : 'subscription/unsubscribe',
    {
      'context': _ctx(c, login: true),
      'channelIds': [channelId],
    },
    c,
    setLogin: true,
  );

  Future<Map<String, dynamic>> editPlaylist(
    YouTubeClient c,
    String playlistId,
    List<Map<String, dynamic>> actions,
  ) => _post(
    'browse/edit_playlist',
    {
      'context': _ctx(c, login: true),
      'playlistId': playlistId,
      'actions': actions,
    },
    c,
    setLogin: true,
  );

  Future<Map<String, dynamic>> createPlaylist(YouTubeClient c, String title) =>
      _post(
        'playlist/create',
        {'context': _ctx(c, login: true), 'title': title},
        c,
        setLogin: true,
      );

  Future<Map<String, dynamic>> deletePlaylist(
    YouTubeClient c,
    String playlistId,
  ) => _post(
    'playlist/delete',
    {'context': _ctx(c, login: true), 'playlistId': playlistId},
    c,
    setLogin: true,
  );

  Future<String> getSwJsData() => _withRetry(() async {
    final res = await _dio.get<String>(
      'https://music.youtube.com/sw.js_data',
      options: Options(
        responseType: ResponseType.plain,
        headers: {'User-Agent': YouTubeClient.userAgentWeb},
      ),
    );
    return res.data ?? '';
  });

  Future<void> registerPlayback(
    String url,
    String cpn,
    String? playlistId, {
    YouTubeClient client = YouTubeClient.webRemix,
  }) => _withRetry(() async {
    await _dio.get<dynamic>(
      url,
      queryParameters: {
        'ver': '2',
        'c': client.clientName,
        'cpn': cpn,
        'list': ?playlistId,
        if (playlistId != null)
          'referrer': 'https://music.youtube.com/playlist?list=$playlistId',
      },
      options: Options(
        headers: _ytHeaders(client, setLogin: true),
        responseType: ResponseType.plain,
        validateStatus: (_) => true,
      ),
    );
  });

  /// Plain GET helper for non-InnerTube endpoints (returns parsed JSON).
  Future<dynamic> getJson(
    String url, {
    Map<String, dynamic>? query,
    Map<String, String>? headers,
  }) async {
    final res = await _dio.get<dynamic>(
      url,
      queryParameters: query,
      options: Options(headers: headers, responseType: ResponseType.json),
    );
    final d = res.data;
    if (d is String) return jsonDecode(d);
    return d;
  }
}
