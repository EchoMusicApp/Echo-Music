import 'package:flutter/material.dart';

import '../../core/utils.dart';
import '../../data/database.dart';
import '../../data/download_manager.dart';
import '../../innertube/youtube.dart';
import '../../playback/player_controller.dart';
import '../components/common.dart';
import '../components/items.dart';
import '../components/menus.dart';
import 'library_screen.dart';
import '../components/watch_builder.dart';

/// Local playlists: custom, liked, downloaded, and top 50
/// (port of `LocalPlaylistScreen.kt` / `AutoPlaylistScreen.kt` / `TopPlaylistScreen.kt`).
class LocalPlaylistScreen extends StatefulWidget {
  static const likedId = PlaylistRow.likedId;
  static const downloadedId = PlaylistRow.downloadedId;
  static const topId = 'LP_TOP';

  final String playlistId;
  const LocalPlaylistScreen({super.key, required this.playlistId});

  @override
  State<LocalPlaylistScreen> createState() => _LocalPlaylistScreenState();
}

class _LocalPlaylistScreenState extends State<LocalPlaylistScreen> {
  bool _syncing = false;

  bool get _isLiked => widget.playlistId == LocalPlaylistScreen.likedId;
  bool get _isDownloaded =>
      widget.playlistId == LocalPlaylistScreen.downloadedId;
  bool get _isTop => widget.playlistId == LocalPlaylistScreen.topId;
  bool get _isSpecial => _isLiked || _isDownloaded || _isTop;

  Future<List<Song>> _songs() {
    final db = AppDatabase.instance;
    if (_isLiked) return db.likedSongs();
    if (_isDownloaded) return db.downloadedSongs();
    if (_isTop) return db.mostPlayedSongs(limit: 50);
    return db.playlistSongs(widget.playlistId);
  }

  @override
  void initState() {
    super.initState();
    _maybeSyncRemote();
  }

  Future<void> _maybeSyncRemote() async {
    if (_isSpecial) return;
    final db = AppDatabase.instance;
    final pl = await db.playlist(widget.playlistId);
    if (pl?.browseId == null) return;
    final songs = await db.playlistSongs(widget.playlistId);
    if (songs.isNotEmpty) return;
    await _syncRemote(pl!);
  }

