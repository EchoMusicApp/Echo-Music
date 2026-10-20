import 'dart:async';

import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:sqflite/sqflite.dart';

import '../innertube/models/yt_item.dart';
import '../playback/media_metadata.dart';

/// Row models (port of the Room entities in `core/.../db/entities`).
class SongRow {
  final String id;
  final String title;
  final int duration;
  final String? thumbnailUrl;
  final String? albumId;
  final String? albumName;
  final bool explicit;
  final int? year;
  final DateTime? date;
  final bool liked;
  final DateTime? likedDate;
  final int totalPlayTime;
  final DateTime? inLibrary;
  final DateTime? dateDownload;
  final bool isVideo;
  final bool isLocal;
  final String? localPath;
  final int lyricsOffset;

  const SongRow({
    required this.id,
    required this.title,
    this.duration = -1,
    this.thumbnailUrl,
    this.albumId,
    this.albumName,
    this.explicit = false,
    this.year,
    this.date,
    this.liked = false,
    this.likedDate,
    this.totalPlayTime = 0,
    this.inLibrary,
    this.dateDownload,
    this.isVideo = false,
    this.isLocal = false,
    this.localPath,
    this.lyricsOffset = 0,
  });

  static SongRow fromMap(Map<String, Object?> m) => SongRow(
    id: m['id'] as String,
    title: m['title'] as String? ?? '',
    duration: m['duration'] as int? ?? -1,
    thumbnailUrl: m['thumbnailUrl'] as String?,
    albumId: m['albumId'] as String?,
    albumName: m['albumName'] as String?,
    explicit: (m['explicit'] as int? ?? 0) == 1,
    year: m['year'] as int?,
    date: _dt(m['date']),
    liked: (m['liked'] as int? ?? 0) == 1,
    likedDate: _dt(m['likedDate']),
    totalPlayTime: m['totalPlayTime'] as int? ?? 0,
    inLibrary: _dt(m['inLibrary']),
    dateDownload: _dt(m['dateDownload']),
    isVideo: (m['isVideo'] as int? ?? 0) == 1,
    isLocal: (m['isLocal'] as int? ?? 0) == 1,
    localPath: m['localPath'] as String?,
    lyricsOffset: m['lyricsOffset'] as int? ?? 0,
  );

  Map<String, Object?> toMap() => {
    'id': id,
    'title': title,
    'duration': duration,
    'thumbnailUrl': thumbnailUrl,
    'albumId': albumId,
    'albumName': albumName,
    'explicit': explicit ? 1 : 0,
    'year': year,
    'date': date?.millisecondsSinceEpoch,
    'liked': liked ? 1 : 0,
    'likedDate': likedDate?.millisecondsSinceEpoch,
    'totalPlayTime': totalPlayTime,
    'inLibrary': inLibrary?.millisecondsSinceEpoch,
    'dateDownload': dateDownload?.millisecondsSinceEpoch,
    'isVideo': isVideo ? 1 : 0,
    'isLocal': isLocal ? 1 : 0,
    'localPath': localPath,
    'lyricsOffset': lyricsOffset,
  };
}

DateTime? _dt(Object? v) =>
    v is int ? DateTime.fromMillisecondsSinceEpoch(v) : null;

class ArtistRow {
  final String id;
  final String name;
  final String? thumbnailUrl;
  final String? channelId;
  final DateTime? bookmarkedAt;
  final DateTime lastUpdateTime;
  const ArtistRow({
    required this.id,
    required this.name,
    this.thumbnailUrl,
    this.channelId,
    this.bookmarkedAt,
    required this.lastUpdateTime,
  });

  static ArtistRow fromMap(Map<String, Object?> m) => ArtistRow(
    id: m['id'] as String,
    name: m['name'] as String? ?? '',
    thumbnailUrl: m['thumbnailUrl'] as String?,
    channelId: m['channelId'] as String?,
    bookmarkedAt: _dt(m['bookmarkedAt']),
    lastUpdateTime: _dt(m['lastUpdateTime']) ?? DateTime.now(),
  );

  Map<String, Object?> toMap() => {
    'id': id,
    'name': name,
    'thumbnailUrl': thumbnailUrl,
    'channelId': channelId,
    'bookmarkedAt': bookmarkedAt?.millisecondsSinceEpoch,
    'lastUpdateTime': lastUpdateTime.millisecondsSinceEpoch,
  };
}

class AlbumRow {
  final String id;
  final String? playlistId;
  final String title;
  final int? year;
  final String? thumbnailUrl;
  final int songCount;
  final int duration;
  final bool explicit;
  final DateTime? bookmarkedAt;
  final DateTime lastUpdateTime;
  const AlbumRow({
    required this.id,
    this.playlistId,
    required this.title,
    this.year,
    this.thumbnailUrl,
    this.songCount = 0,
    this.duration = 0,
    this.explicit = false,
    this.bookmarkedAt,
    required this.lastUpdateTime,
  });

  static AlbumRow fromMap(Map<String, Object?> m) => AlbumRow(
    id: m['id'] as String,
    playlistId: m['playlistId'] as String?,
    title: m['title'] as String? ?? '',
    year: m['year'] as int?,
    thumbnailUrl: m['thumbnailUrl'] as String?,
    songCount: m['songCount'] as int? ?? 0,
    duration: m['duration'] as int? ?? 0,
    explicit: (m['explicit'] as int? ?? 0) == 1,
    bookmarkedAt: _dt(m['bookmarkedAt']),
    lastUpdateTime: _dt(m['lastUpdateTime']) ?? DateTime.now(),
  );

  Map<String, Object?> toMap() => {
    'id': id,
    'playlistId': playlistId,
    'title': title,
    'year': year,
    'thumbnailUrl': thumbnailUrl,
    'songCount': songCount,
    'duration': duration,
    'explicit': explicit ? 1 : 0,
    'bookmarkedAt': bookmarkedAt?.millisecondsSinceEpoch,
    'lastUpdateTime': lastUpdateTime.millisecondsSinceEpoch,
  };
}

class PlaylistRow {
  static const likedId = 'LP_LIKED';
  static const downloadedId = 'LP_DOWNLOADED';

  final String id;
  final String name;
  final String? browseId;
  final DateTime createdAt;
  final DateTime lastUpdateTime;
  final bool isEditable;
  final DateTime? bookmarkedAt;
  final int? remoteSongCount;
  final String? thumbnailUrl;
  final bool isAutoSync;
  const PlaylistRow({
    required this.id,
    required this.name,
    this.browseId,
    required this.createdAt,
    required this.lastUpdateTime,
    this.isEditable = true,
    this.bookmarkedAt,
    this.remoteSongCount,
    this.thumbnailUrl,
    this.isAutoSync = false,
  });

