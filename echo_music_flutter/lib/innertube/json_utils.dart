/// Helpers for navigating InnerTube's deeply nested JSON without modelling
/// every renderer class. Mirrors the extension helpers in the Kotlin
/// `:innertube` module (`Runs.kt`, `MusicShelfRenderer.kt`, etc.).
library;

typedef JsonMap = Map<String, dynamic>;

/// Safely walk a path of String keys / int indices through nested
/// Map/List JSON. Returns null when any step is missing or the wrong type.
dynamic jp(dynamic node, List<Object> path) {
  dynamic cur = node;
  for (final step in path) {
    if (cur == null) return null;
    if (step is String) {
      if (cur is Map) {
        cur = cur[step];
      } else {
        return null;
      }
    } else if (step is int) {
      if (cur is List) {
        if (step < 0 || step >= cur.length) return null;
        cur = cur[step];
      } else {
        return null;
      }
    }
  }
  return cur;
}

JsonMap? jm(dynamic node, [List<Object> path = const []]) {
  final v = jp(node, path);
  if (v is Map) return v is JsonMap ? v : JsonMap.from(v);
  return null;
}

List<dynamic>? jl(dynamic node, [List<Object> path = const []]) {
  final v = jp(node, path);
  return v is List ? v : null;
}

List<JsonMap> jml(dynamic node, [List<Object> path = const []]) {
  final l = jl(node, path);
  if (l == null) return const [];
  return l
      .whereType<Map>()
      .map((e) => e is JsonMap ? e : JsonMap.from(e))
      .toList();
}

String? js(dynamic node, [List<Object> path = const []]) {
  final v = jp(node, path);
  return v is String ? v : null;
}

int? ji(dynamic node, [List<Object> path = const []]) {
  final v = jp(node, path);
  if (v is int) return v;
  if (v is num) return v.toInt();
  if (v is String) return int.tryParse(v);
  return null;
}

bool? jb(dynamic node, [List<Object> path = const []]) {
  final v = jp(node, path);
  return v is bool ? v : null;
}

/// A text run: `{text, navigationEndpoint}`
class Run {
  final String text;
  final JsonMap? navigationEndpoint;
  const Run(this.text, this.navigationEndpoint);

  String? get browseId =>
      js(navigationEndpoint, ['browseEndpoint', 'browseId']);
  String? get pageType => js(navigationEndpoint, [
    'browseEndpoint',
    'browseEndpointContextSupportedConfigs',
    'browseEndpointContextMusicConfig',
    'pageType',
  ]);
  String? get watchVideoId =>
      js(navigationEndpoint, ['watchEndpoint', 'videoId']);
  String? get watchMusicVideoType => js(navigationEndpoint, [
    'watchEndpoint',
    'watchEndpointMusicSupportedConfigs',
    'watchEndpointMusicConfig',
    'musicVideoType',
  ]);
}

/// Parse `{"runs": [...]}` into a list of [Run].
List<Run>? runs(dynamic runsNode) {
  final list = jl(runsNode, ['runs']);
  if (list == null) return null;
  return list
      .whereType<Map>()
      .map(
        (r) => Run((r['text'] as String?) ?? '', jm(r, ['navigationEndpoint'])),
      )
      .toList();
}

String? runsText(dynamic runsNode) {
  final r = runs(runsNode);
  if (r == null) return null;
  return r.map((e) => e.text).join();
}

String? firstRunText(dynamic runsNode) => runs(runsNode)?.firstOrNull?.text;
String? lastRunText(dynamic runsNode) => runs(runsNode)?.lastOrNull?.text;

extension RunListExt on List<Run> {
  /// Split by the " • " separator run.
  List<List<Run>> splitBySeparator() {
    final res = <List<Run>>[];
    var tmp = <Run>[];
    for (final run in this) {
      if (run.text == ' • ') {
        res.add(tmp);
        tmp = <Run>[];
      } else {
        tmp.add(run);
      }
    }
    res.add(tmp);
    return res;
  }

