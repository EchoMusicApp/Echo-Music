import 'package:flutter/material.dart';

import '../../data/settings.dart';
import '../../innertube/models/yt_item.dart';
import '../../innertube/pages/pages.dart';
import '../../innertube/youtube.dart';
import '../../playback/player_controller.dart';
import '../components/common.dart';
import '../components/items.dart';
import 'explore_screen.dart';

/// Generic browse page for moods/genres and "more" shelves
/// (port of `YouTubeBrowseScreen.kt` / `BrowseScreen.kt`).
class BrowseScreen extends StatefulWidget {
  final BrowseEndpoint endpoint;
  final String? title;
  const BrowseScreen({super.key, required this.endpoint, this.title});

  @override
  State<BrowseScreen> createState() => _BrowseScreenState();
}

class _BrowseScreenState extends State<BrowseScreen> {
  BrowseResult? _result;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _error = null);
    try {
      final r = await YouTube.instance.browse(
        widget.endpoint.browseId,
        widget.endpoint.params,
      );
      if (mounted) setState(() => _result = r);
    } catch (e) {
      if (mounted) setState(() => _error = '$e');
    }
  }

  @override
  Widget build(BuildContext context) {
    final r = _result;
    final settings = Settings.instance;
    final sections =
        r?.items
            .map(
              (s) => BrowseResultItem(
                title: s.title,
                items: s.items
                    .filterExplicit(settings.hideExplicit)
                    .filterVideoSongs(settings.hideVideoSongs)
                    .filterYoutubeShorts(settings.hideYoutubeShorts),
              ),
            )
            .where((s) => s.items.isNotEmpty)
            .toList() ??
        const <BrowseResultItem>[];
    return Scaffold(
      appBar: AppBar(title: Text(widget.title ?? r?.title ?? '')),
      body: _error != null
          ? ErrorPlaceholder(message: _error!, onRetry: _load)
          : r == null
          ? const Column(children: [GridShimmer(), ListShimmer(count: 3)])
          : sections.isEmpty
          ? const EmptyPlaceholder(
              icon: Icons.explore_off_rounded,
              text: 'Nothing here',
            )
          : sections.length == 1
          ? _SingleSection(section: sections.first)
          : ListView.builder(
              padding: EdgeInsets.only(
                bottom: MediaQuery.paddingOf(context).bottom + 16,
              ),
              itemCount: sections.length,
              itemBuilder: (context, i) {
                final s = sections[i];
                final songs = s.items.whereType<SongItem>().toList();
                if (songs.isNotEmpty && songs.length == s.items.length) {
                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      NavigationTitle(
                        title: s.title ?? '',
                        onPlayAll: () =>
                            player.playSongItems(songs, title: s.title),
                      ),
                      SongColumnsPager(songs: songs, contextTitle: s.title),
                    ],
                  );
                }
                return SectionCarousel(title: s.title ?? '', items: s.items);
              },
            ),
    );
  }
}

class _SingleSection extends StatelessWidget {
  final BrowseResultItem section;
  const _SingleSection({required this.section});

  @override
  Widget build(BuildContext context) {
    final songs = section.items.whereType<SongItem>().toList();
    if (songs.isNotEmpty && songs.length == section.items.length) {
      return ListView.builder(
        padding: EdgeInsets.fromLTRB(
          4,
          0,
          4,
          MediaQuery.paddingOf(context).bottom + 16,
        ),
        itemCount: songs.length,
        itemBuilder: (context, i) => YTItemTile(
          item: songs[i],
          songContext: songs,
          contextTitle: section.title,
          index: i + 1,
        ),
      );
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (section.title != null) NavigationTitle(title: section.title!),
        Expanded(child: ItemGrid(items: section.items)),
      ],
    );
  }
}