  static PlaylistRow fromMap(Map<String, Object?> m) => PlaylistRow(
    id: m['id'] as String,
    name: m['name'] as String? ?? '',
    browseId: m['browseId'] as String?,
    createdAt: _dt(m['createdAt']) ?? DateTime.now(),
    lastUpdateTime: _dt(m['lastUpdateTime']) ?? DateTime.now(),
    isEditable: (m['isEditable'] as int? ?? 1) == 1,
    bookmarkedAt: _dt(m['bookmarkedAt']),
    remoteSongCount: m['remoteSongCount'] as int?,
    thumbnailUrl: m['thumbnailUrl'] as String?,
    isAutoSync: (m['isAutoSync'] as int? ?? 0) == 1,
  );

  Map<String, Object?> toMap() => {
    'id': id,
    'name': name,
    'browseId': browseId,
    'createdAt': createdAt.millisecondsSinceEpoch,
    'lastUpdateTime': lastUpdateTime.millisecondsSinceEpoch,
    'isEditable': isEditable ? 1 : 0,
    'bookmarkedAt': bookmarkedAt?.millisecondsSinceEpoch,
    'remoteSongCount': remoteSongCount,
    'thumbnailUrl': thumbnailUrl,
    'isAutoSync': isAutoSync ? 1 : 0,
  };

  static String generateId() {
    const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
    final now = DateTime.now().microsecondsSinceEpoch;
    final sb = StringBuffer('LP');
    var x = now;
    for (var i = 0; i < 8; i++) {
      sb.write(chars[x % chars.length]);
      x = x ~/ 7 + i * 13;
    }
    return sb.toString();
  }
}

/// A song joined with its artists and album (port of `Song` relation).
class Song {
  final SongRow song;
  final List<ArtistRow> artists;
  final AlbumRow? album;
  const Song({required this.song, required this.artists, this.album});

  String get id => song.id;
  String get title => song.title;
  String? get thumbnailUrl => song.thumbnailUrl;
  String get artistsText => artists.map((a) => a.name).join(', ');

  MediaMetadata toMediaMetadata() => MediaMetadata(
    id: song.id,
    title: song.title,
    artists: artists.map((a) => Artist(name: a.name, id: a.id)).toList(),
    duration: song.duration,
    thumbnailUrl: song.thumbnailUrl,
    album: song.albumId != null
        ? Album(name: song.albumName ?? album?.title ?? '', id: song.albumId!)
        : null,
    musicVideoType: song.isVideo ? musicVideoTypeOmv : null,
    explicit: song.explicit,
    isLocal: song.isLocal,
    localPath: song.localPath,
  );

  SongItem toSongItem() => toMediaMetadata().toSongItem();
}

class PlaylistWithInfo {
  final PlaylistRow playlist;
  final int songCount;
  final List<String> thumbnails;
  const PlaylistWithInfo({
    required this.playlist,
    required this.songCount,
    required this.thumbnails,
  });
  String? get thumbnailUrl =>
      playlist.thumbnailUrl ??
      (thumbnails.isNotEmpty ? thumbnails.first : null);
}

class ArtistWithSongCount {
  final ArtistRow artist;
  final int songCount;
  const ArtistWithSongCount(this.artist, this.songCount);
}

class AlbumWithInfo {
  final AlbumRow album;
  final List<ArtistRow> artists;
  const AlbumWithInfo(this.album, this.artists);
}

class LyricsRow {
  final String id;
  final String lyrics;
  final String provider;
  const LyricsRow(this.id, this.lyrics, this.provider);
}

class EventWithSong {
  final int id;
  final DateTime timestamp;
  final int playTime;
  final Song song;
  const EventWithSong({
    required this.id,
    required this.timestamp,
    required this.playTime,
    required this.song,
  });
}

enum SongSortType { createDate, name, artist, playTime }

/// sqflite-backed music database — port of `MusicDatabase` / `DatabaseDao`
/// with a simple change-notification bus for reactive UI.
class AppDatabase {
  AppDatabase._();
  static final AppDatabase instance = AppDatabase._();

  Database? _db;
  final _changes = StreamController<void>.broadcast();
  Stream<void> get changes => _changes.stream;

  Future<Database> get db async {
    if (_db != null) return _db!;
    final dir = await getApplicationSupportDirectory();
    final path = p.join(dir.path, 'echo_music.db');
    _db = await openDatabase(
      path,
      version: 1,
      onCreate: _onCreate,
      onConfigure: (d) async {
        await d.execute('PRAGMA foreign_keys = ON');
      },
    );
    return _db!;
  }

