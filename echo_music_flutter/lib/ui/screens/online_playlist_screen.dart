import 'package:flutter/material.dart';

import '../../core/utils.dart';
import '../../data/database.dart';
import '../../data/download_manager.dart';
import '../../data/settings.dart';
import '../../innertube/models/yt_item.dart';
import '../../innertube/pages/pages.dart';
import '../../innertube/youtube.dart';
import '../../playback/media_metadata.dart';
import '../../playback/player_controller.dart';
import '../components/common.dart';
import '../components/items.dart';
import '../components/menus.dart';
import '../components/thumbnail.dart';
import '../shell/app_navigator.dart';
import '../components/watch_builder.dart';

/// YouTube Music playlist page (port of `OnlinePlaylistScreen.kt`).
class OnlinePlaylistScreen extends StatefulWidget {
  final String playlistId;
  final PlaylistItem? initial;
  const OnlinePlaylistScreen({
    super.key,
    required this.playlistId,
    this.initial,
  });

  @override
  State<OnlinePlaylistScreen> createState() => _OnlinePlaylistScreenState();
}

class _OnlinePlaylistScreenState extends State<OnlinePlaylistScreen> {
  PlaylistPage? _page;
  String? _error;
  bool _loadingMore = false;
  final _scroll = ScrollController();

  @override
  void initState() {
    super.initState();
    _load();
    _scroll.addListener(() {
      if (_scroll.position.pixels > _scroll.position.maxScrollExtent - 600) {
        _loadMore();
      }
    });
  }

