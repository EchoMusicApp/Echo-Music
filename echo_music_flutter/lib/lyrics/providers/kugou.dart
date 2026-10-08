import 'dart:convert';

import 'package:dio/dio.dart';

/// Port of `kugou/KuGou.kt`.
class KuGouProvider {
  KuGouProvider._();
  static final instance = KuGouProvider._();
  final _dio = Dio(
    BaseOptions(
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 15),
      responseType: ResponseType.plain,
    ),
  );

  static const _durationTolerance = 8;
  static final _accepted = RegExp(r'\[(\d\d):(\d\d)\.(\d{2,3})\].*');
  static final _banned = RegExp(r'.+].+[:：].+');

  Future<String?> getLyrics(
    String title,
    String artist,
    int duration, {
    String? album,
  }) async {
    final keyword = _keyword(title, artist, album);
    final candidate = await _candidate(keyword, duration);
    if (candidate == null) return null;
    final content = await _download(candidate.$1, candidate.$2);
    if (content == null) return null;
    return _normalize(utf8.decode(base64Decode(content)));
  }

  String _keyword(String title, String artist, String? album) {
    final t = title
        .replaceAll(RegExp(r'\(.*\)'), '')
        .replaceAll(RegExp(r'（.*）'), '')
        .replaceAll(RegExp(r'「.*」'), '')
        .replaceAll(RegExp(r'『.*』'), '')
        .replaceAll(RegExp(r'<.*>'), '')
        .replaceAll(RegExp(r'《.*》'), '');
    final a = artist
        .replaceAll(', ', '、')
        .replaceAll(' & ', '、')
        .replaceAll('.', '')
        .replaceAll('和', '、')
        .replaceAll(RegExp(r'\(.*\)'), '')
        .replaceAll(RegExp(r'（.*）'), '');
    final sb = StringBuffer('$t - $a');
    if (album != null && album.trim().isNotEmpty) sb.write(' $album');
    return sb.toString();
  }

  Future<Map<String, dynamic>?> _getJson(
    String url,
    Map<String, dynamic> q,
  ) async {
    try {
      final res = await _dio.get<String>(url, queryParameters: q);
      if (res.data == null) return null;
      return jsonDecode(res.data!) as Map<String, dynamic>;
    } catch (_) {
      return null;
    }
  }

  Future<(int, String)?> _candidate(String keyword, int duration) async {
    final songs = await _getJson(
      'https://mobileservice.kugou.com/api/v3/search/song',
      {
        'version': 9108,
        'plat': 0,
        'pagesize': 8,
        'showtype': 0,
        'keyword': keyword,
      },
    );
    final infos = ((songs?['data'] as Map?)?['info'] as List?) ?? const [];
    for (final s in infos.whereType<Map>()) {
      final d = (s['duration'] as num?)?.toInt() ?? 0;
      if (duration <= 0 || (d - duration).abs() <= _durationTolerance) {
        final hash = s['hash'] as String?;
        if (hash == null) continue;
        final c = _firstCandidate(
          await _getJson('https://lyrics.kugou.com/search', {
            'ver': 1,
            'man': 'yes',
            'client': 'pc',
            'hash': hash,
          }),
        );
        if (c != null) return c;
      }
    }
    return _firstCandidate(
      await _getJson('https://lyrics.kugou.com/search', {
        'ver': 1,
        'man': 'yes',
        'client': 'pc',
        if (duration > 0) 'duration': duration * 1000,
        'keyword': keyword,
      }),
    );
  }

  (int, String)? _firstCandidate(Map<String, dynamic>? res) {
    final list = (res?['candidates'] as List?) ?? const [];
    for (final c in list.whereType<Map>()) {
      final id = (c['id'] as num?)?.toInt();
      final key = c['accesskey'] as String?;
      if (id != null && key != null) return (id, key);
    }
    return null;
  }

  Future<String?> _download(int id, String accessKey) async {
    final res = await _getJson('https://lyrics.kugou.com/download', {
      'fmt': 'lrc',
      'charset': 'utf8',
      'client': 'pc',
      'ver': 1,
      'id': id,
      'accesskey': accessKey,
    });
    return res?['content'] as String?;
  }

  String _normalize(String raw) {
    final lines = raw.split('\n').where((l) => _accepted.hasMatch(l)).toList();
    if (lines.isEmpty) return '';
    var headCut = 0;
    for (var i = (30 < lines.length - 1 ? 30 : lines.length - 1); i >= 0; i--) {
      if (_banned.hasMatch(lines[i])) {
        headCut = i + 1;
        break;
      }
    }
    final filtered = lines.sublist(headCut);
    var tailCut = 0;
    final limit = (lines.length - 30 < lines.length - 1
        ? lines.length - 30
        : lines.length - 1);
    for (var i = limit; i >= 0; i--) {
      if (_banned.hasMatch(lines[lines.length - 1 - i])) {
        tailCut = i + 1;
        break;
      }
    }
    final finalLines = tailCut >= filtered.length
        ? filtered
        : filtered.sublist(0, filtered.length - tailCut);
    return finalLines.join('\n');
  }
}
