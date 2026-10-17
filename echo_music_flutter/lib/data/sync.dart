import 'package:flutter/foundation.dart';

import '../innertube/models/yt_item.dart';
import '../innertube/youtube.dart';
import '../playback/media_metadata.dart';
import 'database.dart';
import 'settings.dart';

/// Pulls the signed-in user's YouTube Music library into the local
/// database (port of the essential parts of `SyncUtils.kt`).
class SyncManager extends ChangeNotifier {
  SyncManager._();
  static final SyncManager instance = SyncManager._();

  bool syncing = false;
  String? status;
  DateTime? lastSync;

  Future<void> syncAll() async {
    if (syncing ||
        !Settings.instance.isLoggedIn ||
        !Settings.instance.ytmSync) {
      return;
    }
    syncing = true;
    notifyListeners();
    try {
      await _step('Syncing liked songs…', syncLikedSongs);
      await _step('Syncing albums…', syncLikedAlbums);
      await _step('Syncing artists…', syncArtists);
      await _step('Syncing playlists…', syncSavedPlaylists);
      lastSync = DateTime.now();
      status = 'Library synced';
    } catch (e) {
      status = 'Sync failed: $e';
      debugPrint('sync failed: $e');
    } finally {
      syncing = false;
      notifyListeners();
    }
  }

  Future<void> _step(String label, Future<void> Function() fn) async {
    status = label;
    notifyListeners();
    try {
      await fn();
    } catch (e) {
      debugPrint('$label failed: $e');
    }
  }

  Future<void> syncLikedSongs() async {
    final db = AppDatabase.instance;
    final page = await YouTube.instance.playlistCompleted('LM');
    final remoteIds = page.songs.map((s) => s.id).toSet();
    final local = await db.likedSongs();
    for (final s in page.songs.reversed) {
      final m = s;
      await db.insertSong(MediaMetadata.fromSongItem(m));
      final row = await db.song(m.id);
      if (row?.song.liked != true) await db.setLiked(m.id, true);
    }
    for (final s in local) {
      if (!remoteIds.contains(s.id)) await db.setLiked(s.id, false);
    }
  }

  Future<void> syncLikedAlbums() async {
    final db = AppDatabase.instance;
    final page = await YouTube.instance.libraryCompleted(
      'FEmusic_liked_albums',
    );
    final remote = page.items.whereType<AlbumItem>().toList();
    final remoteIds = remote.map((a) => a.browseId).toSet();
    for (final a in remote) {
      final existing = await db.album(a.browseId);
      if (existing?.bookmarkedAt != null) continue;
      try {
        final full = await YouTube.instance.album(a.browseId);
        await db.upsertAlbum(full.album, full.songs);
      } catch (_) {
        await db.upsertAlbum(a, const []);
      }
      await db.setAlbumBookmarked(a.browseId, true);
    }
    for (final a in await db.libraryAlbums()) {
      if (!remoteIds.contains(a.album.id)) {
        await db.setAlbumBookmarked(a.album.id, false);
      }
    }
  }

  Future<void> syncArtists() async {
    final db = AppDatabase.instance;
    final page = await YouTube.instance.libraryCompleted(
      'FEmusic_library_corpus_artists',
    );
    final remote = page.items.whereType<ArtistItem>().toList();
    final remoteIds = remote.map((a) => a.id).toSet();
    for (final a in remote) {
      await db.upsertArtist(a);
      await db.setArtistBookmarked(a.id, true);
    }
    for (final a in await db.libraryArtists(bookmarkedOnly: true)) {
      if (!remoteIds.contains(a.artist.id)) {
        await db.setArtistBookmarked(a.artist.id, false);
      }
    }
  }

  Future<void> syncSavedPlaylists() async {
    final db = AppDatabase.instance;
    final page = await YouTube.instance.libraryCompleted(
      'FEmusic_liked_playlists',
    );
    final remote = page.items
        .whereType<PlaylistItem>()
        .where((p) => p.id != 'LM' && p.id != 'SE')
        .toList();
    final remoteIds = remote.map((p) => p.id).toSet();
    for (final p in remote) {
      final existing = await db.playlistByBrowseId(p.id);
      if (existing != null) continue;
      await db.createPlaylist(
        p.title,
        browseId: p.id,
        isEditable: p.isEditable,
        thumbnailUrl: p.thumbnail,
      );
    }
    for (final pl in await db.playlists()) {
      final b = pl.playlist.browseId;
      if (b != null && !remoteIds.contains(b)) {
        await db.deletePlaylist(pl.playlist.id);
      }
    }
  }
}