  Future<void> _onCreate(Database d, int version) async {
    await d.execute('''CREATE TABLE song(
      id TEXT PRIMARY KEY, title TEXT NOT NULL, duration INTEGER NOT NULL DEFAULT -1,
      thumbnailUrl TEXT, albumId TEXT, albumName TEXT, explicit INTEGER NOT NULL DEFAULT 0,
      year INTEGER, date INTEGER, liked INTEGER NOT NULL DEFAULT 0, likedDate INTEGER,
      totalPlayTime INTEGER NOT NULL DEFAULT 0, inLibrary INTEGER, dateDownload INTEGER,
      isVideo INTEGER NOT NULL DEFAULT 0, isLocal INTEGER NOT NULL DEFAULT 0, localPath TEXT,
      lyricsOffset INTEGER NOT NULL DEFAULT 0)''');
    await d.execute('CREATE INDEX idx_song_album ON song(albumId)');
    await d.execute('''CREATE TABLE artist(
      id TEXT PRIMARY KEY, name TEXT NOT NULL, thumbnailUrl TEXT, channelId TEXT,
      bookmarkedAt INTEGER, lastUpdateTime INTEGER NOT NULL)''');
    await d.execute(
      '''CREATE TABLE album(
      id TEXT PRIMARY KEY, playlistId TEXT, title TEXT NOT NULL, year INTEGER, thumbnailUrl TEXT,
      songCount INTEGER NOT NULL DEFAULT 0, duration INTEGER NOT NULL DEFAULT 0,
      explicit INTEGER NOT NULL DEFAULT 0, bookmarkedAt INTEGER, lastUpdateTime INTEGER NOT NULL)''',
    );
    await d.execute('''CREATE TABLE song_artist_map(
      songId TEXT NOT NULL REFERENCES song(id) ON DELETE CASCADE,
      artistId TEXT NOT NULL REFERENCES artist(id) ON DELETE CASCADE,
      position INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(songId, artistId))''');
    await d.execute('CREATE INDEX idx_sam_artist ON song_artist_map(artistId)');
    await d.execute('''CREATE TABLE song_album_map(
      songId TEXT NOT NULL REFERENCES song(id) ON DELETE CASCADE,
      albumId TEXT NOT NULL REFERENCES album(id) ON DELETE CASCADE,
      idx INTEGER, PRIMARY KEY(songId, albumId))''');
    await d.execute(
      '''CREATE TABLE playlist(
      id TEXT PRIMARY KEY, name TEXT NOT NULL, browseId TEXT, createdAt INTEGER NOT NULL,
      lastUpdateTime INTEGER NOT NULL, isEditable INTEGER NOT NULL DEFAULT 1, bookmarkedAt INTEGER,
      remoteSongCount INTEGER, thumbnailUrl TEXT, isAutoSync INTEGER NOT NULL DEFAULT 0)''',
    );
    await d.execute('''CREATE TABLE playlist_song_map(
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      playlistId TEXT NOT NULL REFERENCES playlist(id) ON DELETE CASCADE,
      songId TEXT NOT NULL REFERENCES song(id) ON DELETE CASCADE,
      position INTEGER NOT NULL DEFAULT 0, setVideoId TEXT)''');
    await d.execute(
      'CREATE INDEX idx_psm_playlist ON playlist_song_map(playlistId)',
    );
    await d.execute('CREATE INDEX idx_psm_song ON playlist_song_map(songId)');
    await d.execute('''CREATE TABLE event(
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      songId TEXT NOT NULL REFERENCES song(id) ON DELETE CASCADE,
      timestamp INTEGER NOT NULL, playTime INTEGER NOT NULL)''');
    await d.execute('CREATE INDEX idx_event_song ON event(songId)');
    await d.execute('CREATE INDEX idx_event_ts ON event(timestamp)');
    await d.execute(
      '''CREATE TABLE lyrics(
      id TEXT PRIMARY KEY, lyrics TEXT NOT NULL, provider TEXT NOT NULL DEFAULT 'Unknown')''',
    );
    await d.execute('''CREATE TABLE search_history(
      id INTEGER PRIMARY KEY AUTOINCREMENT, query TEXT NOT NULL UNIQUE)''');
    await d.execute(
      '''CREATE TABLE download(
      songId TEXT PRIMARY KEY REFERENCES song(id) ON DELETE CASCADE,
      path TEXT NOT NULL, mimeType TEXT, bytes INTEGER, dateDownload INTEGER NOT NULL)''',
    );
  }

  void _notify() {
    if (!_changes.isClosed) _changes.add(null);
  }

  /// Updates the row with `row['id']` in place, inserting it if missing.
  ///
  /// Use this instead of `ConflictAlgorithm.replace` for tables other tables
  /// reference: REPLACE deletes the old row first, which fires
  /// `ON DELETE CASCADE` and drops the song links.
  Future<void> _upsertRow(
    DatabaseExecutor e,
    String table,
    Map<String, Object?> row,
  ) async {
    final n = await e.update(
      table,
      row,
      where: 'id = ?',
      whereArgs: [row['id']],
    );
    if (n == 0) await e.insert(table, row);
  }

  /// Re-run [query] whenever the database changes (debounced).
  Stream<T> watch<T>(Future<T> Function() query) {
    late StreamController<T> controller;
    StreamSubscription<void>? sub;
    Timer? debounce;
    var running = false;
    var dirty = false;
    Future<void> run() async {
      if (running) {
        dirty = true;
        return;
      }
      running = true;
      try {
        final r = await query();
        if (!controller.isClosed) controller.add(r);
      } catch (e, st) {
        if (!controller.isClosed) controller.addError(e, st);
      } finally {
        running = false;
        if (dirty) {
          dirty = false;
          run();
        }
      }
    }

    controller = StreamController<T>(
      onListen: () {
        run();
        sub = _changes.stream.listen((_) {
          debounce?.cancel();
          debounce = Timer(const Duration(milliseconds: 120), run);
        });
      },
      onCancel: () {
        debounce?.cancel();
        sub?.cancel();
      },
    );
    return controller.stream;
  }

  // ---------------------------------------------------------------------
  // Songs
  // ---------------------------------------------------------------------

  Future<Song?> song(String id) async {
    final d = await db;
    final rows = await d.query('song', where: 'id = ?', whereArgs: [id]);
    if (rows.isEmpty) return null;
    return _hydrate([SongRow.fromMap(rows.first)]).then((l) => l.first);
  }

  Future<List<Song>> songsByIds(List<String> ids) async {
    if (ids.isEmpty) return const [];
    final d = await db;
    final rows = await d.query(
      'song',
      where: 'id IN (${List.filled(ids.length, '?').join(',')})',
      whereArgs: ids,
    );
    final songs = await _hydrate(rows.map(SongRow.fromMap).toList());
    final byId = {for (final s in songs) s.id: s};
    return ids.map((id) => byId[id]).whereType<Song>().toList();
  }

  Future<List<Song>> _hydrate(List<SongRow> rows) async {
    if (rows.isEmpty) return const [];
    final d = await db;
    final ids = rows.map((r) => r.id).toList();
    final placeholders = List.filled(ids.length, '?').join(',');
    final maps = await d.rawQuery('''
      SELECT m.songId, m.position, a.* FROM song_artist_map m
      JOIN artist a ON a.id = m.artistId
      WHERE m.songId IN ($placeholders) ORDER BY m.position''', ids);
    final artistsBySong = <String, List<ArtistRow>>{};
    for (final m in maps) {
      artistsBySong
          .putIfAbsent(m['songId'] as String, () => [])
          .add(ArtistRow.fromMap(m));
    }
    final albumIds = rows
        .map((r) => r.albumId)
        .whereType<String>()
        .toSet()
        .toList();
    final albums = <String, AlbumRow>{};
    if (albumIds.isNotEmpty) {
      final am = await d.query(
        'album',
        where: 'id IN (${List.filled(albumIds.length, '?').join(',')})',
        whereArgs: albumIds,
      );
      for (final m in am) {
        final a = AlbumRow.fromMap(m);
        albums[a.id] = a;
      }
    }
    return rows
        .map(
          (r) => Song(
            song: r,
            artists: artistsBySong[r.id] ?? const [],
            album: r.albumId != null ? albums[r.albumId] : null,
          ),
        )
        .toList();
  }

