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

/// Album page (port of `AlbumScreen.kt`).
class AlbumScreen extends StatefulWidget {
  final String browseId;
  final AlbumItem? initial;
  const AlbumScreen({super.key, required this.browseId, this.initial});

  @override
  State<AlbumScreen> createState() => _AlbumScreenState();
}

class _AlbumScreenState extends State<AlbumScreen> {
  AlbumPage? _page;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _error = null);
    try {
      final page = await YouTube.instance.album(widget.browseId);
      if (!mounted) return;
      setState(() => _page = page);
      AppDatabase.instance.upsertAlbum(page.album, page.songs);
    } catch (e) {
      if (mounted) setState(() => _error = '$e');
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final page = _page;
    final album = page?.album ?? widget.initial;
    final songs =
        page?.songs.filterExplicit(Settings.instance.hideExplicit) ??
        const <SongItem>[];
    final duration = songs.fold<int>(0, (a, s) => a + (s.duration ?? 0));
    return Scaffold(
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            pinned: true,
            title: Text(album?.title ?? ''),
            actions: [
              if (album != null)
                IconButton(
                  onPressed: () => showAlbumMenu(context, album),
                  icon: const Icon(Icons.more_vert_rounded),
                ),
            ],
          ),
          if (_error != null)
            SliverFillRemaining(
              hasScrollBody: false,
              child: ErrorPlaceholder(message: _error!, onRetry: _load),
            )
          else if (album == null)
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
                          url: album.thumbnail,
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
                                album.title,
                                style: theme.textTheme.titleLarge?.copyWith(
                                  fontWeight: FontWeight.w800,
                                ),
                                maxLines: 3,
                                overflow: TextOverflow.ellipsis,
                              ),
                              const SizedBox(height: 4),
                              Wrap(
                                children: [
                                  for (final (i, a)
                                      in (album.artists ?? const <Artist>[])
                                          .indexed)
                                    GestureDetector(
                                      onTap: a.id != null
                                          ? () => AppNavigator.openArtist(a.id!)
                                          : null,
                                      child: Text(
                                        '${a.name}${i < album.artists!.length - 1 ? ', ' : ''}',
                                        style: theme.textTheme.bodyLarge
                                            ?.copyWith(
                                              color: a.id != null
                                                  ? theme.colorScheme.primary
                                                  : theme
                                                        .colorScheme
                                                        .onSurfaceVariant,
                                              fontWeight: FontWeight.w600,
                                            ),
                                      ),
                                    ),
                                ],
                              ),
                              const SizedBox(height: 4),
                              Text(
                                joinByBullet([
                                  if (album.year != null) '${album.year}',
                                  if (songs.isNotEmpty) '${songs.length} songs',
                                  if (duration > 0) makeTimeString(duration),
                                ]),
                                style: theme.textTheme.bodyMedium?.copyWith(
                                  color: theme.colorScheme.onSurfaceVariant,
                                ),
                              ),
                              const SizedBox(height: 12),
                              Row(
                                children: [
                                  _BookmarkButton(album: album, songs: songs),
                                  const SizedBox(width: 4),
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
                                                        MediaMetadata
                                                            .fromSongItem,
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
                                                .map(MediaMetadata.fromSongItem)
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
                                : () => player.playAlbum(album, songs: songs),
                            icon: const Icon(Icons.play_arrow_rounded),
                            label: const Text('Play'),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: OutlinedButton.icon(
                            onPressed: songs.isEmpty
                                ? null
                                : () => player.playAlbum(
                                    album,
                                    songs: songs,
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
                    contextTitle: album.title,
                    index: i + 1,
                  ),
                ),
              ),
              if (page.otherVersions.isNotEmpty)
                SliverToBoxAdapter(
                  child: SectionCarousel(
                    title: 'Other versions',
                    items: page.otherVersions,
                  ),
                ),
              if (page.releasesForYou.isNotEmpty)
                SliverToBoxAdapter(
                  child: SectionCarousel(
                    title: 'More from the artist',
                    items: page.releasesForYou,
                  ),
                ),
              if (page.description != null && page.description!.isNotEmpty)
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
                    child: EchoCard(
                      child: Text(
                        page.description!,
                        style: theme.textTheme.bodyMedium,
                      ),
                    ),
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

class _BookmarkButton extends StatelessWidget {
  final AlbumItem album;
  final List<SongItem> songs;
  const _BookmarkButton({required this.album, required this.songs});

  @override
  Widget build(BuildContext context) {
    final db = AppDatabase.instance;
    return WatchBuilder<AlbumRow?>(
      watchKey: album.browseId,
      query: () => db.album(album.browseId),
      builder: (context, data) {
        final saved = data?.bookmarkedAt != null;
        return RoundIconButton(
          icon: saved ? Icons.bookmark_rounded : Icons.bookmark_border_rounded,
          size: 40,
          filled: saved,
          onPressed: () async {
            await db.toggleAlbumBookmark(album, songs);
            if (Settings.instance.isLoggedIn && Settings.instance.ytmSync) {
              try {
                await YouTube.instance.likePlaylist(album.playlistId, !saved);
              } catch (_) {}
            }
          },
        );
      },
    );
  }
}
