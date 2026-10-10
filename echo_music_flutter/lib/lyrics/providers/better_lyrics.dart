import 'package:dio/dio.dart';
import 'package:xml/xml.dart';

/// Port of `betterlyrics/BetterLyrics.kt` + `TTMLParser.kt`: fetches Apple
/// style TTML and converts it to the app's LRC dialect with word timings.
class BetterLyricsProvider {
  BetterLyricsProvider._();
  static final instance = BetterLyricsProvider._();
  final _dio = Dio(
    BaseOptions(
      baseUrl: 'https://lyrics-api.boidu.dev',
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 15),
      validateStatus: (_) => true,
    ),
  );

  Future<String?> getLyrics(
    String title,
    String artist,
    int duration, {
    String? album,
  }) async {
    try {
      final res = await _dio.get<dynamic>(
        '/getLyrics',
        queryParameters: {
          's': title,
          'a': artist,
          if (duration > 0) 'd': duration,
          if (album != null && album.isNotEmpty) 'al': album,
        },
      );
      if (res.statusCode != 200) return null;
      final ttml = (res.data is Map)
          ? (res.data as Map)['ttml'] as String?
          : null;
      if (ttml == null || ttml.isEmpty) return null;
      final lines = parseTtml(ttml);
      if (lines.isEmpty) return null;
      return toLrc(lines);
    } catch (_) {
      return null;
    }
  }

  static List<_Line> parseTtml(String ttml) {
    final out = <_Line>[];
    try {
      final doc = XmlDocument.parse(ttml);
      for (final p in doc.findAllElements('p')) {
        final begin = p.getAttribute('begin');
        if (begin == null || begin.isEmpty) continue;
        final start = _parseTime(begin);
        final spans = <_Span>[];
        final bg = <_Line>[];
        final agent = _attr(p, 'agent');
        for (final node in p.children) {
          if (node is! XmlElement || node.name.local.toLowerCase() != 'span') {
            continue;
          }
          final role = _attr(node, 'role');
          if (role == 'x-bg') {
            final l = _parseBg(node, start);
            if (l != null) bg.add(l);
          } else if (role == 'x-translation' || role == 'x-roman') {
            continue;
          } else {
            final wb = node.getAttribute('begin') ?? '';
            final we = node.getAttribute('end') ?? '';
            final text = node.innerText.trim();
            if (text.isNotEmpty && wb.isNotEmpty && we.isNotEmpty) {
              final next = node.following.firstOrNull;
              final trailing =
                  next is XmlText && RegExp(r'\s').hasMatch(next.value);
              spans.add(_Span(text, _parseTime(wb), _parseTime(we), trailing));
            }
          }
        }
        final words = _merge(spans);
        var text = words.map((w) => w.text).join(' ');
        if (text.isEmpty) text = _directText(p).trim();
        if (text.isNotEmpty) {
          out.add(_Line(text, start, words, agent: agent, background: bg));
        }
      }
    } catch (_) {
      return const [];
    }
    return out;
  }

  static String? _attr(XmlElement e, String local) {
    for (final a in e.attributes) {
      if (a.name.local == local) return a.value.isEmpty ? null : a.value;
    }
    return null;
  }

  static _Line? _parseBg(XmlElement span, double parentStart) {
    final b = span.getAttribute('begin');
    final start = (b != null && b.isNotEmpty) ? _parseTime(b) : parentStart;
    final spans = <_Span>[];
    for (final node in span.children) {
      if (node is! XmlElement || node.name.local.toLowerCase() != 'span') {
        continue;
      }
      final role = _attr(node, 'role');
      if (role == 'x-translation' || role == 'x-roman') continue;
      final wb = node.getAttribute('begin') ?? '';
      final we = node.getAttribute('end') ?? '';
      final text = node.innerText.trim();
      if (text.isNotEmpty && wb.isNotEmpty && we.isNotEmpty) {
        final next = node.following.firstOrNull;
        final trailing = next is XmlText && RegExp(r'\s').hasMatch(next.value);
        spans.add(_Span(text, _parseTime(wb), _parseTime(we), trailing));
      }
    }
    final words = _merge(spans);
    var text = words.map((w) => w.text).join(' ');
    if (text.isEmpty) text = _directText(span).trim();
    if (text.isEmpty) return null;
    return _Line(text, start, words, isBackground: true);
  }

  static String _directText(XmlElement e) {
    final sb = StringBuffer();
    for (final n in e.children) {
      if (n is XmlText) {
        sb.write(n.value);
      } else if (n is XmlElement && n.name.local.toLowerCase() == 'span') {
        final role = _attr(n, 'role') ?? '';
        if (role != 'x-bg' && role != 'x-translation' && role != 'x-roman') {
          sb.write(n.innerText);
        }
      }
    }
    return sb.toString();
  }

  static List<_Word> _merge(List<_Span> spans) {
    if (spans.isEmpty) return const [];
    final words = <_Word>[];
    var text = StringBuffer(spans[0].text);
    var start = spans[0].start;
    var end = spans[0].end;
    for (var i = 1; i < spans.length; i++) {
      if (spans[i - 1].trailingSpace) {
        words.add(_Word(text.toString().trim(), start, end));
        text = StringBuffer(spans[i].text);
        start = spans[i].start;
        end = spans[i].end;
      } else {
        text.write(spans[i].text);
        end = spans[i].end;
      }
    }
    if (text.isNotEmpty) words.add(_Word(text.toString().trim(), start, end));
    return words;
  }

  static String toLrc(List<_Line> lines) {
    final sb = StringBuffer();
    String ts(double s) {
      final ms = (s * 1000).round();
      final m = ms ~/ 60000;
      final sec = (ms % 60000) ~/ 1000;
      final cs = (ms % 1000) ~/ 10;
      return '[${m.toString().padLeft(2, '0')}:${sec.toString().padLeft(2, '0')}.${cs.toString().padLeft(2, '0')}]';
    }

    void writeLine(_Line l, {bool bg = false}) {
      final agent = (!bg && l.agent != null && l.agent!.isNotEmpty)
          ? '{agent:${l.agent}}'
          : '';
      sb.writeln('${ts(l.start)}${bg ? '{bg}' : ''}$agent${l.text}');
      if (l.words.isNotEmpty) {
        sb.writeln(
          '<${l.words.map((w) => '${w.text}:${w.start}:${w.end}').join('|')}>',
        );
      }
    }

    for (final l in lines) {
      writeLine(l);
      for (final b in l.background) {
        writeLine(b, bg: true);
      }
    }
    return sb.toString();
  }

  static double _parseTime(String t) {
    try {
      if (t.contains(':')) {
        final parts = t.split(':').map(double.parse).toList();
        if (parts.length == 2) return parts[0] * 60 + parts[1];
        if (parts.length == 3) {
          return parts[0] * 3600 + parts[1] * 60 + parts[2];
        }
      }
      if (t.endsWith('s')) return double.parse(t.substring(0, t.length - 1));
      return double.parse(t);
    } catch (_) {
      return 0;
    }
  }
}

class _Span {
  final String text;
  final double start;
  final double end;
  final bool trailingSpace;
  _Span(this.text, this.start, this.end, this.trailingSpace);
}

class _Word {
  final String text;
  final double start;
  final double end;
  _Word(this.text, this.start, this.end);
}

class _Line {
  final String text;
  final double start;
  final List<_Word> words;
  final String? agent;
  final bool isBackground;
  final List<_Line> background;
  _Line(
    this.text,
    this.start,
    this.words, {
    this.agent,
    this.isBackground = false,
    this.background = const [],
  });
}

extension _FirstOrNull<E> on Iterable<E> {
  E? get firstOrNull => isEmpty ? null : first;
}