  String _orderBy(SongSortType sort, bool desc) {
    final dir = desc ? 'DESC' : 'ASC';
    switch (sort) {
      case SongSortType.createDate:
        return 'COALESCE(s.inLibrary, s.likedDate, s.date, 0) $dir';
      case SongSortType.name:
        return 's.title COLLATE NOCASE $dir';
      case SongSortType.artist:
        return 's.title COLLATE NOCASE $dir';
      case SongSortType.playTime:
        return 's.totalPlayTime $dir';
    }
  }

  Future<List<Song>> likedSongs({
    SongSortType sort = SongSortType.createDate,
    bool desc = true,
  }) async {
    final d = await db;
    final order = sort == SongSortType.createDate
        ? 's.likedDate ${desc ? 'DESC' : 'ASC'}'
        : _orderBy(sort, desc);
    final rows = await d.rawQuery(
      'SELECT s.* FROM song s WHERE s.liked = 1 ORDER BY $order',
    );
    return _hydrate(rows.map(SongRow.fromMap).toList());
  }

  Future<List<Song>> librarySongs({
    SongSortType sort = SongSortType.createDate,
    bool desc = true,
  }) async {
    final d = await db;
    final rows = await d.rawQuery(
      'SELECT s.* FROM song s WHERE s.inLibrary IS NOT NULL OR s.liked = 1 OR s.dateDownload IS NOT NULL ORDER BY ${_orderBy(sort, desc)}',
    );
    return _hydrate(rows.map(SongRow.fromMap).toList());
  }

  Future<List<Song>> downloadedSongs() async {
    final d = await db;
    final rows = await d.rawQuery(
      'SELECT s.* FROM song s JOIN download dl ON dl.songId = s.id ORDER BY dl.dateDownload DESC',
    );
    return _hydrate(rows.map(SongRow.fromMap).toList());
  }

  Future<List<Song>> mostPlayedSongs({int limit = 50, DateTime? since}) async {
    final d = await db;
    final rows = since == null
        ? await d.rawQuery(
            'SELECT s.* FROM song s WHERE s.totalPlayTime > 0 ORDER BY s.totalPlayTime DESC LIMIT ?',
            [limit],
          )
        : await d.rawQuery(
            '''
            SELECT s.*, SUM(e.playTime) AS pt FROM song s JOIN event e ON e.songId = s.id
            WHERE e.timestamp > ? GROUP BY s.id ORDER BY pt DESC LIMIT ?''',
            [since.millisecondsSinceEpoch, limit],
          );
    return _hydrate(rows.map(SongRow.fromMap).toList());
  }

  /// Port of `quickPicks`: songs played in the last 30 days ranked by
  /// recency-weighted play time.
  Future<List<Song>> quickPicks({int limit = 20}) async {
    final d = await db;
    final since = DateTime.now()
        .subtract(const Duration(days: 30))
        .millisecondsSinceEpoch;
    final rows = await d.rawQuery(
      '''
      SELECT s.*, SUM(e.playTime) AS pt, MAX(e.timestamp) AS last FROM song s
      JOIN event e ON e.songId = s.id WHERE e.timestamp > ?
      GROUP BY s.id ORDER BY pt DESC LIMIT ?''',
      [since, limit],
    );
    return _hydrate(rows.map(SongRow.fromMap).toList());
  }

  /// Port of `forgottenFavorites`: heavily played songs not heard recently.
  Future<List<Song>> forgottenFavorites({int limit = 20}) async {
    final d = await db;
    final cutoff = DateTime.now()
        .subtract(const Duration(days: 30))
        .millisecondsSinceEpoch;
    final rows = await d.rawQuery(
      '''
      SELECT s.*, MAX(e.timestamp) AS last FROM song s JOIN event e ON e.songId = s.id
      GROUP BY s.id HAVING last < ? AND s.totalPlayTime > 300000
      ORDER BY s.totalPlayTime DESC LIMIT ?''',
      [cutoff, limit],
    );
    return _hydrate(rows.map(SongRow.fromMap).toList());
  }

  Future<List<Song>> recentlyPlayed({int limit = 20}) async {
    final d = await db;
    final rows = await d.rawQuery(
      '''
      SELECT s.*, MAX(e.timestamp) AS last FROM song s JOIN event e ON e.songId = s.id
      GROUP BY s.id ORDER BY last DESC LIMIT ?''',
      [limit],
    );
    return _hydrate(rows.map(SongRow.fromMap).toList());
  }

  Future<List<Song>> searchSongs(String query, {int limit = 50}) async {
    final d = await db;
    final rows = await d.rawQuery(
      '''
      SELECT DISTINCT s.* FROM song s LEFT JOIN song_artist_map m ON m.songId = s.id
      LEFT JOIN artist a ON a.id = m.artistId
      WHERE (s.inLibrary IS NOT NULL OR s.liked = 1 OR s.dateDownload IS NOT NULL OR s.totalPlayTime > 0)
      AND (s.title LIKE ? OR a.name LIKE ? OR s.albumName LIKE ?) LIMIT ?''',
      ['%$query%', '%$query%', '%$query%', limit],
    );
    return _hydrate(rows.map(SongRow.fromMap).toList());
  }

