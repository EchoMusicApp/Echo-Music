import 'package:flutter/material.dart';

import '../../data/settings.dart';
import '../../innertube/models/yt_item.dart';
import '../../innertube/pages/pages.dart';
import '../../innertube/youtube.dart';
import '../../playback/player_controller.dart';
import '../components/common.dart';
import '../components/items.dart';
import 'explore_screen.dart';

/// "See all" page for an artist section (port of `ArtistItemsScreen.kt`).
class ArtistItemsScreen extends StatefulWidget {
  final BrowseEndpoint endpoint;
  final String? title;
  const ArtistItemsScreen({super.key, required this.endpoint, this.title});

  @override
  State<ArtistItemsScreen> createState() => _ArtistItemsScreenState();
}

class _ArtistItemsScreenState extends State<ArtistItemsScreen> {
  ArtistItemsPage? _page;
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
    try {
      final p = await YouTube.instance.artistItems(widget.endpoint);
      if (mounted) setState(() => _page = p);
    } catch (e) {
      if (mounted) setState(() => _error = '$e');
    }
  }

  Future<void> _loadMore() async {
    final p = _page;
    if (p == null || p.continuation == null || _loadingMore) return;
    _loadingMore = true;
    try {
      final more = await YouTube.instance.artistItemsContinuation(
        p.continuation!,
      );
      if (mounted) {
        setState(
          () => _page = ArtistItemsPage(
            title: p.title,
            items: [...p.items, ...more.items],
            continuation: more.continuation,
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
    final page = _page;
    final settings = Settings.instance;
    final items =
        page?.items
            .filterExplicit(settings.hideExplicit)
            .filterVideoSongs(settings.hideVideoSongs) ??
        const <YTItem>[];
    final songs = items.whereType<SongItem>().toList();
    final isSongList = items.isNotEmpty && songs.length == items.length;
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title ?? page?.title ?? ''),
        actions: [
          if (isSongList)
            IconButton(
              onPressed: () => player.playSongItems(
                songs,
                title: widget.title,
                shuffle: true,
              ),
              icon: const Icon(Icons.shuffle_rounded),
            ),
        ],
      ),
      body: _error != null
          ? ErrorPlaceholder(message: _error!, onRetry: _load)
          : page == null
          ? const ListShimmer()
          : isSongList
          ? ListView.builder(
              controller: _scroll,
              padding: EdgeInsets.fromLTRB(
                4,
                0,
                4,
                MediaQuery.paddingOf(context).bottom + 16,
              ),
              itemCount: songs.length + (page.continuation != null ? 1 : 0),
              itemBuilder: (context, i) {
                if (i == songs.length) {
                  return const Padding(
                    padding: EdgeInsets.all(16),
                    child: Center(child: CircularProgressIndicator()),
                  );
                }
                return YTItemTile(
                  item: songs[i],
                  songContext: songs,
                  contextTitle: widget.title,
                  index: i + 1,
                );
              },
            )
          : ItemGrid(
              controller: _scroll,
              items: items,
              footer: page.continuation != null
                  ? const Center(child: CircularProgressIndicator())
                  : null,
            ),
    );
  }
}
