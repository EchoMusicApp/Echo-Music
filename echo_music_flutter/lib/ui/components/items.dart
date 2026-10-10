import 'package:flutter/material.dart';

import '../../core/theme.dart';
import '../../core/utils.dart';
import '../../data/database.dart';
import '../../innertube/models/yt_item.dart';
import '../../playback/media_metadata.dart';
import '../../playback/player_controller.dart';
import '../shell/app_navigator.dart';
import 'common.dart';
import 'menus.dart';
import 'thumbnail.dart';

/// Base list row: 48dp thumbnail, title, subtitle, optional index/trailing.
class MediaListTile extends StatelessWidget {
  final String title;
  final String? subtitle;
  final String? thumbnailUrl;
  final VoidCallback? onTap;
  final VoidCallback? onLongPress;
  final VoidCallback? onMore;
  final Widget? trailing;
  final bool isActive;
  final bool isPlaying;
  final int? index;
  final bool circle;
  final bool explicit;
  final bool liked;
  final bool downloaded;
  final double thumbnailSize;
  final Color? color;
  final double radius;

  const MediaListTile({
    super.key,
    required this.title,
    this.subtitle,
    this.thumbnailUrl,
    this.onTap,
    this.onLongPress,
    this.onMore,
    this.trailing,
    this.isActive = false,
    this.isPlaying = false,
    this.index,
    this.circle = false,
    this.explicit = false,
    this.liked = false,
    this.downloaded = false,
    this.thumbnailSize = 48,
    this.color,
    this.radius = 16,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return Material(
      color: color ?? Colors.transparent,
      borderRadius: BorderRadius.circular(radius),
      child: InkWell(
        onTap: onTap,
        onLongPress: onLongPress ?? onMore,
        borderRadius: BorderRadius.circular(radius),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          child: Row(
            children: [
              if (index != null) ...[
                SizedBox(
                  width: 28,
                  child: Center(
                    child: isActive
                        ? PlayingIndicator(
                            color: scheme.primary,
                            playing: isPlaying,
                            height: 16,
                          )
                        : Text(
                            '$index',
                            style: theme.textTheme.labelLarge?.copyWith(
                              color: scheme.onSurfaceVariant,
                            ),
                          ),
                  ),
                ),
                const SizedBox(width: 8),
              ],
              ItemThumbnail(
                url: thumbnailUrl,
                size: thumbnailSize,
                radius: 8,
                isActive: isActive && index == null,
                isPlaying: isPlaying,
                circle: circle,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      title,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodyLarge?.copyWith(
                        fontWeight: FontWeight.w600,
                        color: isActive ? scheme.primary : scheme.onSurface,
                      ),
                    ),
                    if (subtitle != null && subtitle!.isNotEmpty ||
                        explicit ||
                        liked ||
                        downloaded)
                      Padding(
                        padding: const EdgeInsets.only(top: 2),
                        child: Row(
                          children: [
                            if (liked) ...[
                              Icon(
                                Icons.favorite_rounded,
                                size: 14,
                                color: scheme.error,
                              ),
                              const SizedBox(width: 4),
                            ],
                            if (downloaded) ...[
                              Icon(
                                Icons.offline_pin_rounded,
                                size: 14,
                                color: scheme.onSurfaceVariant,
                              ),
                              const SizedBox(width: 4),
                            ],
                            if (explicit) ...[
                              Icon(
                                Icons.explicit_rounded,
                                size: 14,
                                color: scheme.onSurfaceVariant,
                              ),
                              const SizedBox(width: 4),
                            ],
                            Expanded(
                              child: Text(
                                subtitle ?? '',
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: theme.textTheme.bodyMedium?.copyWith(
                                  color: scheme.onSurfaceVariant,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                  ],
                ),
              ),
              ?trailing,
              if (onMore != null)
                IconButton(
                  onPressed: onMore,
                  icon: const Icon(Icons.more_vert_rounded),
                  visualDensity: VisualDensity.compact,
                ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Reactive "is this the current song and is it playing" wrapper.
class NowPlayingAware extends StatelessWidget {
  final String id;
  final Widget Function(BuildContext context, bool isActive, bool isPlaying)
  builder;
  const NowPlayingAware({super.key, required this.id, required this.builder});

  @override
  Widget build(BuildContext context) {
    final handler = player.handler;
    return ValueListenableBuilder<MediaMetadata?>(
      valueListenable: handler.currentMetadata,
      builder: (context, meta, _) {
        final active = meta?.id == id;
        if (!active) return builder(context, false, false);
        return StreamBuilder<bool>(
          stream: handler.player.playingStream,
          initialData: handler.player.playing,
          builder: (context, snap) =>
              builder(context, true, snap.data ?? false),
        );
      },
    );
  }
}

/// Row for any InnerTube item with default tap + long-press behaviour.
class YTItemTile extends StatelessWidget {
  final YTItem item;
  final List<SongItem>? songContext;
  final String? contextTitle;
  final int? index;
  final Widget? trailing;
  final VoidCallback? onTap;
  final bool showMore;

  const YTItemTile({
    super.key,
    required this.item,
    this.songContext,
    this.contextTitle,
    this.index,
    this.trailing,
    this.onTap,
    this.showMore = true,
  });

  @override
  Widget build(BuildContext context) {
    final it = item;
    if (it is SongItem) {
      return NowPlayingAware(
        id: it.id,
        builder: (context, active, playing) => MediaListTile(
          title: it.title,
          subtitle: joinByBullet([
            it.artistsText,
            if (it.album != null && songContext == null) it.album!.name,
            if (it.duration != null) formatDuration(it.duration!),
          ]),
          thumbnailUrl: it.thumbnail,
          explicit: it.explicit,
          isActive: active,
          isPlaying: playing,
          index: index,
          onTap:
              onTap ??
              () => player.playSong(
                it,
                context: songContext,
                title: contextTitle,
              ),
          onMore: showMore ? () => showSongMenu(context, it) : null,
          trailing: trailing,
        ),
      );
    }
    if (it is AlbumItem) {
      return MediaListTile(
        title: it.title,
        subtitle: joinByBullet([
          'Album',
          it.artistsText,
          if (it.year != null) '${it.year}',
        ]),
        thumbnailUrl: it.thumbnail,
        explicit: it.explicit,
        onTap: onTap ?? () => AppNavigator.openAlbum(it.browseId, album: it),
        onMore: showMore ? () => showAlbumMenu(context, it) : null,
        trailing: trailing,
      );
    }
    if (it is ArtistItem) {
      return MediaListTile(
        title: it.title,
        subtitle: 'Artist',
        thumbnailUrl: it.thumbnail,
        circle: true,
        onTap: onTap ?? () => AppNavigator.openArtist(it.id),
        onMore: showMore ? () => showArtistMenu(context, it) : null,
        trailing: trailing,
      );
    }
    if (it is PlaylistItem) {
      return MediaListTile(
        title: it.title,
        subtitle: joinByBullet(['Playlist', it.author?.name, it.songCountText]),
        thumbnailUrl: it.thumbnail,
        onTap: onTap ?? () => AppNavigator.openPlaylist(it.id, playlist: it),
        onMore: showMore ? () => showPlaylistMenu(context, it) : null,
        trailing: trailing,
      );
    }
    return const SizedBox.shrink();
  }
}

/// Row for a song stored in the local database.
class LocalSongTile extends StatelessWidget {
  final Song song;
  final List<Song>? songContext;
  final String? contextTitle;
  final int? index;
  final String? playlistId;
  final Widget? trailing;
  final VoidCallback? onTap;
  final bool showLiked;
  final bool downloaded;

  const LocalSongTile({
    super.key,
    required this.song,
    this.songContext,
    this.contextTitle,
    this.index,
    this.playlistId,
    this.trailing,
    this.onTap,
    this.showLiked = true,
    this.downloaded = false,
  });

  @override
  Widget build(BuildContext context) {
    return NowPlayingAware(
      id: song.id,
      builder: (context, active, playing) => MediaListTile(
        title: song.title,
        subtitle: joinByBullet([
          song.artistsText,
          if (song.song.duration > 0) formatDuration(song.song.duration),
        ]),
        thumbnailUrl: song.thumbnailUrl,
        explicit: song.song.explicit,
        liked: showLiked && song.song.liked,
        downloaded: downloaded || song.song.dateDownload != null,
        isActive: active,
        isPlaying: playing,
        index: index,
        onTap:
            onTap ??
            () => player.playLocal(
              song,
              context: songContext,
              title: contextTitle,
            ),
        onMore: () => showSongMenu(
          context,
          song.toSongItem(),
          local: song,
          playlistId: playlistId,
        ),
        trailing: trailing,
      ),
    );
  }
}

/// Grid card (square art, title, subtitle) for carousels.
class YTGridItem extends StatelessWidget {
  final YTItem item;
  final double width;
  final VoidCallback? onTap;
  final String? subtitleOverride;

  const YTGridItem({
    super.key,
    required this.item,
    this.width = 150,
    this.onTap,
    this.subtitleOverride,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final it = item;
    final circle = it is ArtistItem;
    String subtitle;
    VoidCallback tap;
    VoidCallback more;
    if (it is SongItem) {
      subtitle = it.artistsText;
      tap = () => player.playSong(it);
      more = () => showSongMenu(context, it);
    } else if (it is AlbumItem) {
      subtitle = joinByBullet([
        it.artistsText.isNotEmpty ? it.artistsText : 'Album',
        if (it.year != null) '${it.year}',
      ]);
      tap = () => AppNavigator.openAlbum(it.browseId, album: it);
      more = () => showAlbumMenu(context, it);
    } else if (it is ArtistItem) {
      subtitle = 'Artist';
      tap = () => AppNavigator.openArtist(it.id);
      more = () => showArtistMenu(context, it);
    } else if (it is PlaylistItem) {
      subtitle = joinByBullet([
        it.author?.name ?? 'Playlist',
        it.songCountText,
      ]);
      tap = () => AppNavigator.openPlaylist(it.id, playlist: it);
      more = () => showPlaylistMenu(context, it);
    } else {
      return const SizedBox.shrink();
    }
    final sub = subtitleOverride ?? subtitle;
    final image = EchoImage(
      url: it.thumbnail,
      width: width,
      height: width,
      radius: 16,
      circle: circle,
    );
    Widget art = image;
    if (it is SongItem) {
      // Note: reference `image`, not `art` — closures capture variables, and
      // `art` is reassigned to this very widget (infinite recursion otherwise).
      art = NowPlayingAware(
        id: it.id,
        builder: (context, active, playing) => Stack(
          children: [
            image,
            if (active)
              Positioned.fill(
                child: Container(
                  decoration: BoxDecoration(
                    color: Colors.black.withValues(alpha: 0.5),
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Center(
                    child: PlayingIndicator(
                      color: Colors.white,
                      playing: playing,
                      height: 28,
                      barWidth: 5,
                    ),
                  ),
                ),
              ),
          ],
        ),
      );
    }
    return SizedBox(
      width: width,
      child: InkWell(
        onTap: onTap ?? tap,
        onLongPress: more,
        borderRadius: BorderRadius.circular(16),
        child: Column(
          crossAxisAlignment: circle
              ? CrossAxisAlignment.center
              : CrossAxisAlignment.start,
          children: [
            art,
            const SizedBox(height: 8),
            Text(
              it.title,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              textAlign: circle ? TextAlign.center : TextAlign.start,
              style: theme.textTheme.bodyMedium?.copyWith(
                fontWeight: FontWeight.w600,
              ),
            ),
            if (sub.isNotEmpty)
              Text(
                sub,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                textAlign: circle ? TextAlign.center : TextAlign.start,
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

/// Carousel of grid items with a section header.
class SectionCarousel extends StatelessWidget {
  final String title;
  final String? label;
  final List<YTItem> items;
  final VoidCallback? onMore;
  final VoidCallback? onPlayAll;
  final double itemWidth;

  const SectionCarousel({
    super.key,
    required this.title,
    required this.items,
    this.label,
    this.onMore,
    this.onPlayAll,
    this.itemWidth = 150,
  });

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) return const SizedBox.shrink();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        NavigationTitle(
          title: title,
          label: label,
          onTap: onMore,
          onPlayAll: onPlayAll,
        ),
        SizedBox(
          height: itemWidth + 52,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            itemCount: items.length,
            separatorBuilder: (_, _) => const SizedBox(width: 12),
            itemBuilder: (context, i) =>
                YTGridItem(item: items[i], width: itemWidth),
          ),
        ),
      ],
    );
  }
}

/// Song list paged horizontally in columns of [rows] rows (YouTube Music
/// "Quick picks" style).
class SongColumnsPager extends StatelessWidget {
  final List<SongItem> songs;
  final int rows;
  final String? contextTitle;
  const SongColumnsPager({
    super.key,
    required this.songs,
    this.rows = 4,
    this.contextTitle,
  });

  @override
  Widget build(BuildContext context) {
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
        itemCount: pages.length,
        physics: const PageScrollPhysics(),
        itemBuilder: (context, p) => SizedBox(
          width: width,
          child: Column(
            children: [
              for (final s in pages[p])
                SizedBox(
                  height: 64,
                  child: YTItemTile(
                    item: s,
                    songContext: songs,
                    contextTitle: contextTitle,
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Large square tile used by the Library quick actions (Liked, Downloaded…).
class LibraryTile extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  const LibraryTile({
    super.key,
    required this.icon,
    required this.label,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return EchoCard(
      onTap: onTap,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 18),
      radius: EchoTheme.cardRadius,
      child: Row(
        children: [
          Icon(icon, color: theme.colorScheme.onSurface),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: theme.textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