  /// Insert or refresh a song and its artists/album (port of `insert(MediaMetadata)`).
  Future<void> insertSong(MediaMetadata m, {bool markPlayed = false}) async {
    final d = await db;
    await d.transaction((txn) async {
      final existing = await txn.query(
        'song',
        where: 'id = ?',
        whereArgs: [m.id],
      );
      final now = DateTime.now();
      if (existing.isEmpty) {
        await txn.insert(
          'song',
          SongRow(
            id: m.id,
            title: m.title,
            duration: m.duration,
            thumbnailUrl: m.thumbnailUrl,
            albumId: m.album?.id,
            albumName: m.album?.name,
            explicit: m.explicit,
            date: now,
            isVideo: m.isVideoSong,
            isLocal: m.isLocal,
            localPath: m.localPath,
          ).toMap(),
        );
      } else {
        final old = SongRow.fromMap(existing.first);
        await txn.update(
          'song',
          {
            'title': m.title,
            'duration': m.duration > 0 ? m.duration : old.duration,
            'thumbnailUrl': m.thumbnailUrl ?? old.thumbnailUrl,
            'albumId': m.album?.id ?? old.albumId,
            'albumName': m.album?.name ?? old.albumName,
          },
          where: 'id = ?',
          whereArgs: [m.id],
        );
      }
      await txn.delete(
        'song_artist_map',
        where: 'songId = ?',
        whereArgs: [m.id],
      );
      for (var i = 0; i < m.artists.length; i++) {
        final a = m.artists[i];
        final artistId = a.id ?? 'LA${a.name.hashCode.toRadixString(16)}';
        final ex = await txn.query(
          'artist',
          where: 'id = ?',
          whereArgs: [artistId],
        );
        if (ex.isEmpty) {
          await txn.insert(
            'artist',
            ArtistRow(id: artistId, name: a.name, lastUpdateTime: now).toMap(),
          );
        }
        await txn.insert('song_artist_map', {
          'songId': m.id,
          'artistId': artistId,
          'position': i,
        }, conflictAlgorithm: ConflictAlgorithm.replace);
      }
      if (m.album != null && m.album!.id.isNotEmpty) {
        final ex = await txn.query(
          'album',
          where: 'id = ?',
          whereArgs: [m.album!.id],
        );
        if (ex.isEmpty) {
          await txn.insert(
            'album',
            AlbumRow(
              id: m.album!.id,
              title: m.album!.name,
              thumbnailUrl: m.thumbnailUrl,
              lastUpdateTime: now,
            ).toMap(),
          );
        }
        await txn.insert('song_album_map', {
          'songId': m.id,
          'albumId': m.album!.id,
        }, conflictAlgorithm: ConflictAlgorithm.ignore);
      }
    });
    _notify();
  }

  Future<void> upsertAlbum(AlbumItem album, List<SongItem> songs) async {
    final d = await db;
    final now = DateTime.now();
    await d.transaction((txn) async {
      final ex = await txn.query(
        'album',
        where: 'id = ?',
        whereArgs: [album.browseId],
      );
      final old = ex.isEmpty ? null : AlbumRow.fromMap(ex.first);
      await _upsertRow(
        txn,
        'album',
        AlbumRow(
          id: album.browseId,
          playlistId: album.playlistId,
          title: album.title,
          year: album.year,
          thumbnailUrl: album.thumbnail,
          // Library sync saves albums without their tracks; keep what we know.
          songCount: songs.isEmpty ? (old?.songCount ?? 0) : songs.length,
          duration: songs.isEmpty
              ? (old?.duration ?? 0)
              : songs.fold<int>(0, (acc, s) => acc + (s.duration ?? 0)),
          explicit: album.explicit,
          bookmarkedAt: old?.bookmarkedAt,
          lastUpdateTime: now,
        ).toMap(),
      );
    });
    for (var i = 0; i < songs.length; i++) {
      final s = songs[i];
      await insertSong(
        MediaMetadata.fromSongItem(s).copyWith(
          album: Album(name: album.title, id: album.browseId),
          thumbnailUrl: s.thumbnail,
        ),
      );
      await d.update(
        'song_album_map',
        {'idx': i},
        where: 'songId = ? AND albumId = ?',
        whereArgs: [s.id, album.browseId],
      );
    }
    _notify();
  }

  Future<void> toggleLike(MediaMetadata m) async {
    await insertSong(m);
    final d = await db;
    final rows = await d.query(
      'song',
      columns: ['liked'],
      where: 'id = ?',
      whereArgs: [m.id],
    );
    final liked = rows.isNotEmpty && (rows.first['liked'] as int? ?? 0) == 1;
    final now = DateTime.now().millisecondsSinceEpoch;
    await d.update(
      'song',
      {'liked': liked ? 0 : 1, 'likedDate': liked ? null : now},
      where: 'id = ?',
      whereArgs: [m.id],
    );
    _notify();
  }

  Future<void> setLiked(String id, bool liked, {DateTime? likedDate}) async {
    final d = await db;
    await d.update(
      'song',
      {
        'liked': liked ? 1 : 0,
        'likedDate': liked
            ? (likedDate ?? DateTime.now()).millisecondsSinceEpoch
            : null,
      },
      where: 'id = ?',
      whereArgs: [id],
    );
    _notify();
  }

  Future<bool> isLiked(String id) async {
    final d = await db;
    final rows = await d.query(
      'song',
      columns: ['liked'],
      where: 'id = ?',
      whereArgs: [id],
    );
    return rows.isNotEmpty && (rows.first['liked'] as int? ?? 0) == 1;
  }

  Future<void> toggleInLibrary(MediaMetadata m) async {
    await insertSong(m);
    final d = await db;
    final rows = await d.query(
      'song',
      columns: ['inLibrary'],
      where: 'id = ?',
      whereArgs: [m.id],
    );
    final inLib = rows.isNotEmpty && rows.first['inLibrary'] != null;
    await d.update(
      'song',
      {'inLibrary': inLib ? null : DateTime.now().millisecondsSinceEpoch},
      where: 'id = ?',
      whereArgs: [m.id],
    );
    _notify();
  }

  Future<void> setInLibrary(String id, bool inLibrary) async {
    final d = await db;
    await d.update(
      'song',
      {'inLibrary': inLibrary ? DateTime.now().millisecondsSinceEpoch : null},
      where: 'id = ?',
      whereArgs: [id],
    );
    _notify();
  }

  /// Records a play and returns the event id.
  Future<int> addEvent(String songId, int playTimeMs) async {
    final d = await db;
    final id = await d.insert('event', {
      'songId': songId,
      'timestamp': DateTime.now().millisecondsSinceEpoch,
      'playTime': playTimeMs,
    });
    await d.rawUpdate(
      'UPDATE song SET totalPlayTime = totalPlayTime + ? WHERE id = ?',
      [playTimeMs, songId],
    );
    _notify();
    return id;
  }

  /// Sets the final play time of an event created by [addEvent].
  Future<void> finishEvent(int eventId, String songId, int playTimeMs) async {
    final d = await db;
    final rows = await d.query(
      'event',
      columns: ['playTime'],
      where: 'id = ?',
      whereArgs: [eventId],
    );
    if (rows.isEmpty) return; // deleted from history meanwhile
    final previous = rows.first['playTime'] as int? ?? 0;
    await d.update(
      'event',
      {'playTime': playTimeMs},
      where: 'id = ?',
      whereArgs: [eventId],
    );
    await d.rawUpdate(
      'UPDATE song SET totalPlayTime = totalPlayTime + ? WHERE id = ?',
      [playTimeMs - previous, songId],
    );
    _notify();
  }

