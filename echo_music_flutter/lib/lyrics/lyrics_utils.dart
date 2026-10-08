/// LRC parsing — port of `LyricsUtils.parseLyrics` (standard LRC, multi
/// timestamp lines, rich-sync `<mm:ss.xx>` words, `{agent:x}` / `{bg}` tags
/// and the BetterLyrics `<word:start:end|...>` word-timing lines).
class WordTimestamp {
  final String text;
  final double startTime; // seconds
  final double endTime;
  const WordTimestamp(this.text, this.startTime, this.endTime);
}

class LyricsEntry implements Comparable<LyricsEntry> {
  final int time; // ms
  final String text;
  final List<WordTimestamp>? words;
  final String? agent;
  final bool isBackground;
  const LyricsEntry(
    this.time,
    this.text, {
    this.words,
    this.agent,
    this.isBackground = false,
  });

  static const head = LyricsEntry(0, '');

  @override
  int compareTo(LyricsEntry other) => time - other.time;
}

class LyricsUtils {
  LyricsUtils._();

  static final _lineRegex = RegExp(r'((\[\d\d:\d\d\.\d{2,3}\] ?)+)(.+)');
  static final _timeRegex = RegExp(r'\[(\d\d):(\d\d)\.(\d{2,3})\]');
  static final _richLineRegex = RegExp(r'\[(\d{1,2}):(\d{2})\.(\d{2,3})\](.+)');
  static final _richWordRegex = RegExp(
    r'<(\d{1,2}):(\d{2})\.(\d{2,3})>\s*([^<]+)',
  );
  static final _agentRegex = RegExp(r'\{agent:([^}]+)\}');
  static final _bgRegex = RegExp(r'^\{bg\}');

  static bool isSynced(String lyrics) => lyrics.trimLeft().startsWith('[');

