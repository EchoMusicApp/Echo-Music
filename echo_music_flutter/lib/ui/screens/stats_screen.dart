import 'package:flutter/material.dart';

import '../../core/utils.dart';
import '../../data/database.dart';
import '../../innertube/models/yt_item.dart';
import '../../playback/player_controller.dart';
import '../components/common.dart';
import '../components/items.dart';
import '../components/menus.dart';
import '../shell/app_navigator.dart';

/// Listening stats (port of `StatsScreen.kt`).
class StatsScreen extends StatefulWidget {
  const StatsScreen({super.key});

  @override
  State<StatsScreen> createState() => _StatsScreenState();
}

class _StatsScreenState extends State<StatsScreen> {
  int _period = 0; // 0: week, 1: month, 2: year, 3: all

  DateTime? get _since => switch (_period) {
    0 => DateTime.now().subtract(const Duration(days: 7)),
    1 => DateTime.now().subtract(const Duration(days: 30)),
    2 => DateTime.now().subtract(const Duration(days: 365)),
    _ => null,
  };

  @override
  Widget build(BuildContext context) {
    final db = AppDatabase.instance;
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Stats')),
      body: ListView(
        padding: EdgeInsets.only(
          bottom: MediaQuery.paddingOf(context).bottom + 16,
        ),
        children: [
          ChipsRow(
            labels: const ['Week', 'Month', 'Year', 'All time'],
            selected: _period,
            onSelected: (i) => setState(() => _period = i ?? 3),
          ),
          FutureBuilder<Map<String, int>>(
            future: db.stats(),
            builder: (context, snap) {
              final s = snap.data ?? const {};
              return Padding(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                child: Row(
                  children: [
                    _Stat(
                      label: 'Listened',
                      value: makeTimeString((s['playTimeMs'] ?? 0) ~/ 1000),
                    ),
                    const SizedBox(width: 10),
                    _Stat(label: 'Songs', value: '${s['songs'] ?? 0}'),
                    const SizedBox(width: 10),
                    _Stat(label: 'Artists', value: '${s['artists'] ?? 0}'),
                  ],
                ),
              );
            },
          ),
          FutureBuilder<List<Song>>(
            future: db.mostPlayedSongs(limit: 20, since: _since),
            builder: (context, snap) {
              final songs = snap.data ?? const [];
              if (songs.isEmpty) {
                return const EmptyPlaceholder(
                  icon: Icons.insights_rounded,
                  text: 'No listening data for this period.',
                );
              }
              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  NavigationTitle(
                    title: 'Top songs',
                    onPlayAll: () =>
                        player.playLocalList(songs, title: 'Top songs'),
                  ),
                  for (final (i, s) in songs.indexed)
                    LocalSongTile(
                      song: s,
                      songContext: songs,
                      contextTitle: 'Top songs',
                      index: i + 1,
                    ),
                ],
              );
            },
          ),
          FutureBuilder<List<ArtistWithSongCount>>(
            future: db.mostPlayedArtists(limit: 10, since: _since),
            builder: (context, snap) {
              final artists = snap.data ?? const [];
              if (artists.isEmpty) return const SizedBox.shrink();
              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const NavigationTitle(title: 'Top artists'),
                  for (final a in artists)
                    MediaListTile(
                      title: a.artist.name,
                      subtitle: makeTimeString(a.songCount),
                      thumbnailUrl: a.artist.thumbnailUrl,
                      circle: true,
                      onTap: () => AppNavigator.openArtist(a.artist.id),
                      onMore: () => showArtistMenu(
                        context,
                        ArtistItem(
                          id: a.artist.id,
                          title: a.artist.name,
                          thumbnail: a.artist.thumbnailUrl,
                        ),
                      ),
                    ),
                ],
              );
            },
          ),
          const SizedBox(height: 8),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Text(
              'Stats are computed from playback on this device.',
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Stat extends StatelessWidget {
  final String label;
  final String value;
  const _Stat({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Expanded(
      child: EchoCard(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              value,
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w800,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            Text(
              label,
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
