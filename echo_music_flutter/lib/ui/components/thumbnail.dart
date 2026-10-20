import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';

import '../../core/utils.dart';

/// Network image with YouTube thumbnail resizing and a neutral placeholder.
class EchoImage extends StatelessWidget {
  final String? url;
  final double? width;
  final double? height;
  final double radius;
  final BoxFit fit;
  final bool circle;
  final int resize;

  const EchoImage({
    super.key,
    required this.url,
    this.width,
    this.height,
    this.radius = 12,
    this.fit = BoxFit.cover,
    this.circle = false,
    this.resize = 544,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final placeholder = Container(
      width: width,
      height: height,
      color: scheme.surfaceContainerHighest,
      child: Icon(
        Icons.music_note_rounded,
        color: scheme.onSurfaceVariant.withValues(alpha: 0.4),
      ),
    );
    Widget child;
    final u = url;
    if (u == null || u.isEmpty) {
      child = placeholder;
    } else {
      child = CachedNetworkImage(
        imageUrl: resizeThumbnail(u, resize),
        width: width,
        height: height,
        fit: fit,
        fadeInDuration: const Duration(milliseconds: 200),
        placeholder: (_, _) => placeholder,
        errorWidget: (_, _, _) => placeholder,
      );
    }
    if (circle) return ClipOval(child: child);
    return ClipRRect(borderRadius: BorderRadius.circular(radius), child: child);
  }
}

/// Thumbnail with the "now playing" overlay (port of `ItemThumbnail`).
class ItemThumbnail extends StatelessWidget {
  final String? url;
  final double size;
  final double radius;
  final bool isActive;
  final bool isPlaying;
  final int? index;
  final bool circle;

  const ItemThumbnail({
    super.key,
    required this.url,
    this.size = 48,
    this.radius = 8,
    this.isActive = false,
    this.isPlaying = false,
    this.index,
    this.circle = false,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    if (index != null && url == null) {
      return SizedBox(
        width: size,
        height: size,
        child: Center(
          child: isActive
              ? PlayingIndicator(color: scheme.primary, playing: isPlaying)
              : Text('$index', style: Theme.of(context).textTheme.labelLarge),
        ),
      );
    }
    return SizedBox(
      width: size,
      height: size,
      child: Stack(
        fit: StackFit.expand,
        children: [
          EchoImage(
            url: url,
            width: size,
            height: size,
            radius: radius,
            circle: circle,
            resize: 224,
          ),
          if (isActive)
            Container(
              decoration: BoxDecoration(
                color: Colors.black.withValues(alpha: 0.55),
                borderRadius: circle ? null : BorderRadius.circular(radius),
                shape: circle ? BoxShape.circle : BoxShape.rectangle,
              ),
              child: Center(
                child: PlayingIndicator(
                  color: Colors.white,
                  playing: isPlaying,
                ),
              ),
            ),
        ],
      ),
    );
  }
}

/// Three animated bars (port of `PlayingIndicator`).
class PlayingIndicator extends StatefulWidget {
  final Color color;
  final bool playing;
  final double barWidth;
  final double height;
  const PlayingIndicator({
    super.key,
    required this.color,
    required this.playing,
    this.barWidth = 3,
    this.height = 18,
  });

  @override
  State<PlayingIndicator> createState() => _PlayingIndicatorState();
}

class _PlayingIndicatorState extends State<PlayingIndicator>
    with TickerProviderStateMixin {
  late final List<AnimationController> _controllers;

  @override
  void initState() {
    super.initState();
    _controllers = List.generate(3, (i) {
      final c = AnimationController(
        vsync: this,
        duration: Duration(milliseconds: 420 + i * 140),
        lowerBound: 0.2,
        upperBound: 1,
      );
      if (widget.playing) c.repeat(reverse: true);
      return c;
    });
  }

  @override
  void didUpdateWidget(covariant PlayingIndicator old) {
    super.didUpdateWidget(old);
    if (old.playing != widget.playing) {
      for (final c in _controllers) {
        if (widget.playing) {
          c.repeat(reverse: true);
        } else {
          c.animateTo(0.3, duration: const Duration(milliseconds: 200));
        }
      }
    }
  }

  @override
  void dispose() {
    for (final c in _controllers) {
      c.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: widget.height,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          for (var i = 0; i < 3; i++) ...[
            AnimatedBuilder(
              animation: _controllers[i],
              builder: (_, _) => Container(
                width: widget.barWidth,
                height: widget.height * _controllers[i].value,
                decoration: BoxDecoration(
                  color: widget.color,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            if (i < 2) SizedBox(width: widget.barWidth * 0.8),
          ],
        ],
      ),
    );
  }
}
