import 'package:flutter/material.dart';

import '../../core/utils.dart';
import '../../data/database.dart';
import '../../data/settings.dart';
import '../../innertube/pages/pages.dart';
import '../../innertube/youtube.dart';
import '../../playback/player_controller.dart';
import '../components/common.dart';
import '../components/items.dart';
import '../components/menus.dart';
import '../components/watch_builder.dart';

/// Listening history — device history grouped by day, plus the YouTube
/// Music history when logged in (port of `HistoryScreen.kt`).
class HistoryScreen extends StatefulWidget {
  const HistoryScreen({super.key});

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  bool _remote = false;
  HistoryPage? _remotePage;
  String? _remoteError;

  Future<void> _loadRemote() async {
    try {
      final p = await YouTube.instance.musicHistory();
      if (mounted) setState(() => _remotePage = p);
    } catch (e) {
      if (mounted) setState(() => _remoteError = '$e');
    }
  }

  @override
  Widget build(BuildContext context) {
    final db = AppDatabase.instance;
    final loggedIn = Settings.instance.isLoggedIn;
    return Scaffold(
      appBar: AppBar(
        title: const Text('History'),
        actions: [
          if (!_remote)
            IconButton(
              onPressed: () async {
                if (await showConfirmDialog(
                  context,
                  'Clear all listening history?',
                  confirm: 'Clear',
                )) {
                  await db.clearHistory();
                }
              },
              icon: const Icon(Icons.delete_sweep_outlined),
            ),
        ],
      ),
      body: Column(
        children: [
          if (loggedIn)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 4, 16, 4),
              child: SegmentedButton<bool>(
                segments: const [
                  ButtonSegment(
                    value: false,
                    label: Text('This device'),
                    icon: Icon(Icons.phone_iphone_rounded),
                  ),
                  ButtonSegment(
                    value: true,
                    label: Text('YouTube Music'),
                    icon: Icon(Icons.cloud_outlined),
                  ),
                ],
                selected: {_remote},
                onSelectionChanged: (s) {
                  setState(() => _remote = s.first);
                  if (_remote && _remotePage == null) _loadRemote();
                },
              ),
            ),
          Expanded(child: _remote ? _remoteBody() : _localBody(db)),
        ],
      ),
    );
  }

  Widget _localBody(AppDatabase db) {
    return WatchBuilder<List<EventWithSong>>(
      query: () => db.events(),
      builder: (context, data) {
        final events = data ?? const <EventWithSong>[];
        if (events.isEmpty) {
          return const EmptyPlaceholder(
            icon: Icons.history_rounded,
            text: 'Nothing played yet.',
          );
        }
        final groups = <String, List<EventWithSong>>{};
        for (final e in events) {
          groups.putIfAbsent(relativeDay(e.timestamp), () => []).add(e);
        }
        final keys = groups.keys.toList();
        return ListView.builder(
          padding: EdgeInsets.fromLTRB(
            4,
            0,
            4,
            MediaQuery.paddingOf(context).bottom + 16,
          ),
          itemCount: keys.length,
          itemBuilder: (context, i) {
            final list = groups[keys[i]]!;
            final seen = <String>{};
            final unique = list.where((e) => seen.add(e.song.id)).toList();
            final songs = unique.map((e) => e.song).toList();
            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                NavigationTitle(
                  title: keys[i],
                  onPlayAll: () => player.playLocalList(
                    songs,
                    title: 'History • ${keys[i]}',
                  ),
                ),
                for (final e in unique)
                  NowPlayingAware(
                    id: e.song.id,
                    builder: (context, active, playing) => MediaListTile(
                      title: e.song.title,
                      subtitle: e.song.artistsText,
                      thumbnailUrl: e.song.thumbnailUrl,
                      isActive: active,
                      isPlaying: playing,
                      explicit: e.song.song.explicit,
                      liked: e.song.song.liked,
                      onTap: () => player.playLocal(
                        e.song,
                        context: songs,
                        title: 'History',
                      ),
                      onMore: () => showSongMenu(
                        context,
                        e.song.toSongItem(),
                        local: e.song,
                        eventId: e.id,
                      ),
                    ),
                  ),
              ],
            );
          },
        );
      },
    );
  }

  Widget _remoteBody() {
    if (_remoteError != null) {
      return ErrorPlaceholder(message: _remoteError!, onRetry: _loadRemote);
    }
    final page = _remotePage;
    if (page == null) return const ListShimmer();
    if (page.sections.isEmpty) {
      return const EmptyPlaceholder(
        icon: Icons.history_rounded,
        text: 'No YouTube Music history.',
      );
    }
    return ListView.builder(
      padding: EdgeInsets.fromLTRB(
        4,
        0,
        4,
        MediaQuery.paddingOf(context).bottom + 16,
      ),
      itemCount: page.sections.length,
      itemBuilder: (context, i) {
        final s = page.sections[i];
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            NavigationTitle(
              title: s.title,
              onPlayAll: () => player.playSongItems(s.songs, title: 'History'),
            ),
            for (final song in s.songs)
              YTItemTile(
                item: song,
                songContext: s.songs,
                contextTitle: 'History',
              ),
          ],
        );
      },
    );
  }
}