  List<Run> oddElements() {
    final out = <Run>[];
    for (var i = 0; i < length; i += 2) {
      out.add(this[i]);
    }
    return out;
  }
}

extension RunListListExt on List<List<Run>> {
  /// Drop a leading "type" segment (e.g. "Song") when it has no endpoint and
  /// is not an artist list.
  List<List<Run>> clean() {
    final first = isNotEmpty && this[0].isNotEmpty ? this[0][0] : null;
    if (first == null) return this;
    if (first.navigationEndpoint != null ||
        RegExp('[&,]').hasMatch(first.text)) {
      return this;
    }
    return sublist(1);
  }
}

extension IterableFirstOrNull<E> on Iterable<E> {
  E? get firstOrNull => isEmpty ? null : first;
  E? get lastOrNull => isEmpty ? null : last;
}

/// `thumbnail.thumbnails.last.url` from a thumbnail renderer-like node.
String? thumbnailUrl(dynamic thumbnailsNode) {
  final list = jml(thumbnailsNode, ['thumbnails']);
  return list.isEmpty ? null : js(list.last, ['url']);
}

/// `musicThumbnailRenderer.thumbnail.thumbnails.last.url` (also accepts the
/// croppedSquareThumbnailRenderer variant).
String? rendererThumbnail(dynamic thumbnailRendererNode) {
  final mtr =
      jm(thumbnailRendererNode, ['musicThumbnailRenderer']) ??
      jm(thumbnailRendererNode, ['croppedSquareThumbnailRenderer']);
  return thumbnailUrl(jm(mtr, ['thumbnail']));
}

/// "3:45" -> seconds
int? parseTime(String? s) {
  if (s == null) return null;
  try {
    final parts = s.split(':').map((p) => int.parse(p.trim())).toList();
    if (parts.length == 2) return parts[0] * 60 + parts[1];
    if (parts.length == 3) return parts[0] * 3600 + parts[1] * 60 + parts[2];
  } catch (_) {}
  return null;
}

bool hasExplicitBadge(List<JsonMap>? badges) {
  if (badges == null) return false;
  return badges.any(
    (b) =>
        js(b, ['musicInlineBadgeRenderer', 'icon', 'iconType']) ==
        'MUSIC_EXPLICIT_BADGE',
  );
}

String? continuationOf(dynamic continuationsNode) {
  final list = jml(continuationsNode);
  if (list.isEmpty) return null;
  final first = list.first;
  return js(first, ['nextContinuationData', 'continuation']) ??
      js(first, ['nextRadioContinuationData', 'continuation']);
}

/// Continuation token embedded in a `continuationItemRenderer` within a list
/// of shelf contents.
String? contentsContinuation(List<JsonMap> contents) {
  for (final c in contents) {
    final token = js(c, [
      'continuationItemRenderer',
      'continuationEndpoint',
      'continuationCommand',
      'token',
    ]);
    if (token != null) return token;
  }
  return null;
}

/// Count text extraction ("1.2M subscribers" -> "1.2M")
final _countRegex = RegExp(
  r'\p{Nd}[\p{Nd}\s,.，．]*[KkMmBbTt万萬億亿兆千천만억]*',
  unicode: true,
);

String? extractCountText(dynamic runsNode) {
  final r = runs(runsNode);
  if (r == null) return null;
  final texts = r.map((e) => e.text.trim()).where((t) => t.isNotEmpty).toList();
  final joined = texts.join();
  final m = _countRegex.firstMatch(joined);
  if (m != null) {
    final v = m.group(0)!.trim().replaceAll(RegExp(r'\s+(?=[KkMmBbTt]$)'), '');
    if (v.contains(RegExp(r'\d'))) return v;
  }
  for (final t in texts) {
    final mm = _countRegex.firstMatch(t);
    if (mm != null) {
      final v = mm.group(0)!.trim();
      if (v.contains(RegExp(r'\d'))) return v;
    }
  }
  return null;
}