  @override
  void dispose() {
    _scroll.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _error = null);
    try {
      final p = await YouTube.instance.playlist(widget.playlistId);
      if (mounted) setState(() => _page = p);
    } catch (e) {
      if (mounted) setState(() => _error = '$e');
    }
  }

  Future<void> _loadMore() async {
    final p = _page;
    if (p == null || p.songsContinuation == null || _loadingMore) return;
    _loadingMore = true;
    try {
      final more = await YouTube.instance.playlistContinuation(
        p.songsContinuation!,
      );
      if (mounted) {
        setState(
          () => _page = PlaylistPage(
            playlist: p.playlist,
            songs: [...p.songs, ...more.songs],
            songsContinuation: more.continuation,
            continuation: p.continuation,
            related: p.related,
          ),
        );
      }
    } catch (_) {
    } finally {
      _loadingMore = false;
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final page = _page;
    final playlist = page?.playlist ?? widget.initial;
    final songs =
        page?.songs
            .filterExplicit(Settings.instance.hideExplicit)
            .filterVideoSongs(Settings.instance.hideVideoSongs) ??
        const <SongItem>[];
    final duration = songs.fold<int>(0, (a, s) => a + (s.duration ?? 0));
    return Scaffold(
      body: CustomScrollView(
        controller: _scroll,
        slivers: [
          SliverAppBar(
            pinned: true,
            title: Text(playlist?.title ?? ''),
            actions: [
              if (playlist != null)
                IconButton(
                  onPressed: () => showPlaylistMenu(context, playlist),
                  icon: const Icon(Icons.more_vert_rounded),
                ),
            ],
          ),
          if (_error != null)
            SliverFillRemaining(
              hasScrollBody: false,
              child: ErrorPlaceholder(message: _error!, onRetry: _load),
            )
          else if (playlist == null)
            const SliverFillRemaining(
              hasScrollBody: false,
              child: Center(child: CircularProgressIndicator()),
            )
          else ...[
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        EchoImage(
                          url: playlist.thumbnail,
                          width: 144,
                          height: 144,
                          radius: 16,
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                playlist.title,
                                style: theme.textTheme.titleLarge?.copyWith(
                                  fontWeight: FontWeight.w800,
                                ),
                                maxLines: 3,
                                overflow: TextOverflow.ellipsis,
                              ),
                              const SizedBox(height: 4),
                              if (playlist.author != null)
                                GestureDetector(
                                  onTap: playlist.author!.id != null
                                      ? () => AppNavigator.openArtist(
                                          playlist.author!.id!,
                                        )
                                      : null,
                                  child: Text(
                                    playlist.author!.name,
                                    style: theme.textTheme.bodyLarge?.copyWith(
                                      color: playlist.author!.id != null
                                          ? theme.colorScheme.primary
                                          : theme.colorScheme.onSurfaceVariant,
                                      fontWeight: FontWeight.w600,
                                    ),
                                  ),
                                ),
                              const SizedBox(height: 4),
                              Text(
                                joinByBullet([
                                  playlist.songCountText ??
                                      (songs.isNotEmpty
                                          ? '${songs.length} songs'
                                          : null),
                                  if (duration > 0 &&
                                      page?.songsContinuation == null)
                                    makeTimeString(duration),
                                ]),
                                style: theme.textTheme.bodyMedium?.copyWith(
                                  color: theme.colorScheme.onSurfaceVariant,
                                ),
                              ),
                              const SizedBox(height: 12),
                              Row(
                                children: [
                                  _SaveButton(playlist: playlist),
                                  const SizedBox(width: 4),
                                  RoundIconButton(
                                    icon: Icons.download_rounded,
                                    size: 40,
                                    onPressed: () async {
                                      final all = await YouTube.instance
                                          .playlistCompleted(playlist.id);
                                      DownloadManager.instance.downloadAll(
                                        all.songs
                                            .map(MediaMetadata.fromSongItem)
                                            .toList(),
                                      );
                                      if (context.mounted) {
                                        showSnack(
                                          context,
                                          'Downloading ${all.songs.length} songs',
                                        );
                                      }
                                    },
                                  ),
                                  const SizedBox(width: 4),
                                  RoundIconButton(
                                    icon: Icons.playlist_add_rounded,
                                    size: 40,
                                    onPressed: () async {
                                      final all = await YouTube.instance
                                          .playlistCompleted(playlist.id);
                                      if (context.mounted) {
                                        showAddToPlaylistSheet(
                                          context,
                                          all.songs
                                              .map(MediaMetadata.fromSongItem)
                                              .toList(),
                                        );
                                      }
                                    },
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
                            onPressed: () => player.playPlaylist(playlist),
                            icon: const Icon(Icons.play_arrow_rounded),
                            label: const Text('Play'),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: OutlinedButton.icon(
                            onPressed: () =>
                                player.playPlaylist(playlist, shuffle: true),
                            icon: const Icon(Icons.shuffle_rounded),
                            label: const Text('Shuffle'),
                          ),
                        ),
                        if (playlist.radioEndpoint != null) ...[
                          const SizedBox(width: 12),
                          RoundIconButton(
                            icon: Icons.radio_rounded,
                            onPressed: () =>
                                player.playPlaylist(playlist, radio: true),
                          ),
                        ],
                      ],
                    ),
                  ],
                ),
              ),
            ),
            if (page == null)
              const SliverToBoxAdapter(child: ListShimmer())
            else ...[
              SliverPadding(
                padding: const EdgeInsets.symmetric(horizontal: 4),
                sliver: SliverList.builder(
                  itemCount: songs.length,
                  itemBuilder: (context, i) => YTItemTile(
                    item: songs[i],
                    songContext: songs,
                    contextTitle: playlist.title,
                    index: i + 1,
                  ),
                ),
              ),
              if (page.songsContinuation != null)
                const SliverToBoxAdapter(
                  child: Padding(
                    padding: EdgeInsets.all(16),
                    child: Center(child: CircularProgressIndicator()),
                  ),
                ),
              if (page.related != null && page.related!.isNotEmpty)
                SliverToBoxAdapter(
                  child: SectionCarousel(
                    title: 'Related',
                    items: page.related!,
                  ),
                ),
            ],
          ],
          SliverToBoxAdapter(
            child: SizedBox(height: MediaQuery.paddingOf(context).bottom + 16),
          ),
        ],
      ),
    );
  }
}

class _SaveButton extends StatelessWidget {
  final PlaylistItem playlist;
  const _SaveButton({required this.playlist});

  @override
  Widget build(BuildContext context) {
    final db = AppDatabase.instance;
    return WatchBuilder<PlaylistRow?>(
      watchKey: playlist.id,
      query: () => db.playlistByBrowseId(playlist.id),
      builder: (context, data) {
        final row = data;
        final saved = row != null;
        return RoundIconButton(
          icon: saved ? Icons.bookmark_rounded : Icons.bookmark_border_rounded,
          size: 40,
          filled: saved,
          onPressed: () async {
            if (saved) {
              await db.deletePlaylist(row.id);
            } else {
              final page = await YouTube.instance.playlistCompleted(
                playlist.id,
              );
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
        );
      },
    );
  }
}
