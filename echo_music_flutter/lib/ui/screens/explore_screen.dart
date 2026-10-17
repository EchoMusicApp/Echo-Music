import 'package:flutter/material.dart';

import '../../data/settings.dart';
import '../../innertube/models/yt_item.dart';
import '../../innertube/pages/pages.dart';
import '../../innertube/youtube.dart';
import '../../playback/player_controller.dart';
import '../components/common.dart';
import '../components/items.dart';
import '../shell/app_navigator.dart';
import 'home_screen.dart';

/// Explore — new releases, moods & genres, and charts (port of
/// `ExploreScreen.kt` + `ChartsScreen.kt`).
class ExploreScreen extends StatefulWidget {
  final bool standalone;
  const ExploreScreen({super.key, this.standalone = false});

  @override
  State<ExploreScreen> createState() => _ExploreScreenState();
}

class _ExploreScreenState extends State<ExploreScreen>
    with AutomaticKeepAliveClientMixin {
  ExplorePage? _explore;
  ChartsPage? _charts;
  bool _loading = true;
  String? _error;

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final yt = YouTube.instance;
      final results = await Future.wait([
        yt.explore(),
        yt.charts().catchError((_) => const ChartsPage(sections: [])),
      ]);
      if (!mounted) return;
      setState(() {
        _explore = results[0] as ExplorePage;
        _charts = results[1] as ChartsPage;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = '$e';
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final theme = Theme.of(context);
    final settings = Settings.instance;
    return Scaffold(
      body: RefreshIndicator(
        onRefresh: _load,
        edgeOffset: 100,
        child: CustomScrollView(
          slivers: [
            SliverAppBar(
              floating: true,
              snap: true,
              title: const Text('Explore'),
              leading: widget.standalone ? const BackButton() : null,
            ),
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
                child: Material(
                  color: theme.colorScheme.surfaceContainerHigh,
                  borderRadius: BorderRadius.circular(28),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(28),
                    onTap: () => AppNavigator.currentTab.value = 3,
                    child: Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 20,
                        vertical: 16,
                      ),
                      child: Row(
                        children: [
                          Icon(
                            Icons.search_rounded,
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                          const SizedBox(width: 12),
                          Text(
                            'Search YouTube Music…',
                            style: theme.textTheme.bodyLarge?.copyWith(
                              color: theme.colorScheme.onSurfaceVariant,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
            if (_loading)
              const SliverToBoxAdapter(
                child: Column(children: [GridShimmer(), ListShimmer(count: 5)]),
              )
            else if (_error != null)
              SliverFillRemaining(
                hasScrollBody: false,
                child: ErrorPlaceholder(message: _error!, onRetry: _load),
              )
            else ...[
              if (_explore != null && _explore!.newReleaseAlbums.isNotEmpty)
                SliverToBoxAdapter(
                  child: SectionCarousel(
                    title: 'New release albums',
                    items: _explore!.newReleaseAlbums.filterExplicit(
                      settings.hideExplicit,
                    ),
                    onMore: () => AppNavigator.push(const NewReleasesScreen()),
                  ),
                ),
              if (_explore != null && _explore!.moodAndGenres.isNotEmpty)
                SliverToBoxAdapter(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      NavigationTitle(
                        title: 'Moods & genres',
                        onTap: () => AppNavigator.push(const MoodsScreen()),
                      ),
                      SizedBox(
                        height: 140,
                        child: GridView.builder(
                          scrollDirection: Axis.horizontal,
                          padding: const EdgeInsets.symmetric(horizontal: 16),
                          gridDelegate:
                              const SliverGridDelegateWithFixedCrossAxisCount(
                                crossAxisCount: 2,
                                mainAxisSpacing: 8,
                                crossAxisSpacing: 8,
                                childAspectRatio: 62 / 170,
                              ),
                          itemCount: _explore!.moodAndGenres.length,
                          itemBuilder: (context, i) => MoodChip(
                            item: _explore!.moodAndGenres[i],
                            width: 170,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              if (_charts != null)
                SliverList.builder(
                  itemCount: _charts!.sections.length,
                  itemBuilder: (context, i) =>
                      _ChartSectionView(section: _charts!.sections[i]),
                ),
            ],
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

class _ChartSectionView extends StatelessWidget {
  final ChartSection section;
  const _ChartSectionView({required this.section});

  @override
  Widget build(BuildContext context) {
    final songs = section.items.whereType<SongItem>().toList();
    if (songs.length == section.items.length && songs.isNotEmpty) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          NavigationTitle(
            title: section.title,
            onPlayAll: () => player.playSongItems(songs, title: section.title),
          ),
          _ChartColumns(songs: songs, title: section.title),
        ],
      );
    }
    return SectionCarousel(title: section.title, items: section.items);
  }
}

class _ChartColumns extends StatelessWidget {
  final List<SongItem> songs;
  final String title;
  const _ChartColumns({required this.songs, required this.title});

  @override
  Widget build(BuildContext context) {
    const rows = 4;
    final pages = <List<SongItem>>[];
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
              for (var r = 0; r < pages[p].length; r++)
                SizedBox(
                  height: 64,
                  child: YTItemTile(
                    item: pages[p][r],
                    songContext: songs,
                    contextTitle: title,
                    index: pages[p][r].chartPosition ?? (p * rows + r + 1),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class NewReleasesScreen extends StatelessWidget {
  const NewReleasesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('New release albums')),
      body: FutureBuilder<List<AlbumItem>>(
        future: YouTube.instance.newReleaseAlbums(),
        builder: (context, snap) {
          if (snap.hasError) return ErrorPlaceholder(message: '${snap.error}');
          if (!snap.hasData) {
            return const Center(child: CircularProgressIndicator());
          }
          final items = snap.data!.filterExplicit(
            Settings.instance.hideExplicit,
          );
          return ItemGrid(items: items);
        },
      ),
    );
  }
}

class MoodsScreen extends StatelessWidget {
  const MoodsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Moods & genres')),
      body: FutureBuilder<List<MoodAndGenres>>(
        future: YouTube.instance.moodAndGenres(),
        builder: (context, snap) {
          if (snap.hasError) return ErrorPlaceholder(message: '${snap.error}');
          if (!snap.hasData) {
            return const Center(child: CircularProgressIndicator());
          }
          final groups = snap.data!;
          return ListView.builder(
            padding: EdgeInsets.only(
              bottom: MediaQuery.paddingOf(context).bottom + 16,
            ),
            itemCount: groups.length,
            itemBuilder: (context, i) => Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                NavigationTitle(title: groups[i].title),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: GridView.builder(
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    gridDelegate:
                        const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 2,
                          mainAxisSpacing: 8,
                          crossAxisSpacing: 8,
                          childAspectRatio: 170 / 62,
                        ),
                    itemCount: groups[i].items.length,
                    itemBuilder: (context, j) => MoodChip(
                      item: groups[i].items[j],
                      width: double.infinity,
                    ),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

/// 2-column grid of any items with bottom padding for the floating bars.
class ItemGrid extends StatelessWidget {
  final List<YTItem> items;
  final ScrollController? controller;
  final Widget? footer;
  const ItemGrid({
    super.key,
    required this.items,
    this.controller,
    this.footer,
  });

  @override
  Widget build(BuildContext context) {
    final width = (MediaQuery.sizeOf(context).width - 48) / 2;
    return GridView.builder(
      controller: controller,
      padding: EdgeInsets.fromLTRB(
        16,
        8,
        16,
        MediaQuery.paddingOf(context).bottom + 16,
      ),
      gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        mainAxisSpacing: 16,
        crossAxisSpacing: 16,
        childAspectRatio: width / (width + 52),
      ),
      itemCount: items.length + (footer != null ? 1 : 0),
      itemBuilder: (context, i) {
        if (i == items.length) return footer!;
        return YTGridItem(item: items[i], width: width);
      },
    );
  }
}