  Future<List<EventWithSong>> events({int limit = 500}) async {
    final d = await db;
    final rows = await d.rawQuery(
      '''
      SELECT e.id AS eid, e.timestamp AS ets, e.playTime AS ept, s.* FROM event e
      JOIN song s ON s.id = e.songId ORDER BY e.timestamp DESC LIMIT ?''',
      [limit],
    );
    final songs = await _hydrate(rows.map(SongRow.fromMap).toList());
    final byId = {for (final s in songs) s.id: s};
    return rows
        .map(
          (r) => EventWithSong(
            id: r['eid'] as int,
            timestamp: DateTime.fromMillisecondsSinceEpoch(r['ets'] as int),
            playTime: r['ept'] as int,
            song: byId[r['id'] as String]!,
          ),
        )
        .toList();
  }

  Future<void> deleteEvent(int id) async {
    final d = await db;
    await d.delete('event', where: 'id = ?', whereArgs: [id]);
    _notify();
  }

  Future<void> clearHistory() async {
    final d = await db;
    await d.delete('event');
    await d.update('song', {'totalPlayTime': 0});
    _notify();
  }

  Future<void> setLyricsOffset(String id, int offset) async {
    final d = await db;
    await d.update(
      'song',
      {'lyricsOffset': offset},
      where: 'id = ?',
      whereArgs: [id],
    );
    _notify();
  }

  // ---------------------------------------------------------------------
  // Artists & albums
  // ---------------------------------------------------------------------

  Future<ArtistRow?> artist(String id) async {
    final d = await db;
    final rows = await d.query('artist', where: 'id = ?', whereArgs: [id]);
    return rows.isEmpty ? null : ArtistRow.fromMap(rows.first);
  }

  Future<void> upsertArtist(ArtistItem a) async {
    final d = await db;
    final ex = await d.query('artist', where: 'id = ?', whereArgs: [a.id]);
    final old = ex.isEmpty ? null : ArtistRow.fromMap(ex.first);
    await _upsertRow(
      d,
      'artist',
      ArtistRow(
        id: a.id,
        name: a.title,
        thumbnailUrl: a.thumbnail ?? old?.thumbnailUrl,
        channelId: a.channelId ?? old?.channelId,
        bookmarkedAt: old?.bookmarkedAt,
        lastUpdateTime: DateTime.now(),
      ).toMap(),
    );
    _notify();
  }

  Future<void> toggleArtistBookmark(ArtistItem a) async {
    await upsertArtist(a);
    final d = await db;
    final rows = await d.query(
      'artist',
      columns: ['bookmarkedAt'],
      where: 'id = ?',
      whereArgs: [a.id],
    );
    final bookmarked = rows.isNotEmpty && rows.first['bookmarkedAt'] != null;
    await d.update(
      'artist',
      {
        'bookmarkedAt': bookmarked
            ? null
            : DateTime.now().millisecondsSinceEpoch,
      },
      where: 'id = ?',
      whereArgs: [a.id],
    );
    _notify();
  }

  Future<void> setArtistBookmarked(String id, bool v) async {
    final d = await db;
    await d.update(
      'artist',
      {'bookmarkedAt': v ? DateTime.now().millisecondsSinceEpoch : null},
      where: 'id = ?',
      whereArgs: [id],
    );
    _notify();
  }

  Future<List<ArtistWithSongCount>> libraryArtists({
    bool bookmarkedOnly = false,
  }) async {
    final d = await db;
    final rows = await d.rawQuery('''
      SELECT a.*, (SELECT COUNT(*) FROM song_artist_map m JOIN song s ON s.id = m.songId
                   WHERE m.artistId = a.id AND (s.inLibrary IS NOT NULL OR s.liked = 1)) AS cnt
      FROM artist a
      WHERE a.bookmarkedAt IS NOT NULL ${bookmarkedOnly ? '' : 'OR cnt > 0'}
      ORDER BY a.bookmarkedAt DESC, a.name COLLATE NOCASE''');
    return rows
        .map(
          (m) =>
              ArtistWithSongCount(ArtistRow.fromMap(m), m['cnt'] as int? ?? 0),
        )
        .toList();
  }

  Future<List<Song>> artistSongs(String artistId) async {
    final d = await db;
    final rows = await d.rawQuery(
      '''
      SELECT s.* FROM song s JOIN song_artist_map m ON m.songId = s.id
      WHERE m.artistId = ? AND (s.inLibrary IS NOT NULL OR s.liked = 1 OR s.totalPlayTime > 0)
      ORDER BY s.totalPlayTime DESC''',
      [artistId],
    );
    return _hydrate(rows.map(SongRow.fromMap).toList());
  }

  Future<AlbumRow?> album(String id) async {
    final d = await db;
    final rows = await d.query('album', where: 'id = ?', whereArgs: [id]);
    return rows.isEmpty ? null : AlbumRow.fromMap(rows.first);
  }

  Future<void> toggleAlbumBookmark(
    AlbumItem album,
    List<SongItem> songs,
  ) async {
    await upsertAlbum(album, songs);
    final d = await db;
    final rows = await d.query(
      'album',
      columns: ['bookmarkedAt'],
      where: 'id = ?',
      whereArgs: [album.browseId],
    );
    final bookmarked = rows.isNotEmpty && rows.first['bookmarkedAt'] != null;
    await d.update(
      'album',
      {
        'bookmarkedAt': bookmarked
            ? null
            : DateTime.now().millisecondsSinceEpoch,
      },
      where: 'id = ?',
      whereArgs: [album.browseId],
    );
    _notify();
  }

  Future<void> setAlbumBookmarked(String id, bool v) async {
    final d = await db;
    await d.update(
      'album',
      {'bookmarkedAt': v ? DateTime.now().millisecondsSinceEpoch : null},
      where: 'id = ?',
      whereArgs: [id],
    );
    _notify();
  }

  Future<List<AlbumWithInfo>> libraryAlbums() async {
    final d = await db;
    final rows = await d.query(
      'album',
      where: 'bookmarkedAt IS NOT NULL',
      orderBy: 'bookmarkedAt DESC',
    );
    final out = <AlbumWithInfo>[];
    for (final m in rows) {
      final a = AlbumRow.fromMap(m);
      final am = await d.rawQuery(
        '''
        SELECT DISTINCT ar.* FROM artist ar JOIN song_artist_map sam ON sam.artistId = ar.id
        JOIN song_album_map alm ON alm.songId = sam.songId WHERE alm.albumId = ? LIMIT 3''',
        [a.id],
      );
      out.add(AlbumWithInfo(a, am.map(ArtistRow.fromMap).toList()));
    }
    return out;
  }

