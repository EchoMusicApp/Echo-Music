import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:just_audio/just_audio.dart';

import '../../playback/media_metadata.dart';
import '../../playback/player_controller.dart';
import '../components/thumbnail.dart';
import '../shell/floating_nav_bar.dart';

/// Floating glass mini player docked above the nav bar.
class MiniPlayer extends StatelessWidget {
  final double height;
  final VoidCallback onTap;
  final VoidCallback onDragUp;
  const MiniPlayer({
    super.key,
    required this.height,
    required this.onTap,
    required this.onDragUp,
  });

  @override
  Widget build(BuildContext context) {
    final handler = player.handler;
    final scheme = Theme.of(context).colorScheme;
    return GestureDetector(
      onTap: onTap,
      onVerticalDragEnd: (d) {
        if (d.primaryVelocity != null && d.primaryVelocity! < -200) onDragUp();
      },
      onHorizontalDragEnd: (d) {
        if (d.primaryVelocity == null) return;
        if (d.primaryVelocity! < -300) handler.skipToNext();
        if (d.primaryVelocity! > 300) handler.skipToPrevious();
      },
      child: GlassSurface(
        radius: 32,
        child: SizedBox(
          height: height,
          child: ValueListenableBuilder<MediaMetadata?>(
            valueListenable: handler.currentMetadata,
            builder: (context, meta, _) {
              if (meta == null) return const SizedBox.shrink();
              return Stack(
                children: [
                  // Progress hairline at the bottom.
                  Positioned(
                    left: 24,
                    right: 24,
                    bottom: 0,
                    child: StreamBuilder<Duration>(
                      stream: handler.player.positionStream,
                      builder: (context, snap) {
                        final pos = snap.data ?? Duration.zero;
                        final dur =
                            handler.effectiveDuration.value ??
                            Duration(
                              seconds: meta.duration > 0 ? meta.duration : 1,
                            );
                        final v = dur.inMilliseconds == 0
                            ? 0.0
                            : (pos.inMilliseconds / dur.inMilliseconds).clamp(
                                0.0,
                                1.0,
                              );
                        return ClipRRect(
                          borderRadius: BorderRadius.circular(2),
                          child: LinearProgressIndicator(
                            value: v,
                            minHeight: 2,
                            backgroundColor: scheme.onSurface.withValues(
                              alpha: 0.08,
                            ),
                            color: scheme.primary,
                          ),
                        );
                      },
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 10),
                    child: Row(
                      children: [
                        EchoImage(
                          url: meta.thumbnailUrl,
                          width: 46,
                          height: 46,
                          radius: 23,
                          resize: 224,
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                meta.title,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: Theme.of(context).textTheme.bodyLarge
                                    ?.copyWith(fontWeight: FontWeight.w700),
                              ),
                              Text(
                                meta.artistsText,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: Theme.of(context).textTheme.bodySmall
                                    ?.copyWith(color: scheme.onSurfaceVariant),
                              ),
                            ],
                          ),
                        ),
                        IconButton(
                          onPressed: handler.skipToPrevious,
                          icon: const Icon(Icons.skip_previous_rounded),
                          visualDensity: VisualDensity.compact,
                        ),
                        _PlayPauseButton(handler: handler),
                        IconButton(
                          onPressed: handler.skipToNext,
                          icon: const Icon(Icons.skip_next_rounded),
                          visualDensity: VisualDensity.compact,
                        ),
                      ],
                    ),
                  ),
                ],
              );
            },
          ),
        ),
      ),
    );
  }
}

class _PlayPauseButton extends StatelessWidget {
  final dynamic handler;
  const _PlayPauseButton({required this.handler});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final AudioPlayer p = handler.player;
    return StreamBuilder<PlayerState>(
      stream: p.playerStateStream,
      initialData: p.playerState,
      builder: (context, snap) {
        final state = snap.data;
        final playing = state?.playing ?? false;
        final loading =
            state?.processingState == ProcessingState.loading ||
            state?.processingState == ProcessingState.buffering;
        return SizedBox(
          width: 46,
          height: 46,
          child: Material(
            color: scheme.onSurface,
            shape: const _ScallopBorder(),
            child: InkWell(
              customBorder: const CircleBorder(),
              onTap: () => playing ? handler.pause() : handler.play(),
              child: loading && !playing
                  ? Padding(
                      padding: const EdgeInsets.all(13),
                      child: CircularProgressIndicator(
                        strokeWidth: 2.5,
                        color: scheme.surface,
                      ),
                    )
                  : Icon(
                      playing ? Icons.pause_rounded : Icons.play_arrow_rounded,
                      color: scheme.surface,
                    ),
            ),
          ),
        );
      },
    );
  }
}

/// The scalloped "cookie" shape used by the Android app's play button.
class _ScallopBorder extends ShapeBorder {
  const _ScallopBorder();

  @override
  EdgeInsetsGeometry get dimensions => EdgeInsets.zero;

  @override
  Path getInnerPath(Rect rect, {TextDirection? textDirection}) =>
      getOuterPath(rect);

  @override
  Path getOuterPath(Rect rect, {TextDirection? textDirection}) =>
      scallopPath(rect, 8, 0.08);

  @override
  void paint(Canvas canvas, Rect rect, {TextDirection? textDirection}) {}

  @override
  ShapeBorder scale(double t) => this;
}

Path scallopPath(Rect rect, int bumps, double depth) {
  final path = Path();
  final c = rect.center;
  final r = rect.shortestSide / 2;
  const steps = 240;
  for (var i = 0; i <= steps; i++) {
    final t = i / steps * 2 * math.pi;
    final rr = r * (1 - depth + depth * (0.5 + 0.5 * math.cos(bumps * t)));
    final x = c.dx + rr * math.cos(t);
    final y = c.dy + rr * math.sin(t);
    if (i == 0) {
      path.moveTo(x, y);
    } else {
      path.lineTo(x, y);
    }
  }
  path.close();
  return path;
}