  Future<void> _syncRemote(PlaylistRow pl) async {
    if (_syncing || !mounted) return;
    setState(() => _syncing = true);
    try {
      final page = await YouTube.instance.playlistCompleted(pl.browseId!);
      await AppDatabase.instance.replacePlaylistSongs(pl.id, page.songs);
    } catch (e) {
      if (mounted) showSnack(context, 'Sync failed: $e');
    } finally {
      if (mounted) setState(() => _syncing = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final db = AppDatabase.instance;
    final theme = Theme.of(context);
    return WatchBuilder<PlaylistRow?>(
      watchKey: widget.playlistId,
      query: () async => _isSpecial ? null : db.playlist(widget.playlistId),
      builder: (context, pl) {
        final title = _isLiked
            ? 'Liked songs'
            : _isDownloaded
            ? 'Downloaded'
            : _isTop
            ? 'My top 50'
            : pl?.name ?? '';
        return WatchBuilder<List<Song>>(
          watchKey: widget.playlistId,
          query: _songs,
          builder: (context, data) {
            final songs = data ?? const <Song>[];
            final duration = songs.fold<int>(
              0,
              (a, s) => a + (s.song.duration > 0 ? s.song.duration : 0),
            );
            final editable = pl != null && pl.isEditable && !_isSpecial;
            return Scaffold(
              body: CustomScrollView(
                slivers: [
                  SliverAppBar(
                    pinned: true,
                    title: Text(title),
                    actions: [
                      if (pl?.browseId != null)
                        IconButton(
                          onPressed: _syncing ? null : () => _syncRemote(pl!),
                          icon: _syncing
                              ? const SizedBox(
                                  width: 20,
                                  height: 20,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2,
                                  ),
                                )
                              : const Icon(Icons.sync_rounded),
                        ),
                      if (pl != null)
                        IconButton(
                          onPressed: () async {
                            final info = PlaylistWithInfo(
                              playlist: pl,
                              songCount: songs.length,
                              thumbnails: songs
                                  .map((s) => s.thumbnailUrl)
                                  .whereType<String>()
                                  .take(4)
                                  .toList(),
                            );
                            await showLocalPlaylistMenu(context, info);
                            if (context.mounted &&
                                await db.playlist(pl.id) == null &&
                                context.mounted) {
                              Navigator.of(context).pop();
                            }
                          },
                          icon: const Icon(Icons.more_vert_rounded),
                        ),
                    ],
                  ),
                  SliverToBoxAdapter(
                    child: Padding(
                      padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              PlaylistMosaic(
                                thumbnails: pl?.thumbnailUrl != null
                                    ? [pl!.thumbnailUrl!]
                                    : songs
                                          .map((s) => s.thumbnailUrl)
                                          .whereType<String>()
                                          .take(4)
                                          .toList(),
                                size: 144,
                                icon: _isLiked
                                    ? Icons.favorite_rounded
                                    : _isDownloaded
                                    ? Icons.offline_pin_rounded
                                    : _isTop
                                    ? Icons.trending_up_rounded
                                    : null,
                              ),
                              const SizedBox(width: 16),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      title,
                                      style: theme.textTheme.titleLarge
                                          ?.copyWith(
                                            fontWeight: FontWeight.w800,
                                          ),
                                      maxLines: 3,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                    const SizedBox(height: 4),
                                    Text(
                                      joinByBullet([
                                        '${songs.length} songs',
                                        if (duration > 0)
                                          makeTimeString(duration),
                                        if (pl?.browseId != null)
                                          'YouTube Music',
                                      ]),
                                      style: theme.textTheme.bodyMedium
                                          ?.copyWith(
                                            color: theme
                                                .colorScheme
                                                .onSurfaceVariant,
                                          ),
                                    ),
                                    const SizedBox(height: 12),
                                    Row(
                                      children: [
                                        if (!_isDownloaded)
                                          RoundIconButton(
                                            icon: Icons.download_rounded,
                                            size: 40,
                                            onPressed: songs.isEmpty
                                                ? null
                                                : () {
                                                    DownloadManager.instance
                                                        .downloadAll(
                                                          songs
                                                              .map(
                                                                (s) => s
                                                                    .toMediaMetadata(),
                                                              )
                                                              .toList(),
                                                        );
                                                    showSnack(
                                                      context,
                                                      'Downloading ${songs.length} songs',
                                                    );
                                                  },
                                          ),
                                        const SizedBox(width: 4),
                                        RoundIconButton(
                                          icon: Icons.playlist_add_rounded,
                                          size: 40,
                                          onPressed: songs.isEmpty
                                              ? null
                                              : () => showAddToPlaylistSheet(
                                                  context,
                                                  songs
                                                      .map(
                                                        (s) =>
                                                            s.toMediaMetadata(),
                                                      )
                                                      .toList(),
                                                ),
                                        ),
                                        const SizedBox(width: 4),
                                        RoundIconButton(
                                          icon: Icons.queue_music_rounded,
                                          size: 40,
                                          onPressed: songs.isEmpty
                                              ? null
                                              : () => player.addToQueue(
                                                  songs
                                                      .map(
                                                        (s) =>
                                                            s.toMediaMetadata(),
                                                      )
                                                      .toList(),
                                                ),
                                        ),
                                      ],
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 16),
                          Row(
                            children: [
                              Expanded(
                                child: FilledButton.icon(
                                  onPressed: songs.isEmpty
                                      ? null
                                      : () => player.playLocalList(
                                          songs,
                                          title: title,
                                        ),
                                  icon: const Icon(Icons.play_arrow_rounded),
                                  label: const Text('Play'),
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: OutlinedButton.icon(
                                  onPressed: songs.isEmpty
                                      ? null
                                      : () => player.playLocalList(
                                          songs,
                                          title: title,
                                          shuffle: true,
                                        ),
                                  icon: const Icon(Icons.shuffle_rounded),
                                  label: const Text('Shuffle'),
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                  if (songs.isEmpty)
                    SliverToBoxAdapter(
                      child: EmptyPlaceholder(
                        icon: _isLiked
                            ? Icons.favorite_border_rounded
                            : Icons.music_note_rounded,
                        text: _isLiked
                            ? 'Songs you like will appear here.'
                            : _isDownloaded
                            ? 'Downloaded songs will appear here.'
                            : _isTop
                            ? 'Play some music to build your top 50.'
                            : 'This playlist is empty.',
                      ),
                    )
                  else if (editable)
                    SliverReorderableList(
                      itemCount: songs.length,
                      // onReorderItem already accounts for the removed item.
                      onReorderItem: (from, to) =>
                          db.movePlaylistSong(widget.playlistId, from, to),
                      itemBuilder: (context, i) => Padding(
                        key: ValueKey('${songs[i].id}-$i'),
                        padding: const EdgeInsets.symmetric(horizontal: 4),
                        child: LocalSongTile(
                          song: songs[i],
                          songContext: songs,
                          contextTitle: title,
                          playlistId: widget.playlistId,
                          index: i + 1,
                          trailing: ReorderableDragStartListener(
                            index: i,
                            child: const Padding(
                              padding: EdgeInsets.symmetric(horizontal: 8),
                              child: Icon(Icons.drag_handle_rounded),
                            ),
                          ),
                        ),
                      ),
                    )
                  else
                    SliverPadding(
                      padding: const EdgeInsets.symmetric(horizontal: 4),
                      sliver: SliverList.builder(
                        itemCount: songs.length,
                        itemBuilder: (context, i) => LocalSongTile(
                          song: songs[i],
                          songContext: songs,
                          contextTitle: title,
                          playlistId: _isSpecial ? null : widget.playlistId,
                          index: i + 1,
                          showLiked: !_isLiked,
                          downloaded: _isDownloaded,
                        ),
                      ),
                    ),
                  SliverToBoxAdapter(
                    child: SizedBox(
                      height: MediaQuery.paddingOf(context).bottom + 16,
                    ),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }
}
