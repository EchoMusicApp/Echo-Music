import 'package:flutter/material.dart';

import '../../data/database.dart';
import '../../data/settings.dart';
import '../../innertube/models/yt_item.dart';
import '../../innertube/pages/pages.dart';
import '../../innertube/youtube.dart';
import '../../playback/player_controller.dart';
import '../components/common.dart';
import '../components/items.dart';
import '../components/menus.dart';
import '../components/thumbnail.dart';
import '../shell/app_navigator.dart';
import '../components/watch_builder.dart';

/// Artist page (port of `ArtistScreen.kt`).
class ArtistScreen extends StatefulWidget {
  final String artistId;
  const ArtistScreen({super.key, required this.artistId});

  @override
  State<ArtistScreen> createState() => _ArtistScreenState();
}

class _ArtistScreenState extends State<ArtistScreen> {
  ArtistPage? _page;
  String? _error;
  bool _descExpanded = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _error = null);
    try {
      final page = await YouTube.instance.artist(widget.artistId);
      if (!mounted) return;
      setState(() => _page = page);
      AppDatabase.instance.upsertArtist(page.artist);
    } catch (e) {
      if (mounted) setState(() => _error = '$e');
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final page = _page;
    final settings = Settings.instance;
    return Scaffold(
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            expandedHeight: 320,
            pinned: true,
            stretch: true,
            flexibleSpace: FlexibleSpaceBar(
              stretchModes: const [StretchMode.zoomBackground],
              background: page == null
                  ? Container(color: theme.colorScheme.surfaceContainerHighest)
                  : Stack(
                      fit: StackFit.expand,
                      children: [
                        EchoImage(
                          url: page.artist.thumbnail,
                          radius: 0,
                          resize: 1080,
                        ),
                        DecoratedBox(
                          decoration: BoxDecoration(
                            gradient: LinearGradient(
                              begin: Alignment.topCenter,
                              end: Alignment.bottomCenter,
                              colors: [
                                Colors.transparent,
                                theme.colorScheme.surface,
                              ],
                              stops: const [0.45, 1],
                            ),
                          ),
                        ),
                        Positioned(
                          left: 16,
                          right: 16,
                          bottom: 12,
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                page.artist.title,
                                style: theme.textTheme.headlineMedium?.copyWith(
                                  fontWeight: FontWeight.w900,
                                ),
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                              ),
                              if (page.subscriberCountText != null ||
                                  page.monthlyListenerCount != null)
                                Text(
                                  [
                                    if (page.subscriberCountText != null)
                                      '${page.subscriberCountText} subscribers',
                                    if (page.monthlyListenerCount != null)
                                      '${page.monthlyListenerCount} monthly listeners',
                                  ].join(' • '),
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
            actions: [
              if (page != null)
                IconButton(
                  onPressed: () => showArtistMenu(context, page.artist),
                  icon: const Icon(Icons.more_vert_rounded),
                ),
            ],
          ),
          if (_error != null)
            SliverFillRemaining(
              hasScrollBody: false,
              child: ErrorPlaceholder(message: _error!, onRetry: _load),
            )
          else if (page == null)
            const SliverToBoxAdapter(child: ListShimmer())
          else ...[
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
                child: Row(
                  children: [
                    if (page.artist.shuffleEndpoint != null ||
                        page.artist.playEndpoint != null)
                      Expanded(
                        child: FilledButton.icon(
                          onPressed: () =>
                              player.playArtist(page.artist, shuffle: true),
                          icon: const Icon(Icons.shuffle_rounded),
                          label: const Text('Shuffle'),
                        ),
                      ),
                    if (page.artist.radioEndpoint != null) ...[
                      const SizedBox(width: 12),
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: () =>
                              player.playArtist(page.artist, radio: true),
                          icon: const Icon(Icons.radio_rounded),
                          label: const Text('Radio'),
                        ),
                      ),
                    ],
                    const SizedBox(width: 12),
                    _SubscribeButton(artist: page.artist),
                  ],
                ),
              ),
            ),
            SliverList.builder(
              itemCount: page.sections.length,
              itemBuilder: (context, i) {
                final s = page.sections[i];
                final items = s.items
                    .filterExplicit(settings.hideExplicit)
                    .filterVideoSongs(settings.hideVideoSongs);
                if (items.isEmpty) return const SizedBox.shrink();
                final songs = items.whereType<SongItem>().toList();
                final onMore = s.moreEndpoint != null
                    ? () => AppNavigator.openArtistItems(
                        s.moreEndpoint!,
                        title: s.title,
                      )
                    : null;
                if (songs.length == items.length) {
                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      NavigationTitle(
                        title: s.title,
                        onTap: onMore,
                        onPlayAll: () => player.playSongItems(
                          songs,
                          title: '${page.artist.title} • ${s.title}',
                        ),
                      ),
                      for (final song in songs.take(5))
                        YTItemTile(
                          item: song,
                          songContext: songs,
                          contextTitle: page.artist.title,
                        ),
                    ],
                  );
                }
                return SectionCarousel(
                  title: s.title,
                  items: items,
                  onMore: onMore,
                );
              },
            ),
            if (page.description != null && page.description!.isNotEmpty)
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
                  child: EchoCard(
                    onTap: () => setState(() => _descExpanded = !_descExpanded),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'ABOUT',
                          style: theme.textTheme.labelLarge?.copyWith(
                            fontWeight: FontWeight.w800,
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                        const SizedBox(height: 6),
                        Text(
                          page.description!,
                          maxLines: _descExpanded ? null : 4,
                          overflow: _descExpanded
                              ? null
                              : TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                ),
              ),
          ],
          SliverToBoxAdapter(
            child: SizedBox(height: MediaQuery.paddingOf(context).bottom + 16),
          ),
        ],
      ),
    );
  }
}

class _SubscribeButton extends StatelessWidget {
  final ArtistItem artist;
  const _SubscribeButton({required this.artist});

  @override
  Widget build(BuildContext context) {
    final db = AppDatabase.instance;
    return WatchBuilder<ArtistRow?>(
      watchKey: artist.id,
      query: () => db.artist(artist.id),
      builder: (context, data) {
        final sub = data?.bookmarkedAt != null;
        return RoundIconButton(
          icon: sub ? Icons.bookmark_rounded : Icons.bookmark_border_rounded,
          filled: sub,
          onPressed: () async {
            await db.toggleArtistBookmark(artist);
            if (Settings.instance.isLoggedIn &&
                Settings.instance.ytmSync &&
                artist.channelId != null) {
              try {
                await YouTube.instance.subscribeChannel(
                  artist.channelId!,
                  !sub,
                );
              } catch (_) {}
            }
          },
        );
      },
    );
  }
}
