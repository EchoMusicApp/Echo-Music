import 'dart:ui';

import 'package:audio_service/audio_service.dart';
import 'package:flutter/material.dart';
import 'package:just_audio/just_audio.dart';

import '../../core/utils.dart';
import '../../data/database.dart';
import '../../data/download_manager.dart';
import '../../data/settings.dart';
import '../../playback/media_metadata.dart';
import '../../playback/player_controller.dart';
import '../../stream/stream_resolver.dart';
import '../components/menus.dart';
import '../components/thumbnail.dart';
import '../shell/app_navigator.dart';
import 'lyrics_view.dart';
import 'player_palette.dart';
import 'queue_sheet.dart';
import '../components/watch_builder.dart';

/// Full-screen now-playing view — Apple-Music-inspired full-bleed artwork
/// with a blurred backdrop (port of `Player.kt` "new player design").
class FullPlayer extends StatefulWidget {
  const FullPlayer({super.key});

  @override
  State<FullPlayer> createState() => _FullPlayerState();
}

class _FullPlayerState extends State<FullPlayer> {
  bool _showLyrics = false;
  List<Color>? _colors;
  String? _paletteFor;
  double? _dragValue;

  bool _depsReady = false;

  @override
  void initState() {
    super.initState();
    _showLyrics = Settings.instance.showLyricsOnPlayer;
    AppNavigator.queueRequested.addListener(_openQueue);
    player.handler.currentMetadata.addListener(_onMeta);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!_depsReady) {
      _depsReady = true;
      _onMeta();
    }
  }

  @override
  void dispose() {
    AppNavigator.queueRequested.removeListener(_openQueue);
    player.handler.currentMetadata.removeListener(_onMeta);
    super.dispose();
  }

  void _openQueue() {
    if (mounted) showQueueSheet(context);
  }

  Future<void> _onMeta() async {
    if (!mounted || !_depsReady) return;
    final meta = player.handler.currentMetadata.value;
    final url = meta?.thumbnailUrl;
    if (url == _paletteFor) return;
    _paletteFor = url;
    final scheme = Theme.of(context).colorScheme;
    final colors = await PlayerPalette.extract(url, scheme: scheme);
    if (mounted && _paletteFor == url) setState(() => _colors = colors);
  }

  @override
  Widget build(BuildContext context) {
    final handler = player.handler;
    final scheme = Theme.of(context).colorScheme;
    final settings = Settings.instance;
    return ValueListenableBuilder<MediaMetadata?>(
      valueListenable: handler.currentMetadata,
      builder: (context, meta, _) {
        if (meta == null) return const SizedBox.shrink();
        final colors = _colors ?? PlayerPalette.fallback(scheme);
        final onBg = Colors.white;
        return Material(
          color: scheme.surface,
          child: Stack(
            fit: StackFit.expand,
            children: [
              _Background(
                url: meta.thumbnailUrl,
                colors: colors,
                style: settings.playerBackground,
              ),
              SafeArea(
                child: Column(
                  children: [
                    _TopBar(onBg: onBg, meta: meta),
                    Expanded(
                      child: AnimatedSwitcher(
                        duration: const Duration(milliseconds: 250),
                        child: _showLyrics
                            ? LyricsView(
                                key: ValueKey('lyrics-${meta.id}'),
                                meta: meta,
                                textColor: onBg,
                              )
                            : _Artwork(
                                key: ValueKey('art-${meta.id}'),
                                meta: meta,
                                hide: settings.hidePlayerThumbnail,
                              ),
                      ),
                    ),
                    _TitleRow(meta: meta, onBg: onBg),
                    const SizedBox(height: 12),
                    _Seekbar(
                      onBg: onBg,
                      meta: meta,
                      dragValue: _dragValue,
                      onDrag: (v) => setState(() => _dragValue = v),
                      onDragEnd: (v) {
                        setState(() => _dragValue = null);
                        handler.seek(v);
                      },
                    ),
                    const SizedBox(height: 8),
                    _Controls(onBg: onBg),
                    const SizedBox(height: 8),
                    _BottomRow(
                      onBg: onBg,
                      lyricsOn: _showLyrics,
                      onLyrics: () =>
                          setState(() => _showLyrics = !_showLyrics),
                      onQueue: () => showQueueSheet(context),
                    ),
                    const SizedBox(height: 12),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _Background extends StatelessWidget {
  final String? url;
  final List<Color> colors;
  final PlayerBackgroundStyle style;
  const _Background({
    required this.url,
    required this.colors,
    required this.style,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    if (style == PlayerBackgroundStyle.plain) {
      return Container(color: scheme.surface);
    }
    return Stack(
      fit: StackFit.expand,
      children: [
        if (style == PlayerBackgroundStyle.blur)
          ImageFiltered(
            imageFilter: ImageFilter.blur(
              sigmaX: 60,
              sigmaY: 60,
              tileMode: TileMode.mirror,
            ),
            child: EchoImage(url: url, radius: 0, resize: 320),
          )
        else
          AnimatedContainer(
            duration: const Duration(milliseconds: 600),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [colors.first, colors.last],
              ),
            ),
          ),
        Container(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
              colors: [
                Colors.black.withValues(alpha: 0.25),
                Colors.black.withValues(alpha: 0.55),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _TopBar extends StatelessWidget {
  final Color onBg;
  final MediaMetadata meta;
  const _TopBar({required this.onBg, required this.meta});

  @override
  Widget build(BuildContext context) {
    final handler = player.handler;
    return Padding(
      padding: const EdgeInsets.fromLTRB(8, 4, 8, 0),
      child: Row(
        children: [
          IconButton(
            onPressed: AppNavigator.closePlayer,
            icon: Icon(
              Icons.keyboard_arrow_down_rounded,
              color: onBg,
              size: 30,
            ),
          ),
          Expanded(
            child: ValueListenableBuilder<String?>(
              valueListenable: handler.queueTitleNotifier,
              builder: (context, title, _) => Column(
                children: [
                  Text(
                    'PLAYING FROM',
                    style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      color: onBg.withValues(alpha: 0.7),
                      letterSpacing: 1.2,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  Text(
                    title ?? 'Queue',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.labelLarge
                        ?.copyWith(color: onBg, fontWeight: FontWeight.w700),
                  ),
                ],
              ),
            ),
          ),
          IconButton(
            onPressed: () => showSongMenu(context, meta.toSongItem()),
            icon: Icon(Icons.more_horiz_rounded, color: onBg, size: 28),
          ),
        ],
      ),
    );
  }
}

class _Artwork extends StatelessWidget {
  final MediaMetadata meta;
  final bool hide;
  const _Artwork({super.key, required this.meta, required this.hide});

  @override
  Widget build(BuildContext context) {
    final handler = player.handler;
    return LayoutBuilder(
      builder: (context, c) {
        final size = (c.maxWidth - 48).clamp(120.0, c.maxHeight - 24);
        return GestureDetector(
          onHorizontalDragEnd: (d) {
            if (d.primaryVelocity == null) return;
            if (d.primaryVelocity! < -300) handler.skipToNext();
            if (d.primaryVelocity! > 300) handler.skipToPrevious();
          },
          onTap: () =>
              handler.player.playing ? handler.pause() : handler.play(),
          child: Center(
            child: ValueListenableBuilder<bool>(
              valueListenable: handler.isLoadingItem,
              builder: (context, loading, _) => Stack(
                alignment: Alignment.center,
                children: [
                  if (!hide)
                    Container(
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(20),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withValues(alpha: 0.45),
                            blurRadius: 40,
                            offset: const Offset(0, 16),
                          ),
                        ],
                      ),
                      child: EchoImage(
                        url: meta.thumbnailUrl,
                        width: size,
                        height: size,
                        radius: 20,
                        resize: 1080,
                      ),
                    ),
                  if (loading)
                    const SizedBox(
                      width: 42,
                      height: 42,
                      child: CircularProgressIndicator(
                        color: Colors.white,
                        strokeWidth: 3,
                      ),
                    ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

class _TitleRow extends StatelessWidget {
  final MediaMetadata meta;
  final Color onBg;
  const _TitleRow({required this.meta, required this.onBg});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  meta.title,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.titleLarge?.copyWith(
                    color: onBg,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 2),
                GestureDetector(
                  onTap: () {
                    final a = meta.artists.where((a) => a.id != null).toList();
                    if (a.isNotEmpty) {
                      AppNavigator.closePlayer();
                      AppNavigator.openArtist(a.first.id!);
                    }
                  },
                  child: Text(
                    meta.artistsText,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.bodyLarge?.copyWith(
                      color: onBg.withValues(alpha: 0.8),
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          _LikeButton(meta: meta, onBg: onBg),
        ],
      ),
    );
  }
}

class _LikeButton extends StatelessWidget {
  final MediaMetadata meta;
  final Color onBg;
  const _LikeButton({required this.meta, required this.onBg});

  @override
  Widget build(BuildContext context) {
    return WatchBuilder<bool>(
      watchKey: meta.id,
      query: () => AppDatabase.instance.isLiked(meta.id),
      builder: (context, data) {
        final liked = data ?? false;
        return SizedBox(
          width: 44,
          height: 44,
          child: Material(
            color: onBg.withValues(alpha: 0.15),
            shape: const CircleBorder(),
            child: InkWell(
              customBorder: const CircleBorder(),
              onTap: () => player.toggleLike(meta),
              child: Icon(
                liked ? Icons.favorite_rounded : Icons.favorite_border_rounded,
                color: liked ? const Color(0xFFFF5C7A) : onBg,
              ),
            ),
          ),
        );
      },
    );
  }
}

class _Seekbar extends StatelessWidget {
  final Color onBg;
  final MediaMetadata meta;
  final double? dragValue;
  final ValueChanged<double> onDrag;
  final ValueChanged<Duration> onDragEnd;
  const _Seekbar({
    required this.onBg,
    required this.meta,
    required this.dragValue,
    required this.onDrag,
    required this.onDragEnd,
  });

  @override
  Widget build(BuildContext context) {
    final handler = player.handler;
    return StreamBuilder<Duration>(
      stream: handler.player.positionStream,
      builder: (context, snap) {
        final dur =
            handler.effectiveDuration.value ??
            Duration(seconds: meta.duration > 0 ? meta.duration : 0);
        final pos = snap.data ?? Duration.zero;
        final total = dur.inMilliseconds;
        final v =
            dragValue ??
            (total == 0 ? 0.0 : (pos.inMilliseconds / total).clamp(0.0, 1.0));
        return Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Column(
            children: [
              SliderTheme(
                data: SliderTheme.of(context).copyWith(
                  trackHeight: 6,
                  activeTrackColor: onBg,
                  inactiveTrackColor: onBg.withValues(alpha: 0.25),
                  thumbColor: onBg,
                  thumbShape: const RoundSliderThumbShape(
                    enabledThumbRadius: 0,
                    disabledThumbRadius: 0,
                  ),
                  overlayShape: SliderComponentShape.noOverlay,
                  trackShape: const RoundedRectSliderTrackShape(),
                ),
                child: Slider(
                  value: v,
                  onChanged: total == 0 ? null : onDrag,
                  onChangeEnd: total == 0
                      ? null
                      : (x) => onDragEnd(
                          Duration(milliseconds: (x * total).round()),
                        ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 8),
                child: Row(
                  children: [
                    Text(
                      formatDurationMs(
                        dragValue != null
                            ? Duration(
                                milliseconds: (dragValue! * total).round(),
                              )
                            : pos,
                      ),
                      style: TextStyle(
                        color: onBg.withValues(alpha: 0.8),
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const Spacer(),
                    ValueListenableBuilder<ResolvedStream?>(
                      valueListenable: handler.currentStream,
                      builder: (context, s, _) => s == null
                          ? const SizedBox.shrink()
                          : Text(
                              '${s.mimeType.contains('webm') ? 'OPUS' : 'AAC'} ${s.bitrate ~/ 1000}k',
                              style: TextStyle(
                                color: onBg.withValues(alpha: 0.5),
                                fontSize: 10,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                    ),
                    const Spacer(),
                    Text(
                      formatDurationMs(dur),
                      style: TextStyle(
                        color: onBg.withValues(alpha: 0.8),
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _Controls extends StatelessWidget {
  final Color onBg;
  const _Controls({required this.onBg});

  @override
  Widget build(BuildContext context) {
    final handler = player.handler;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          ValueListenableBuilder<bool>(
            valueListenable: handler.shuffleEnabled,
            builder: (context, on, _) => IconButton(
              onPressed: handler.toggleShuffle,
              icon: Icon(
                Icons.shuffle_rounded,
                color: on ? onBg : onBg.withValues(alpha: 0.5),
              ),
            ),
          ),
          IconButton(
            onPressed: handler.skipToPrevious,
            iconSize: 44,
            icon: Icon(Icons.skip_previous_rounded, color: onBg),
          ),
          StreamBuilder<PlayerState>(
            stream: handler.player.playerStateStream,
            initialData: handler.player.playerState,
            builder: (context, snap) {
              final playing = snap.data?.playing ?? false;
              final buffering =
                  snap.data?.processingState == ProcessingState.buffering ||
                  snap.data?.processingState == ProcessingState.loading;
              return SizedBox(
                width: 76,
                height: 76,
                child: Material(
                  color: onBg,
                  shape: const CircleBorder(),
                  child: InkWell(
                    customBorder: const CircleBorder(),
                    onTap: () => playing ? handler.pause() : handler.play(),
                    child: buffering && !playing
                        ? const Padding(
                            padding: EdgeInsets.all(24),
                            child: CircularProgressIndicator(
                              strokeWidth: 3,
                              color: Colors.black,
                            ),
                          )
                        : Icon(
                            playing
                                ? Icons.pause_rounded
                                : Icons.play_arrow_rounded,
                            size: 44,
                            color: Colors.black,
                          ),
                  ),
                ),
              );
            },
          ),
          IconButton(
            onPressed: handler.skipToNext,
            iconSize: 44,
            icon: Icon(Icons.skip_next_rounded, color: onBg),
          ),
          ValueListenableBuilder<AudioServiceRepeatMode>(
            valueListenable: handler.repeatMode,
            builder: (context, mode, _) => IconButton(
              onPressed: handler.toggleRepeat,
              icon: Icon(
                mode == AudioServiceRepeatMode.one
                    ? Icons.repeat_one_rounded
                    : Icons.repeat_rounded,
                color: mode == AudioServiceRepeatMode.none
                    ? onBg.withValues(alpha: 0.5)
                    : onBg,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _BottomRow extends StatelessWidget {
  final Color onBg;
  final bool lyricsOn;
  final VoidCallback onLyrics;
  final VoidCallback onQueue;
  const _BottomRow({
    required this.onBg,
    required this.lyricsOn,
    required this.onLyrics,
    required this.onQueue,
  });

  @override
  Widget build(BuildContext context) {
    final handler = player.handler;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Column(
        children: [
          ValueListenableBuilder<String?>(
            valueListenable: handler.error,
            builder: (context, err, _) => err == null
                ? const SizedBox.shrink()
                : Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: Row(
                      children: [
                        Icon(
                          Icons.error_outline_rounded,
                          color: Colors.orangeAccent,
                          size: 16,
                        ),
                        const SizedBox(width: 6),
                        Expanded(
                          child: Text(
                            err,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              color: onBg.withValues(alpha: 0.85),
                              fontSize: 12,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
          ),
          Row(
            children: [
              Expanded(
                child: _PillButton(
                  icon: Icons.lyrics_rounded,
                  label: 'Lyrics',
                  active: lyricsOn,
                  onBg: onBg,
                  onTap: onLyrics,
                ),
              ),
              const SizedBox(width: 6),
              Expanded(
                child: ValueListenableBuilder<DateTime?>(
                  valueListenable: handler.sleepTimerEnd,
                  builder: (context, end, _) => ValueListenableBuilder<bool>(
                    valueListenable: handler.sleepAtEndOfSong,
                    builder: (context, atEnd, _) => _PillButton(
                      icon: Icons.bedtime_rounded,
                      label: end != null
                          ? '${(end.difference(DateTime.now()).inMinutes + 1).clamp(0, 999)} min'
                          : atEnd
                          ? 'End of song'
                          : 'Sleep',
                      active: end != null || atEnd,
                      onBg: onBg,
                      onTap: () => _showSleepTimer(context),
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 6),
              Expanded(child: _DownloadButton(onBg: onBg)),
              const SizedBox(width: 6),
              Expanded(
                child: _PillButton(
                  icon: Icons.queue_music_rounded,
                  label: 'Queue',
                  active: false,
                  onBg: onBg,
                  onTap: onQueue,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  void _showSleepTimer(BuildContext context) {
    final handler = player.handler;
    showModalBottomSheet<void>(
      context: context,
      useRootNavigator: true,
      builder: (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 0, 20, 8),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  'Sleep timer',
                  style: Theme.of(ctx).textTheme.titleLarge,
                ),
              ),
            ),
            Flexible(
              child: ListView(
                shrinkWrap: true,
                padding: EdgeInsets.zero,
                children: [
                  for (final m in [5, 10, 15, 30, 45, 60, 90])
                    ListTile(
                      leading: const Icon(Icons.timer_outlined),
                      title: Text('$m minutes'),
                      onTap: () {
                        handler.startSleepTimer(Duration(minutes: m));
                        Navigator.of(ctx).pop();
                      },
                    ),
                  ListTile(
                    leading: const Icon(Icons.music_off_rounded),
                    title: const Text('End of song'),
                    onTap: () {
                      handler.sleepAtEnd();
                      Navigator.of(ctx).pop();
                    },
                  ),
                  if (handler.sleepTimerEnd.value != null ||
                      handler.sleepAtEndOfSong.value)
                    ListTile(
                      leading: const Icon(Icons.close_rounded),
                      title: const Text('Cancel timer'),
                      onTap: () {
                        handler.cancelSleepTimer();
                        Navigator.of(ctx).pop();
                      },
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _DownloadButton extends StatelessWidget {
  final Color onBg;
  const _DownloadButton({required this.onBg});

  @override
  Widget build(BuildContext context) {
    final handler = player.handler;
    return ValueListenableBuilder<MediaMetadata?>(
      valueListenable: handler.currentMetadata,
      builder: (context, meta, _) => ListenableBuilder(
        listenable: DownloadManager.instance,
        builder: (context, _) {
          final id = meta?.id;
          final state = id == null
              ? null
              : DownloadManager.instance.stateOf(id);
          final done = state?.state == DownloadState.completed;
          final busy =
              state?.state == DownloadState.downloading ||
              state?.state == DownloadState.queued;
          return _PillButton(
            icon: done
                ? Icons.offline_pin_rounded
                : busy
                ? Icons.downloading_rounded
                : Icons.download_rounded,
            label: done
                ? 'Saved'
                : busy
                ? '${((state?.progress ?? 0) * 100).round()}%'
                : 'Save',
            active: done,
            onBg: onBg,
            onTap: () {
              if (meta == null || meta.isLocal) return;
              if (done) {
                DownloadManager.instance.remove(meta.id);
              } else if (!busy) {
                DownloadManager.instance.download(meta);
              }
            },
          );
        },
      ),
    );
  }
}

class _PillButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool active;
  final Color onBg;
  final VoidCallback onTap;
  const _PillButton({
    required this.icon,
    required this.label,
    required this.active,
    required this.onBg,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 38,
      child: Material(
        color: active ? onBg : onBg.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(20),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(20),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 10),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(icon, size: 16, color: active ? Colors.black : onBg),
                const SizedBox(width: 4),
                Flexible(
                  child: Text(
                    label,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      color: active ? Colors.black : onBg,
                      fontSize: 11,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
