import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:palette_generator/palette_generator.dart';

import '../../core/utils.dart';

/// Extracts gradient colours from album art (port of `PlayerColorExtractor`
/// / `extractGradientColors`). Results are cached per URL.
class PlayerPalette {
  PlayerPalette._();
  static final _cache = <String, List<Color>>{};

  static List<Color> fallback(ColorScheme scheme) => [
    scheme.surfaceContainerHighest,
    scheme.surface,
  ];

  static Future<List<Color>> extract(
    String? url, {
    required ColorScheme scheme,
  }) async {
    if (url == null || url.isEmpty) return fallback(scheme);
    final key = resizeThumbnail(url, 160);
    final cached = _cache[key];
    if (cached != null) return cached;
    try {
      final palette = await PaletteGenerator.fromImageProvider(
        CachedNetworkImageProvider(key),
        maximumColorCount: 16,
        size: const Size(80, 80),
      );
      final candidates = <Color>[
        if (palette.vibrantColor != null) palette.vibrantColor!.color,
        if (palette.darkVibrantColor != null) palette.darkVibrantColor!.color,
        if (palette.mutedColor != null) palette.mutedColor!.color,
        if (palette.darkMutedColor != null) palette.darkMutedColor!.color,
        if (palette.dominantColor != null) palette.dominantColor!.color,
      ];
      if (candidates.isEmpty) return fallback(scheme);
      candidates.sort(
        (a, b) => b.computeLuminance().compareTo(a.computeLuminance()),
      );
      final colors = candidates.length >= 2
          ? [candidates.first, candidates.last]
          : [candidates.first, Colors.black];
      _cache[key] = colors;
      return colors;
    } catch (_) {
      return fallback(scheme);
    }
  }
}
