import 'package:flutter/material.dart';
import 'package:share_plus/share_plus.dart';

import '../../core/utils.dart';
import '../../data/database.dart';
import '../../data/download_manager.dart';
import '../../data/settings.dart';
import '../../innertube/models/yt_item.dart';
import '../../innertube/youtube.dart';
import '../../playback/media_metadata.dart';
import '../../playback/player_controller.dart';
import '../shell/app_navigator.dart';
import 'thumbnail.dart';

/// Bottom-sheet context menus (port of `ui/menu/*Menu.kt`).

class _MenuAction {
  final IconData icon;
  final String label;
  final Future<void> Function() onTap;
  final bool destructive;
  const _MenuAction(
    this.icon,
    this.label,
    this.onTap, {
    this.destructive = false,
  });
}

Future<void> _showMenu(
  BuildContext context, {
  required String title,
  required String subtitle,
  required String? thumbnail,
  bool circle = false,
  required List<_MenuAction> actions,
  List<Widget> header = const [],
}) {
  return showModalBottomSheet<void>(
    context: context,
    useRootNavigator: true,
    isScrollControlled: true,
    builder: (sheetContext) {
      final theme = Theme.of(sheetContext);
      return SafeArea(
        child: ConstrainedBox(
          constraints: BoxConstraints(
            maxHeight: MediaQuery.sizeOf(sheetContext).height * 0.85,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 4, 20, 12),
                child: Row(
                  children: [
                    EchoImage(
                      url: thumbnail,
                      width: 56,
                      height: 56,
                      radius: 10,
                      circle: circle,
                      resize: 224,
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            title,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: theme.textTheme.titleMedium?.copyWith(
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                          Text(
                            subtitle,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: theme.textTheme.bodyMedium?.copyWith(
                              color: theme.colorScheme.onSurfaceVariant,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              ...header,
              const Divider(),
              Flexible(
                child: ListView(
                  shrinkWrap: true,
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  children: [
                    for (final a in actions)
                      ListTile(
                        leading: Icon(
                          a.icon,
                          color: a.destructive ? theme.colorScheme.error : null,
                        ),
                        title: Text(
                          a.label,
                          style: a.destructive
                              ? TextStyle(color: theme.colorScheme.error)
                              : null,
                        ),
                        onTap: () async {
                          Navigator.of(sheetContext).pop();
                          await a.onTap();
                        },
                      ),
                  ],
                ),
              ),
            ],
          ),
        ),
      );
    },
  );
}

Future<void> showSongMenu(
  BuildContext context,
  SongItem song, {
  Song? local,
  String? playlistId,
  int? eventId,
}) async {
  final db = AppDatabase.instance;
  final meta = local?.toMediaMetadata() ?? MediaMetadata.fromSongItem(song);
  final liked = await db.isLiked(song.id);
  final downloaded = DownloadManager.instance.isDownloaded(song.id);
  final inLibrary =
      local?.song.inLibrary != null ||
      ((await db.song(song.id))?.song.inLibrary != null);
  if (!context.mounted) return;
  final actions = <_MenuAction>[
    _MenuAction(
      Icons.playlist_play_rounded,
      'Play next',
      () => player.playNext([meta]),
    ),
    _MenuAction(
      Icons.queue_music_rounded,
      'Add to queue',
      () => player.addToQueue([meta]),
    ),
    if (!meta.isLocal)
      _MenuAction(
        Icons.radio_rounded,
        'Start radio',
        () => player.playRadio(meta),
      ),
    _MenuAction(
      liked ? Icons.favorite_rounded : Icons.favorite_border_rounded,
      liked ? 'Remove from liked' : 'Like',
      () => player.toggleLike(meta),
    ),
    _MenuAction(
      inLibrary ? Icons.bookmark_remove_rounded : Icons.bookmark_add_outlined,
      inLibrary ? 'Remove from library' : 'Add to library',
      () async {
        await db.toggleInLibrary(meta);
        if (Settings.instance.isLoggedIn && Settings.instance.ytmSync) {
          try {
            await YouTube.instance.toggleSongLibrary(song.id, !inLibrary);
          } catch (_) {}
        }
      },
    ),
    _MenuAction(
      Icons.playlist_add_rounded,
      'Add to playlist',
      () => showAddToPlaylistSheet(context, [meta]),
    ),
    if (!meta.isLocal)
      _MenuAction(
        downloaded ? Icons.delete_outline_rounded : Icons.download_rounded,
        downloaded ? 'Remove download' : 'Download',
        () async {
          if (downloaded) {
            await DownloadManager.instance.remove(song.id);
          } else {
            await DownloadManager.instance.download(meta);
            if (context.mounted) {
              showSnack(context, 'Downloading "${song.title}"');
            }
          }
        },
      ),
    for (final a in song.artists.where((a) => a.id != null && a.id!.isNotEmpty))
      _MenuAction(
        Icons.person_rounded,
        'View artist: ${a.name}',
        () async => AppNavigator.openArtist(a.id!),
      ),
    if (song.album != null && song.album!.id.isNotEmpty)
      _MenuAction(
        Icons.album_rounded,
        'View album',
        () async => AppNavigator.openAlbum(song.album!.id),
      ),
    if (playlistId != null)
      _MenuAction(
        Icons.playlist_remove_rounded,
        'Remove from playlist',
        () async {
          await db.removeFromPlaylist(playlistId, song.id);
          final pl = await db.playlist(playlistId);
          if (pl?.browseId != null &&
              Settings.instance.isLoggedIn &&
              song.setVideoId != null) {
            try {
              await YouTube.instance.removeFromPlaylist(
                pl!.browseId!,
                song.id,
                song.setVideoId!,
              );
            } catch (_) {}
          }
        },
        destructive: true,
      ),
    if (eventId != null)
      _MenuAction(
        Icons.delete_sweep_rounded,
        'Remove from history',
        () => db.deleteEvent(eventId),
        destructive: true,
      ),
    if (!meta.isLocal)
      _MenuAction(
        Icons.share_rounded,
        'Share',
        () => SharePlus.instance.share(
          ShareParams(uri: Uri.parse(song.shareLink)),
        ),
      ),
  ];
  await _showMenu(
    context,
    title: song.title,
    subtitle: joinByBullet([
      song.artistsText,
      if (song.album != null) song.album!.name,
    ]),
    thumbnail: song.thumbnail,
    actions: actions,
  );
}

Future<void> showAlbumMenu(BuildContext context, AlbumItem album) async {
  final db = AppDatabase.instance;
  final row = await db.album(album.browseId);
  final saved = row?.bookmarkedAt != null;
  if (!context.mounted) return;
  Future<List<MediaMetadata>> songs() async =>
      (await YouTube.instance.albumSongs(
        album.playlistId,
        album: album,
      )).map(MediaMetadata.fromSongItem).toList();
  await _showMenu(
    context,
    title: album.title,
    subtitle: joinByBullet([
      'Album',
      album.artistsText,
      if (album.year != null) '${album.year}',
    ]),
    thumbnail: album.thumbnail,
    actions: [
      _MenuAction(
        Icons.play_arrow_rounded,
        'Play',
        () => player.playAlbum(album),
      ),
      _MenuAction(
        Icons.shuffle_rounded,
        'Shuffle',
        () => player.playAlbum(album, shuffle: true),
      ),
      _MenuAction(
        Icons.playlist_play_rounded,
        'Play next',
        () async => player.playNext(await songs()),
      ),
      _MenuAction(
        Icons.queue_music_rounded,
        'Add to queue',
        () async => player.addToQueue(await songs()),
      ),
      _MenuAction(
        saved ? Icons.bookmark_remove_rounded : Icons.bookmark_add_outlined,
        saved ? 'Remove from library' : 'Save to library',
        () async {
          final list = await YouTube.instance.albumSongs(
            album.playlistId,
            album: album,
          );
          await db.toggleAlbumBookmark(album, list);
          if (Settings.instance.isLoggedIn && Settings.instance.ytmSync) {
            try {
              await YouTube.instance.likePlaylist(album.playlistId, !saved);
            } catch (_) {}
          }
        },
      ),
      _MenuAction(
        Icons.playlist_add_rounded,
        'Add to playlist',
        () async => showAddToPlaylistSheet(context, await songs()),
      ),
      _MenuAction(
        Icons.download_rounded,
        'Download',
        () async => DownloadManager.instance.downloadAll(await songs()),
      ),
      for (final a in (album.artists ?? const <Artist>[]).where(
        (a) => a.id != null,
      ))
        _MenuAction(
          Icons.person_rounded,
          'View artist: ${a.name}',
          () async => AppNavigator.openArtist(a.id!),
        ),
      _MenuAction(
        Icons.share_rounded,
        'Share',
        () => SharePlus.instance.share(
          ShareParams(uri: Uri.parse(album.shareLink)),
        ),
      ),
    ],
  );
}

Future<void> showArtistMenu(BuildContext context, ArtistItem artist) async {
  final db = AppDatabase.instance;
  final row = await db.artist(artist.id);
  final subscribed = row?.bookmarkedAt != null;
  if (!context.mounted) return;
  await _showMenu(
    context,
    title: artist.title,
    subtitle: 'Artist',
    thumbnail: artist.thumbnail,
    circle: true,
    actions: [
      if (artist.shuffleEndpoint != null || artist.playEndpoint != null)
        _MenuAction(
          Icons.shuffle_rounded,
          'Shuffle',
          () => player.playArtist(artist, shuffle: true),
        ),
      if (artist.radioEndpoint != null)
        _MenuAction(
          Icons.radio_rounded,
          'Start radio',
          () => player.playArtist(artist, radio: true),
        ),
      _MenuAction(
        subscribed
            ? Icons.bookmark_remove_rounded
            : Icons.bookmark_add_outlined,
        subscribed ? 'Unsubscribe' : 'Subscribe',
        () async {
          await db.toggleArtistBookmark(artist);
          if (Settings.instance.isLoggedIn && Settings.instance.ytmSync) {
            try {
              final channelId =
                  artist.channelId ??
                  (await YouTube.instance.artist(artist.id)).artist.channelId;
              if (channelId != null) {
                await YouTube.instance.subscribeChannel(channelId, !subscribed);
              }
            } catch (_) {}
          }
        },
      ),
      _MenuAction(
        Icons.share_rounded,
        'Share',
        () => SharePlus.instance.share(
          ShareParams(uri: Uri.parse(artist.shareLink)),
        ),
      ),
    ],
  );
}

Future<void> showPlaylistMenu(
  BuildContext context,
  PlaylistItem playlist,
) async {
  final db = AppDatabase.instance;
  final row = await db.playlistByBrowseId(playlist.id);
  final saved = row != null;
  if (!context.mounted) return;
  Future<List<MediaMetadata>> songs() async =>
      (await YouTube.instance.playlistCompleted(playlist.id)).songs
          .map(MediaMetadata.fromSongItem)
          .toList();
  await _showMenu(
    context,
    title: playlist.title,
    subtitle: joinByBullet([
      'Playlist',
      playlist.author?.name,
      playlist.songCountText,
    ]),
    thumbnail: playlist.thumbnail,
    actions: [
      _MenuAction(
        Icons.play_arrow_rounded,
        'Play',
        () => player.playPlaylist(playlist),
      ),
      _MenuAction(
        Icons.shuffle_rounded,
        'Shuffle',
        () => player.playPlaylist(playlist, shuffle: true),
      ),
      if (playlist.radioEndpoint != null)
        _MenuAction(
          Icons.radio_rounded,
          'Start radio',
          () => player.playPlaylist(playlist, radio: true),
        ),
      _MenuAction(
        Icons.playlist_play_rounded,
        'Play next',
        () async => player.playNext(await songs()),
      ),
      _MenuAction(
        Icons.queue_music_rounded,
        'Add to queue',
        () async => player.addToQueue(await songs()),
      ),
      _MenuAction(
        saved ? Icons.bookmark_remove_rounded : Icons.bookmark_add_outlined,
        saved ? 'Remove from library' : 'Save to library',
        () async {
          if (saved) {
            await db.deletePlaylist(row.id);
          } else {
            final page = await YouTube.instance.playlistCompleted(playlist.id);
            final pl = await db.createPlaylist(
              playlist.title,
              browseId: playlist.id,
              isEditable: playlist.isEditable,
              thumbnailUrl: playlist.thumbnail,
            );
            await db.replacePlaylistSongs(pl.id, page.songs);
          }
          if (Settings.instance.isLoggedIn && Settings.instance.ytmSync) {
            try {
              await YouTube.instance.likePlaylist(playlist.id, !saved);
            } catch (_) {}
          }
        },
      ),
      _MenuAction(
        Icons.download_rounded,
        'Download',
        () async => DownloadManager.instance.downloadAll(await songs()),
      ),
      _MenuAction(
        Icons.share_rounded,
        'Share',
        () => SharePlus.instance.share(
          ShareParams(uri: Uri.parse(playlist.shareLink)),
        ),
      ),
    ],
  );
}

Future<void> showLocalPlaylistMenu(
  BuildContext context,
  PlaylistWithInfo info,
) async {
  final db = AppDatabase.instance;
  Future<List<MediaMetadata>> songs() async =>
      (await db.playlistSongs(info.playlist.id))
          .map((s) => s.toMediaMetadata())
          .toList();
  await _showMenu(
    context,
    title: info.playlist.name,
    subtitle: '${info.songCount} songs',
    thumbnail: info.thumbnailUrl,
    actions: [
      _MenuAction(
        Icons.play_arrow_rounded,
        'Play',
        () async =>
            player.playMetadataList(await songs(), title: info.playlist.name),
      ),
      _MenuAction(
        Icons.shuffle_rounded,
        'Shuffle',
        () async => player.playMetadataList(
          await songs(),
          title: info.playlist.name,
          shuffle: true,
        ),
      ),
      _MenuAction(
        Icons.playlist_play_rounded,
        'Play next',
        () async => player.playNext(await songs()),
      ),
      _MenuAction(
        Icons.queue_music_rounded,
        'Add to queue',
        () async => player.addToQueue(await songs()),
      ),
      _MenuAction(
        Icons.download_rounded,
        'Download all',
        () async => DownloadManager.instance.downloadAll(await songs()),
      ),
      if (info.playlist.browseId != null)
        _MenuAction(Icons.sync_rounded, 'Sync from YouTube Music', () async {
          final page = await YouTube.instance.playlistCompleted(
            info.playlist.browseId!,
          );
          await db.replacePlaylistSongs(info.playlist.id, page.songs);
        }),
      _MenuAction(Icons.edit_rounded, 'Rename', () async {
        final name = await showTextInputDialog(
          context,
          title: 'Rename playlist',
          initial: info.playlist.name,
        );
        if (name != null && name.trim().isNotEmpty) {
          await db.renamePlaylist(info.playlist.id, name.trim());
          if (info.playlist.browseId != null && Settings.instance.isLoggedIn) {
            try {
              await YouTube.instance.renamePlaylist(
                info.playlist.browseId!,
                name.trim(),
              );
            } catch (_) {}
          }
        }
      }),
      _MenuAction(Icons.delete_outline_rounded, 'Delete', () async {
        final ok = await showConfirmDialog(
          context,
          'Delete "${info.playlist.name}"?',
        );
        if (ok) {
          await db.deletePlaylist(info.playlist.id);
          if (info.playlist.browseId != null &&
              Settings.instance.isLoggedIn &&
              info.playlist.isEditable) {
            try {
              await YouTube.instance.deletePlaylist(info.playlist.browseId!);
            } catch (_) {}
          }
        }
      }, destructive: true),
    ],
  );
}

Future<void> showAddToPlaylistSheet(
  BuildContext context,
  List<MediaMetadata> items,
) async {
  final db = AppDatabase.instance;
  final playlists = (await db.playlists())
      .where((p) => p.playlist.isEditable)
      .toList();
  if (!context.mounted) return;
  await showModalBottomSheet<void>(
    context: context,
    useRootNavigator: true,
    builder: (sheetContext) {
      final theme = Theme.of(sheetContext);
      return SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 0, 20, 8),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      'Add to playlist',
                      style: theme.textTheme.titleLarge,
                    ),
                  ),
                  TextButton.icon(
                    onPressed: () async {
                      final name = await showTextInputDialog(
                        sheetContext,
                        title: 'Create playlist',
                      );
                      if (name == null || name.trim().isEmpty) return;
                      final pl = await db.createPlaylist(name.trim());
                      await db.addToPlaylist(pl.id, items);
                      if (sheetContext.mounted) {
                        Navigator.of(sheetContext).pop();
                      }
                    },
                    icon: const Icon(Icons.add_rounded),
                    label: const Text('New'),
                  ),
                ],
              ),
            ),
            Flexible(
              child: playlists.isEmpty
                  ? const Padding(
                      padding: EdgeInsets.all(24),
                      child: Text('No playlists yet'),
                    )
                  : ListView.builder(
                      shrinkWrap: true,
                      itemCount: playlists.length,
                      itemBuilder: (_, i) {
                        final p = playlists[i];
                        return ListTile(
                          leading: EchoImage(
                            url: p.thumbnailUrl,
                            width: 44,
                            height: 44,
                            radius: 8,
                            resize: 224,
                          ),
                          title: Text(p.playlist.name),
                          subtitle: Text('${p.songCount} songs'),
                          onTap: () async {
                            await db.addToPlaylist(p.playlist.id, items);
                            if (p.playlist.browseId != null &&
                                Settings.instance.isLoggedIn &&
                                Settings.instance.ytmSync) {
                              for (final m in items) {
                                try {
                                  await YouTube.instance.addToPlaylist(
                                    p.playlist.browseId!,
                                    m.id,
                                  );
                                } catch (_) {}
                              }
                            }
                            if (sheetContext.mounted) {
                              Navigator.of(sheetContext).pop();
                            }
                            if (context.mounted) {
                              showSnack(context, 'Added to ${p.playlist.name}');
                            }
                          },
                        );
                      },
                    ),
            ),
          ],
        ),
      );
    },
  );
}

Future<String?> showTextInputDialog(
  BuildContext context, {
  required String title,
  String? initial,
  String hint = 'Name',
}) {
  final controller = TextEditingController(text: initial);
  return showDialog<String>(
    context: context,
    useRootNavigator: true,
    builder: (ctx) => AlertDialog(
      title: Text(title),
      content: TextField(
        controller: controller,
        autofocus: true,
        decoration: InputDecoration(hintText: hint),
        onSubmitted: (v) => Navigator.of(ctx).pop(v),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(ctx).pop(),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: () => Navigator.of(ctx).pop(controller.text),
          child: const Text('OK'),
        ),
      ],
    ),
  );
}

Future<bool> showConfirmDialog(
  BuildContext context,
  String message, {
  String confirm = 'Delete',
}) async {
  final r = await showDialog<bool>(
    context: context,
    useRootNavigator: true,
    builder: (ctx) => AlertDialog(
      content: Text(message),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(ctx).pop(false),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: () => Navigator.of(ctx).pop(true),
          child: Text(confirm),
        ),
      ],
    ),
  );
  return r ?? false;
}