  Future<List<Song>> albumSongs(String albumId) async {
    final d = await db;
    final rows = await d.rawQuery(
      '''
      SELECT s.* FROM song s JOIN song_album_map m ON m.songId = s.id
      WHERE m.albumId = ? ORDER BY m.idx''',
      [albumId],
    );
    return _hydrate(rows.map(SongRow.fromMap).toList());
  }

  // ---------------------------------------------------------------------
  // Playlists
  // ---------------------------------------------------------------------

  Future<List<PlaylistWithInfo>> playlists() async {
    final d = await db;
    final rows = await d.query('playlist', orderBy: 'lastUpdateTime DESC');
    final out = <PlaylistWithInfo>[];
    for (final m in rows) {
      final pl = PlaylistRow.fromMap(m);
      final cnt =
          Sqflite.firstIntValue(
            await d.rawQuery(
              'SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = ?',
              [pl.id],
            ),
          ) ??
          0;
      final thumbs = await d.rawQuery(
        '''
        SELECT s.thumbnailUrl FROM playlist_song_map m JOIN song s ON s.id = m.songId
        WHERE m.playlistId = ? AND s.thumbnailUrl IS NOT NULL ORDER BY m.position LIMIT 4''',
        [pl.id],
      );
      out.add(
        PlaylistWithInfo(
          playlist: pl,
          songCount: cnt,
          thumbnails: thumbs.map((t) => t['thumbnailUrl'] as String).toList(),
        ),
      );
    }
    return out;
  }

  Future<PlaylistRow?> playlist(String id) async {
    final d = await db;
    final rows = await d.query('playlist', where: 'id = ?', whereArgs: [id]);
    return rows.isEmpty ? null : PlaylistRow.fromMap(rows.first);
  }

  Future<PlaylistRow?> playlistByBrowseId(String browseId) async {
    final d = await db;
    final rows = await d.query(
      'playlist',
      where: 'browseId = ?',
      whereArgs: [browseId],
    );
    return rows.isEmpty ? null : PlaylistRow.fromMap(rows.first);
  }

  Future<PlaylistRow> createPlaylist(
    String name, {
    String? browseId,
    bool isEditable = true,
    String? thumbnailUrl,
  }) async {
    final d = await db;
    final now = DateTime.now();
    final row = PlaylistRow(
      id: PlaylistRow.generateId(),
      name: name,
      browseId: browseId,
      createdAt: now,
      lastUpdateTime: now,
      isEditable: isEditable,
      bookmarkedAt: browseId != null ? now : null,
      thumbnailUrl: thumbnailUrl,
    );
    await d.insert('playlist', row.toMap());
    _notify();
    return row;
  }

  Future<void> renamePlaylist(String id, String name) async {
    final d = await db;
    await d.update(
      'playlist',
      {'name': name, 'lastUpdateTime': DateTime.now().millisecondsSinceEpoch},
      where: 'id = ?',
      whereArgs: [id],
    );
    _notify();
  }

  Future<void> deletePlaylist(String id) async {
    final d = await db;
    await d.delete('playlist', where: 'id = ?', whereArgs: [id]);
    _notify();
  }

  Future<List<Song>> playlistSongs(String playlistId) async {
    final d = await db;
    final rows = await d.rawQuery(
      '''
      SELECT s.*, m.id AS mapId, m.setVideoId AS mapSetVideoId FROM playlist_song_map m
      JOIN song s ON s.id = m.songId WHERE m.playlistId = ? ORDER BY m.position''',
      [playlistId],
    );
    return _hydrate(rows.map(SongRow.fromMap).toList());
  }

  Future<List<Map<String, Object?>>> playlistSongMaps(String playlistId) async {
    final d = await db;
    return d.query(
      'playlist_song_map',
      where: 'playlistId = ?',
      whereArgs: [playlistId],
      orderBy: 'position',
    );
  }

  Future<void> addToPlaylist(
    String playlistId,
    List<MediaMetadata> items, {
    bool allowDuplicates = false,
  }) async {
    final d = await db;
    for (final m in items) {
      await insertSong(m);
    }
    await d.transaction((txn) async {
      var pos =
          Sqflite.firstIntValue(
            await txn.rawQuery(
              'SELECT COALESCE(MAX(position), -1) FROM playlist_song_map WHERE playlistId = ?',
              [playlistId],
            ),
          ) ??
          -1;
      for (final m in items) {
        if (!allowDuplicates) {
          final ex = await txn.query(
            'playlist_song_map',
            where: 'playlistId = ? AND songId = ?',
            whereArgs: [playlistId, m.id],
          );
          if (ex.isNotEmpty) continue;
        }
        pos++;
        await txn.insert('playlist_song_map', {
          'playlistId': playlistId,
          'songId': m.id,
          'position': pos,
          'setVideoId': m.setVideoId,
        });
      }
      await txn.update(
        'playlist',
        {'lastUpdateTime': DateTime.now().millisecondsSinceEpoch},
        where: 'id = ?',
        whereArgs: [playlistId],
      );
    });
    _notify();
  }

  Future<void> removeFromPlaylist(String playlistId, String songId) async {
    final d = await db;
    await d.delete(
      'playlist_song_map',
      where: 'playlistId = ? AND songId = ?',
      whereArgs: [playlistId, songId],
    );
    await _renumber(playlistId);
    _notify();
  }

  Future<void> movePlaylistSong(String playlistId, int from, int to) async {
    final d = await db;
    final maps = await d.query(
      'playlist_song_map',
      where: 'playlistId = ?',
      whereArgs: [playlistId],
      orderBy: 'position',
    );
    if (from < 0 || from >= maps.length || to < 0 || to >= maps.length) return;
    final list = maps.toList();
    final item = list.removeAt(from);
    list.insert(to, item);
    await d.transaction((txn) async {
      for (var i = 0; i < list.length; i++) {
        await txn.update(
          'playlist_song_map',
          {'position': i},
          where: 'id = ?',
          whereArgs: [list[i]['id']],
        );
      }
    });
    _notify();
  }

