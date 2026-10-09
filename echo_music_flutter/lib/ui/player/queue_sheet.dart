import 'package:flutter/material.dart';

import '../../core/utils.dart';
import '../../playback/media_metadata.dart';
import '../../playback/player_controller.dart';
import '../components/items.dart';
import '../components/menus.dart';

/// Reorderable queue bottom sheet (port of `Queue.kt`).
Future<void> showQueueSheet(BuildContext context) {
  return showModalBottomSheet<void>(
    context: context,
    useRootNavigator: true,
    isScrollControlled: true,
    builder: (ctx) => DraggableScrollableSheet(
      initialChildSize: 0.75,
      minChildSize: 0.4,
      maxChildSize: 0.95,
      expand: false,
      builder: (ctx, controller) => const _QueueBody(),
    ),
  );
}

class _QueueBody extends StatelessWidget {
  const _QueueBody();

  @override
  Widget build(BuildContext context) {
    final handler = player.handler;
    final theme = Theme.of(context);
    return ValueListenableBuilder<List<MediaMetadata>>(
      valueListenable: handler.queueItems,
      builder: (context, items, _) => ValueListenableBuilder<int>(
        valueListenable: handler.currentIndex,
        builder: (context, index, _) {
          final total = items.fold<int>(
            0,
            (a, m) => a + (m.duration > 0 ? m.duration : 0),
          );
          return Column(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 0, 12, 4),
                child: Row(
                  children: [
                    Expanded(
                      child: ValueListenableBuilder<String?>(
                        valueListenable: handler.queueTitleNotifier,
                        builder: (context, title, _) => Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              title ?? 'Queue',
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: theme.textTheme.titleLarge,
                            ),
                            Text(
                              '${items.length} songs • ${makeTimeString(total)}',
                              style: theme.textTheme.bodySmall?.copyWith(
                                color: theme.colorScheme.onSurfaceVariant,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                    IconButton(
                      tooltip: 'Add to playlist',
                      onPressed: () => showAddToPlaylistSheet(context, items),
                      icon: const Icon(Icons.playlist_add_rounded),
                    ),
                    IconButton(
                      tooltip: 'Clear queue',
                      onPressed: () async {
                        if (await showConfirmDialog(
                          context,
                          'Clear the queue?',
                          confirm: 'Clear',
                        )) {
                          await handler.clearQueue();
                          if (context.mounted) Navigator.of(context).pop();
                        }
                      },
                      icon: const Icon(Icons.clear_all_rounded),
                    ),
                  ],
                ),
              ),
              const Divider(),
              Expanded(
                child: ReorderableListView.builder(
                  itemCount: items.length,
                  buildDefaultDragHandles: false,
                  // onReorderItem already accounts for the removed item.
                  onReorderItem: handler.moveInQueue,
                  itemBuilder: (context, i) {
                    final m = items[i];
                    return Dismissible(
                      key: ValueKey('q-$i-${m.id}'),
                      direction: DismissDirection.endToStart,
                      background: Container(
                        color: theme.colorScheme.errorContainer,
                        alignment: Alignment.centerRight,
                        padding: const EdgeInsets.only(right: 24),
                        child: Icon(
                          Icons.delete_outline_rounded,
                          color: theme.colorScheme.onErrorContainer,
                        ),
                      ),
                      onDismissed: (_) => handler.removeFromQueue(i),
                      child: NowPlayingAware(
                        id: m.id,
                        builder: (context, active, playing) => MediaListTile(
                          title: m.title,
                          subtitle: joinByBullet([
                            m.artistsText,
                            if (m.duration > 0) formatDuration(m.duration),
                          ]),
                          thumbnailUrl: m.thumbnailUrl,
                          isActive: i == index,
                          isPlaying: i == index && playing,
                          explicit: m.explicit,
                          onTap: () => handler.skipToQueueItem(i),
                          onMore: () => showSongMenu(context, m.toSongItem()),
                          trailing: ReorderableDragStartListener(
                            index: i,
                            child: const Padding(
                              padding: EdgeInsets.symmetric(horizontal: 8),
                              child: Icon(Icons.drag_handle_rounded),
                            ),
                          ),
                        ),
                      ),
                    );
                  },
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
