import 'dart:async';

import 'package:flutter/material.dart';

import '../../data/database.dart';
import '../../data/settings.dart';
import '../../innertube/models/yt_item.dart';
import '../../innertube/pages/pages.dart';
import '../../innertube/youtube.dart';
import '../../playback/player_controller.dart';
import '../components/common.dart';
import '../components/items.dart';
import '../components/thumbnail.dart';
import '../shell/app_navigator.dart';
import 'account_screen.dart';
import 'stats_screen.dart';
import 'explore_screen.dart';
import '../components/watch_builder.dart';

/// Home — port of `HomeScreen.kt`: chips, local quick picks / forgotten
/// favourites / keep listening, then the InnerTube home shelves.
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen>
    with AutomaticKeepAliveClientMixin {
  HomePage? _page;
  HomePage? _basePage;
  List<MoodAndGenresItem> _moods = const [];
  HomeChip? _selectedChip;
  bool _loading = true;
  bool _loadingMore = false;
  String? _error;
  final _scroll = ScrollController();
  StreamSubscription<void>? _settingsSub;

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();
    _load();
    _scroll.addListener(_onScroll);
    Settings.instance.addListener(_onSettings);
  }

  void _onSettings() {
    // Re-filter when content preferences change.
    if (mounted) setState(() {});
  }

  @override
  void dispose() {
    _scroll.dispose();
    Settings.instance.removeListener(_onSettings);
    _settingsSub?.cancel();
    super.dispose();
  }

  Future<void> _load({bool refresh = false}) async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final yt = YouTube.instance;
      final results = await Future.wait([
        yt.home(),
        yt
            .explore()
            .then((e) => e.moodAndGenres)
            .catchError((_) => <MoodAndGenresItem>[]),
      ]);
      if (!mounted) return;
      setState(() {
        _basePage = results[0] as HomePage;
        _page = _basePage;
        _moods = results[1] as List<MoodAndGenresItem>;
        _selectedChip = null;
        _loading = false;
      });
      _fillInitial();
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = '$e';
        _loading = false;
      });
    }
  }

  Future<void> _selectChip(HomeChip? chip) async {
    if (chip == null || chip == _selectedChip) {
      setState(() {
        _selectedChip = null;
        _page = _basePage;
      });
      return;
    }
    final ep = chip.endpoint;
    if (ep == null) return;
    setState(() {
      _selectedChip = chip;
      _loading = true;
    });
    try {
      final page = await YouTube.instance.home(
        browseId: ep.browseId,
        params: ep.params,
      );
      if (!mounted) return;
      setState(() {
        _page = HomePage(
          chips: _basePage?.chips,
          sections: page.sections,
          continuation: page.continuation,
        );
        _loading = false;
      });
      _fillInitial();
    } catch (e) {
      if (!mounted) return;
      setState(() => _loading = false);
    }
  }

  /// InnerTube's first home response is short (2-3 shelves); keep pulling
  /// continuations until the page is tall enough to scroll.
  Future<void> _fillInitial() async {
    for (var i = 0; i < 3; i++) {
      final page = _page;
      if (!mounted ||
          page == null ||
          page.continuation == null ||
          page.sections.length >= 6) {
        return;
      }
      await _loadMore();
    }
  }

  void _onScroll() {
    if (_scroll.position.pixels > _scroll.position.maxScrollExtent - 800) {
      _loadMore();
    }
  }

  Future<void> _loadMore() async {
    final page = _page;
    if (page == null || page.continuation == null || _loadingMore) return;
    _loadingMore = true;
    try {
      final more = await YouTube.instance.home(continuation: page.continuation);
      // A chip change replaced the feed while this was loading; drop it.
      if (!mounted || !identical(_page, page)) return;
      setState(() {
        _page = HomePage(
          chips: page.chips,
          sections: [...page.sections, ...more.sections],
          continuation: more.continuation,
        );
        if (_selectedChip == null) _basePage = _page;
      });
    } catch (_) {
    } finally {
      _loadingMore = false;
    }
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final settings = Settings.instance;
    final page = _page?.filter(
      hideExplicit: settings.hideExplicit,
      hideVideos: settings.hideVideoSongs,
    );
    final db = AppDatabase.instance;
    return Scaffold(
      body: RefreshIndicator(
        onRefresh: () => _load(refresh: true),
        edgeOffset: 100,
        child: CustomScrollView(
          controller: _scroll,
          slivers: [
            SliverAppBar(
              floating: true,
              snap: true,
              title: const Text('Echo Music'),
              actions: [
                IconButton(
                  onPressed: AppNavigator.openHistory,
                  icon: const Icon(Icons.history_rounded),
                ),
                IconButton(
                  onPressed: () =>
                      AppNavigator.push(const ExploreScreen(standalone: true)),
                  icon: const Icon(Icons.trending_up_rounded),
                ),
                IconButton(
                  onPressed: () => AppNavigator.push(const StatsScreen()),
                  icon: const Icon(Icons.insights_rounded),
                ),
                const _AccountAvatar(),
                const SizedBox(width: 8),
              ],
            ),
            if (_page?.chips != null && _page!.chips!.isNotEmpty)
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: ChipsRow(
                    labels: _page!.chips!.map((c) => c.title).toList(),
                    selected: _selectedChip == null
                        ? null
                        : _page!.chips!.indexOf(_selectedChip!),
                    onSelected: (i) =>
                        _selectChip(i == null ? null : _page!.chips![i]),
                  ),
                ),
              ),
            if (_selectedChip == null) ...[
              SliverToBoxAdapter(
                child: WatchBuilder<List<Song>>(
                  query: () => db.quickPicks(),
                  builder: (context, data) {
                    final songs = data ?? const <Song>[];
                    if (songs.isEmpty) return const SizedBox.shrink();
                    return Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        NavigationTitle(
                          title: 'Quick picks',
                          onPlayAll: () =>
                              player.playLocalList(songs, title: 'Quick picks'),
                        ),
                        _LocalSongColumns(songs: songs, title: 'Quick picks'),
                      ],
                    );
                  },
                ),
              ),
              SliverToBoxAdapter(
                child: WatchBuilder<List<Song>>(
                  query: () => db.forgottenFavorites(),
                  builder: (context, data) {
                    final songs = data ?? const <Song>[];
                    if (songs.isEmpty) return const SizedBox.shrink();
                    return Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        NavigationTitle(
                          title: 'Forgotten favorites',
                          onPlayAll: () => player.playLocalList(
                            songs,
                            title: 'Forgotten favorites',
                          ),
                        ),
                        _LocalSongColumns(
                          songs: songs,
                          title: 'Forgotten favorites',
                        ),
                      ],
                    );
                  },
                ),
              ),
              SliverToBoxAdapter(
                child: WatchBuilder<List<Song>>(
                  query: () => db.recentlyPlayed(limit: 12),
                  builder: (context, data) {
                    final songs = data ?? const <Song>[];
                    if (songs.isEmpty) return const SizedBox.shrink();
                    return SectionCarousel(
                      title: 'Keep listening',
                      items: songs.map((s) => s.toSongItem()).toList(),
                      itemWidth: 130,
                      onMore: AppNavigator.openHistory,
                    );
                  },
                ),
              ),
            ],
            if (_loading)
              const SliverToBoxAdapter(
                child: Padding(
                  padding: EdgeInsets.only(top: 16),
                  child: Column(
                    children: [
                      GridShimmer(),
                      SizedBox(height: 16),
                      ListShimmer(count: 4),
                    ],
                  ),
                ),
              )
            else if (_error != null && page == null)
              SliverFillRemaining(
                hasScrollBody: false,
                child: ErrorPlaceholder(
                  message: 'Could not load home.\n$_error',
                  onRetry: _load,
                ),
              )
            else if (page != null)
              SliverList.builder(
                itemCount: page.sections.length,
                itemBuilder: (context, i) =>
                    _HomeSection(section: page.sections[i]),
              ),
            if (_page?.continuation != null && !_loading)
              const SliverToBoxAdapter(
                child: Padding(
                  padding: EdgeInsets.all(24),
                  child: Center(child: CircularProgressIndicator()),
                ),
              ),
            if (_moods.isNotEmpty && !_loading)
              SliverToBoxAdapter(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    NavigationTitle(
                      title: 'Mood & genres',
                      onTap: () => AppNavigator.push(const MoodsScreen()),
                    ),
                    SizedBox(
                      height: 100,
                      child: ListView.separated(
                        scrollDirection: Axis.horizontal,
                        padding: const EdgeInsets.symmetric(horizontal: 16),
                        itemCount: _moods.length,
                        separatorBuilder: (_, _) => const SizedBox(width: 8),
                        itemBuilder: (context, i) => MoodChip(item: _moods[i]),
                      ),
                    ),
                  ],
                ),
              ),
            SliverToBoxAdapter(
              child: SizedBox(
                height: MediaQuery.paddingOf(context).bottom + 16,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _LocalSongColumns extends StatelessWidget {
  final List<Song> songs;
  final String title;
  const _LocalSongColumns({required this.songs, required this.title});

  @override
  Widget build(BuildContext context) {
    const rows = 4;
    final pages = <List<Song>>[];
    for (var i = 0; i < songs.length; i += rows) {
      pages.add(songs.sublist(i, (i + rows).clamp(0, songs.length)));
    }
    final width = MediaQuery.sizeOf(context).width - 40;
    return SizedBox(
      height: rows * 64.0,
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 12),
        physics: const PageScrollPhysics(),
        itemCount: pages.length,
        itemBuilder: (context, p) => SizedBox(
          width: width,
          child: Column(
            children: [
              for (final s in pages[p])
                SizedBox(
                  height: 64,
                  child: LocalSongTile(
                    song: s,
                    songContext: songs,
                    contextTitle: title,
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _HomeSection extends StatelessWidget {
  final HomeSection section;
  const _HomeSection({required this.section});

  @override
  Widget build(BuildContext context) {
    final allSongs = section.items.every((i) => i is SongItem);
    final songs = section.items.whereType<SongItem>().toList();
    VoidCallback? onMore;
    final ep = section.endpoint;
    if (ep != null) {
      onMore = () {
        if (ep.browseId.startsWith('VL')) {
          AppNavigator.openPlaylist(ep.browseId.substring(2));
        } else if (ep.browseId.startsWith('MPRE')) {
          AppNavigator.openAlbum(ep.browseId);
        } else if (ep.browseId.startsWith('UC')) {
          AppNavigator.openArtist(ep.browseId);
        } else {
          AppNavigator.openBrowse(ep, title: section.title);
        }
      };
    }
    if (allSongs && songs.length > 3) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          NavigationTitle(
            title: section.title,
            label: section.label,
            onTap: onMore,
            onPlayAll: () => player.playSongItems(songs, title: section.title),
            thumbnail: section.thumbnail != null
                ? EchoImage(
                    url: section.thumbnail,
                    width: 44,
                    height: 44,
                    radius: 22,
                    circle: true,
                    resize: 224,
                  )
                : null,
          ),
          SongColumnsPager(songs: songs, contextTitle: section.title),
        ],
      );
    }
    return SectionCarousel(
      title: section.title,
      label: section.label,
      items: section.items,
      onMore: onMore,
      onPlayAll: allSongs
          ? () => player.playSongItems(songs, title: section.title)
          : null,
      itemWidth: section.items.length == 1 ? 240 : 150,
    );
  }
}

class _AccountAvatar extends StatelessWidget {
  const _AccountAvatar();

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: Settings.instance,
      builder: (context, _) {
        final s = Settings.instance;
        return InkWell(
          onTap: () => AppNavigator.push(const AccountScreen()),
          customBorder: const CircleBorder(),
          child: Padding(
            padding: const EdgeInsets.all(6),
            child: s.isLoggedIn && s.accountAvatarUrl.isNotEmpty
                ? EchoImage(
                    url: s.accountAvatarUrl,
                    width: 32,
                    height: 32,
                    circle: true,
                    resize: 96,
                  )
                : CircleAvatar(
                    radius: 16,
                    backgroundColor: Theme.of(context)
                        .colorScheme
                        .surfaceContainerHighest,
                    child: Icon(
                      Icons.person_rounded,
                      size: 20,
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
                  ),
          ),
        );
      },
    );
  }
}

/// Colourful mood/genre pill (left stripe colour comes from InnerTube).
class MoodChip extends StatelessWidget {
  final MoodAndGenresItem item;
  final double width;
  const MoodChip({super.key, required this.item, this.width = 160});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final stripe = Color(item.stripeColor | 0xFF000000);
    return SizedBox(
      width: width,
      child: Material(
        color: scheme.surfaceContainerHigh,
        borderRadius: BorderRadius.circular(16),
        child: InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: () =>
              AppNavigator.openBrowse(item.endpoint, title: item.title),
          child: Row(
            children: [
              Container(
                width: 8,
                height: double.infinity,
                decoration: BoxDecoration(
                  color: stripe,
                  borderRadius: const BorderRadius.horizontal(
                    left: Radius.circular(16),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  item.title,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.titleSmall
                      ?.copyWith(fontWeight: FontWeight.w700),
                ),
              ),
              const SizedBox(width: 8),
            ],
          ),
        ),
      ),
    );
  }
}