  static List<LyricsEntry> parseLyrics(String lyrics) {
    var text = lyrics.trim();
    if (text.startsWith('"')) text = text.substring(1);
    if (text.endsWith('"')) text = text.substring(0, text.length - 1);
    text = text
        .replaceAll(r'\\', r'\')
        .replaceAll(r'\n', '\n')
        .replaceAll(r'\r', '\r')
        .replaceAll(r'\t', '\t');
    text = _decodeHtml(text);
    final lines = text
        .split('\n')
        .where((l) => l.trim().isNotEmpty && !l.trim().startsWith('[offset:'))
        .toList();
    final isRich = lines.any(
      (l) => _richLineRegex.hasMatch(l.trim()) && _richWordRegex.hasMatch(l),
    );
    final result = isRich ? _parseRich(lines) : _parseStandard(lines);
    result.sort();
    return result;
  }

  static String _decodeHtml(String s) => s
      .replaceAll('&amp;', '&')
      .replaceAll('&lt;', '<')
      .replaceAll('&gt;', '>')
      .replaceAll('&quot;', '"')
      .replaceAll('&#39;', "'")
      .replaceAll('&apos;', "'");

  static List<LyricsEntry> _parseRich(List<String> lines) {
    final result = <LyricsEntry>[];
    for (var i = 0; i < lines.length; i++) {
      final m = _richLineRegex.firstMatch(lines[i].trim());
      if (m == null) continue;
      final min = int.tryParse(m.group(1)!) ?? 0;
      final sec = int.tryParse(m.group(2)!) ?? 0;
      final frac = m.group(3)!;
      final ms = frac.length == 3 ? int.parse(frac) : int.parse(frac) * 10;
      final lineMs = min * 60000 + sec * 1000 + ms;
      var content = m.group(4)!.trimLeft();
      String? agent;
      final am = _agentRegex.firstMatch(content);
      if (am != null) {
        agent = am.group(1);
        content = content.replaceFirst(_agentRegex, '');
      }
      final isBg = _bgRegex.hasMatch(content);
      if (isBg) content = content.replaceFirst(_bgRegex, '');
      final words = _parseRichWords(content, i, lines);
      final plain = content
          .replaceAll(RegExp(r'<\d{1,2}:\d{2}\.\d{2,3}>\s*'), '')
          .trim();
      if (plain.isNotEmpty) {
        result.add(
          LyricsEntry(
            lineMs,
            plain,
            words: words,
            agent: agent,
            isBackground: isBg,
          ),
        );
      }
    }
    return result;
  }

  static double _secondsOf(RegExpMatch m) {
    final min = int.tryParse(m.group(1)!) ?? 0;
    final sec = int.tryParse(m.group(2)!) ?? 0;
    final frac = m.group(3)!;
    final fracPart = frac.length == 3
        ? int.parse(frac) / 1000.0
        : int.parse(frac) / 100.0;
    return min * 60.0 + sec + fracPart;
  }

  static List<WordTimestamp>? _parseRichWords(
    String content,
    int index,
    List<String> all,
  ) {
    final matches = _richWordRegex.allMatches(content).toList();
    if (matches.isEmpty) return null;
    final out = <WordTimestamp>[];
    for (var i = 0; i < matches.length; i++) {
      final start = _secondsOf(matches[i]);
      final text = matches[i].group(4)!.trim();
      double end;
      if (i < matches.length - 1) {
        end = _secondsOf(matches[i + 1]);
      } else {
        double? nextLine;
        if (index + 1 < all.length) {
          final nm = _richLineRegex.firstMatch(all[index + 1].trim());
          if (nm != null) nextLine = _secondsOf(nm);
        }
        end = nextLine ?? start + 0.5;
      }
      if (text.isNotEmpty) out.add(WordTimestamp(text, start, end));
    }
    return out.isEmpty ? null : out;
  }

  static List<LyricsEntry> _parseStandard(List<String> lines) {
    final result = <LyricsEntry>[];
    for (var i = 0; i < lines.length; i++) {
      final line = lines[i];
      final t = line.trim();
      if (t.startsWith('<') && t.endsWith('>')) continue;
      final entries = _parseLine(line);
      if (entries == null) continue;
      List<WordTimestamp>? words;
      if (i + 1 < lines.length) {
        final nt = lines[i + 1].trim();
        if (nt.startsWith('<') && nt.endsWith('>')) {
          words = _parseWordTimestamps(nt.substring(1, nt.length - 1));
        }
      }
      if (words != null) {
        result.addAll(
          entries.map(
            (e) => LyricsEntry(
              e.time,
              e.text,
              words: words,
              agent: e.agent,
              isBackground: e.isBackground,
            ),
          ),
        );
      } else {
        result.addAll(entries);
      }
    }
    return result;
  }

  static List<WordTimestamp>? _parseWordTimestamps(String data) {
    if (data.trim().isEmpty) return null;
    final out = <WordTimestamp>[];
    for (final w in data.split('|')) {
      final parts = w.split(':');
      if (parts.length == 3) {
        out.add(
          WordTimestamp(
            parts[0],
            double.tryParse(parts[1]) ?? 0,
            double.tryParse(parts[2]) ?? 0,
          ),
        );
      }
    }
    return out.isEmpty ? null : out;
  }

  static List<LyricsEntry>? _parseLine(String line) {
    if (line.isEmpty) return null;
    final m = _lineRegex.firstMatch(line.trim());
    if (m == null) return null;
    final times = m.group(1)!;
    var text = m.group(3)!;
    String? agent;
    final am = _agentRegex.firstMatch(text);
    if (am != null) {
      agent = am.group(1);
      text = text.replaceFirst(_agentRegex, '');
    }
    final isBg = _bgRegex.hasMatch(text);
    if (isBg) text = text.replaceFirst(_bgRegex, '');
    return _timeRegex.allMatches(times).map((tm) {
      final min = int.tryParse(tm.group(1)!) ?? 0;
      final sec = int.tryParse(tm.group(2)!) ?? 0;
      final milStr = tm.group(3)!;
      var mil = int.tryParse(milStr) ?? 0;
      if (milStr.length == 2) mil *= 10;
      return LyricsEntry(
        min * 60000 + sec * 1000 + mil,
        text,
        agent: agent,
        isBackground: isBg,
      );
    }).toList();
  }

  /// Port of `findCurrentLineIndex` (300 ms look-ahead).
  static int findCurrentLineIndex(List<LyricsEntry> lines, int positionMs) {
    for (var i = 0; i < lines.length; i++) {
      if (lines[i].time >= positionMs + 300) return i - 1;
    }
    return lines.length - 1;
  }
}

/// Simplified port of the `:metadata` module's MetadataCleaner: strips
/// "(Official Video)", "[Lyrics]", "feat." etc. so lyrics lookups match.
class MetadataCleaner {
  MetadataCleaner._();

  static final _bracketNoise = RegExp(
    r'\s*[\(\[\{][^\)\]\}]*(official|video|audio|lyric|lyrics|visualizer|hd|hq|4k|remaster|live|mv|m/v|version|edit|prod\.?|slowed|reverb|sped|clean|explicit|from\b)[^\)\]\}]*[\)\]\}]',
    caseSensitive: false,
  );
  static final _feat = RegExp(
    r'\s*[\(\[]?\s*(feat\.?|ft\.?|featuring)\s+[^\)\]]*[\)\]]?',
    caseSensitive: false,
  );
  static final _trailingNoise = RegExp(
    r'\s*[\|\-–—]\s*(official.*|lyrics?.*|audio.*|video.*)$',
    caseSensitive: false,
  );

  static String cleanTitle(String title) {
    var t = title;
    t = t.replaceAll(_bracketNoise, '');
    t = t.replaceAll(_feat, '');
    t = t.replaceAll(_trailingNoise, '');
    t = t.replaceAll(RegExp(r'\s+'), ' ').trim();
    return t.isEmpty ? title.trim() : t;
  }

  static String primaryArtist(String artist) {
    var a = artist
        .split(RegExp(r',|&| x | X |feat\.?|ft\.?', caseSensitive: false))
        .first;
    a = a.replaceAll(RegExp(r'\s*-\s*Topic$'), '').trim();
    return a.isEmpty ? artist.trim() : a;
  }

  static String cleanArtist(String artist) => artist
      .replaceAll(RegExp(r'\s*-\s*Topic$'), '')
      .replaceAll(RegExp(r'\s+'), ' ')
      .trim();
}