  Future<void> replacePlaylistSongs(
    String playlistId,
    List<SongItem> songs,
  ) async {
    final d = await db;
    for (final s in songs) {
      await insertSong(MediaMetadata.fromSongItem(s));
    }
    await d.transaction((txn) async {
      await txn.delete(
        'playlist_song_map',
        where: 'playlistId = ?',
        whereArgs: [playlistId],
      );
      for (var i = 0; i < songs.length; i++) {
        await txn.insert('playlist_song_map', {
          'playlistId': playlistId,
          'songId': songs[i].id,
          'position': i,
          'setVideoId': songs[i].setVideoId,
        });
      }
      await txn.update(
        'playlist',
        {
          'lastUpdateTime': DateTime.now().millisecondsSinceEpoch,
          'remoteSongCount': songs.length,
        },
        where: 'id = ?',
        whereArgs: [playlistId],
      );
    });
    _notify();
  }

  Future<void> _renumber(String playlistId) async {
    final d = await db;
    final maps = await d.query(
      'playlist_song_map',
      where: 'playlistId = ?',
      whereArgs: [playlistId],
      orderBy: 'position',
    );
    await d.transaction((txn) async {
      for (var i = 0; i < maps.length; i++) {
        await txn.update(
          'playlist_song_map',
          {'position': i},
          where: 'id = ?',
          whereArgs: [maps[i]['id']],
        );
      }
    });
  }

  // ---------------------------------------------------------------------
  // Lyrics / search history / downloads
  // ---------------------------------------------------------------------

  Future<LyricsRow?> getLyrics(String id) async {
    final d = await db;
    final rows = await d.query('lyrics', where: 'id = ?', whereArgs: [id]);
    if (rows.isEmpty) return null;
    return LyricsRow(
      id,
      rows.first['lyrics'] as String,
      rows.first['provider'] as String? ?? 'Unknown',
    );
  }

  Future<void> putLyrics(String id, String lyrics, String provider) async {
    final d = await db;
    await d.insert('lyrics', {
      'id': id,
      'lyrics': lyrics,
      'provider': provider,
    }, conflictAlgorithm: ConflictAlgorithm.replace);
  }

  Future<void> deleteLyrics(String id) async {
    final d = await db;
    await d.delete('lyrics', where: 'id = ?', whereArgs: [id]);
  }

  Future<List<String>> searchHistory({String? query, int limit = 20}) async {
    final d = await db;
    final rows = query == null || query.isEmpty
        ? await d.query('search_history', orderBy: 'id DESC', limit: limit)
        : await d.query(
            'search_history',
            where: 'query LIKE ?',
            whereArgs: ['%$query%'],
            orderBy: 'id DESC',
            limit: limit,
          );
    return rows.map((r) => r['query'] as String).toList();
  }

  Future<void> addSearchHistory(String query) async {
    final d = await db;
    await d.delete('search_history', where: 'query = ?', whereArgs: [query]);
    await d.insert('search_history', {'query': query});
    _notify();
  }

  Future<void> deleteSearchHistory(String query) async {
    final d = await db;
    await d.delete('search_history', where: 'query = ?', whereArgs: [query]);
    _notify();
  }

  Future<void> clearSearchHistory() async {
    final d = await db;
    await d.delete('search_history');
    _notify();
  }

  Future<void> putDownload(
    String songId,
    String path,
    String? mimeType,
    int bytes,
  ) async {
    final d = await db;
    final now = DateTime.now().millisecondsSinceEpoch;
    await d.insert('download', {
      'songId': songId,
      'path': path,
      'mimeType': mimeType,
      'bytes': bytes,
      'dateDownload': now,
    }, conflictAlgorithm: ConflictAlgorithm.replace);
    await d.update(
      'song',
      {'dateDownload': now},
      where: 'id = ?',
      whereArgs: [songId],
    );
    _notify();
  }

  Future<String?> downloadPath(String songId) async {
    final d = await db;
    final rows = await d.query(
      'download',
      where: 'songId = ?',
      whereArgs: [songId],
    );
    return rows.isEmpty ? null : _downloadFile(rows.first['path'] as String);
  }

  Future<Map<String, String>> allDownloadPaths() async {
    final d = await db;
    final rows = await d.query('download');
    return {
      for (final r in rows)
        r['songId'] as String: await _downloadFile(r['path'] as String),
    };
  }

  /// Downloads are stored by file name, because the iOS app container moves
  /// on updates. `basename` also handles rows saved with a full path.
  Future<String> _downloadFile(String stored) async {
    final base = await getApplicationDocumentsDirectory();
    return p.join(base.path, 'downloads', p.basename(stored));
  }

  Future<void> removeDownload(String songId) async {
    final d = await db;
    await d.delete('download', where: 'songId = ?', whereArgs: [songId]);
    await d.update(
      'song',
      {'dateDownload': null},
      where: 'id = ?',
      whereArgs: [songId],
    );
    _notify();
  }

  Future<int> totalDownloadBytes() async {
    final d = await db;
    return Sqflite.firstIntValue(
          await d.rawQuery('SELECT COALESCE(SUM(bytes),0) FROM download'),
        ) ??
        0;
  }

  Future<Map<String, int>> stats() async {
    final d = await db;
    final songs =
        Sqflite.firstIntValue(
          await d.rawQuery('SELECT COUNT(*) FROM song WHERE totalPlayTime > 0'),
        ) ??
        0;
    final artists =
        Sqflite.firstIntValue(
          await d.rawQuery(
            'SELECT COUNT(DISTINCT artistId) FROM song_artist_map m JOIN song s ON s.id = m.songId WHERE s.totalPlayTime > 0',
          ),
        ) ??
        0;
    final time =
        Sqflite.firstIntValue(
          await d.rawQuery('SELECT COALESCE(SUM(playTime),0) FROM event'),
        ) ??
        0;
    return {'songs': songs, 'artists': artists, 'playTimeMs': time};
  }

  Future<List<ArtistWithSongCount>> mostPlayedArtists({
    int limit = 20,
    DateTime? since,
  }) async {
    final d = await db;
    final rows = await d.rawQuery(
      '''
      SELECT a.*, SUM(e.playTime) AS pt FROM artist a JOIN song_artist_map m ON m.artistId = a.id
      JOIN event e ON e.songId = m.songId ${since != null ? 'WHERE e.timestamp > ${since.millisecondsSinceEpoch}' : ''}
      GROUP BY a.id ORDER BY pt DESC LIMIT ?''',
      [limit],
    );
    return rows
        .map(
          (m) => ArtistWithSongCount(
            ArtistRow.fromMap(m),
            (m['pt'] as int? ?? 0) ~/ 1000,
          ),
        )
        .toList();
  }
}
