import 'package:flutter/material.dart';
import 'package:shimmer/shimmer.dart';

import '../../core/theme.dart';

/// Uppercase bold section header with optional "Play all" (port of
/// `NavigationTitle`).
class NavigationTitle extends StatelessWidget {
  final String title;
  final String? label;
  final VoidCallback? onTap;
  final VoidCallback? onPlayAll;
  final Widget? thumbnail;

  const NavigationTitle({
    super.key,
    required this.title,
    this.label,
    this.onTap,
    this.onPlayAll,
    this.thumbnail,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          children: [
            if (thumbnail != null) ...[thumbnail!, const SizedBox(width: 12)],
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (label != null)
                    Text(
                      label!,
                      style: theme.textTheme.labelLarge,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  Text(
                    title.toUpperCase(),
                    style: theme.textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w800,
                      color: theme.colorScheme.onSurfaceVariant,
                      letterSpacing: 0.4,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
            if (onPlayAll != null)
              SizedBox(
                height: 28,
                child: OutlinedButton(
                  onPressed: onPlayAll,
                  style: OutlinedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(horizontal: 12),
                    visualDensity: VisualDensity.compact,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                    side: BorderSide(
                      color: theme.colorScheme.onSurfaceVariant.withValues(
                        alpha: 0.5,
                      ),
                    ),
                    foregroundColor: theme.colorScheme.onSurfaceVariant,
                  ),
                  child: Text('Play all', style: theme.textTheme.labelSmall),
                ),
              ),
            if (onTap != null)
              Icon(
                Icons.arrow_forward_rounded,
                color: theme.colorScheme.onSurfaceVariant,
              ),
          ],
        ),
      ),
    );
  }
}

/// Horizontal row of selectable pill chips.
class ChipsRow extends StatelessWidget {
  final List<String> labels;
  final int? selected;
  final ValueChanged<int?> onSelected;
  final EdgeInsets padding;

  const ChipsRow({
    super.key,
    required this.labels,
    required this.selected,
    required this.onSelected,
    this.padding = const EdgeInsets.symmetric(horizontal: 16),
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return SizedBox(
      height: 44,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: padding,
        itemCount: labels.length,
        separatorBuilder: (_, _) => const SizedBox(width: 8),
        itemBuilder: (context, i) {
          final isSel = selected == i;
          return Material(
            color: isSel ? scheme.primary : scheme.surfaceContainerHigh,
            borderRadius: BorderRadius.circular(EchoTheme.chipRadius),
            child: InkWell(
              borderRadius: BorderRadius.circular(EchoTheme.chipRadius),
              onTap: () => onSelected(isSel ? null : i),
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 18,
                  vertical: 10,
                ),
                child: Text(
                  labels[i],
                  style: Theme.of(context).textTheme.labelLarge?.copyWith(
                    color: isSel ? scheme.onPrimary : scheme.onSurface,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}

class ShimmerBox extends StatelessWidget {
  final double? width;
  final double height;
  final double radius;
  const ShimmerBox({
    super.key,
    this.width,
    required this.height,
    this.radius = 12,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Shimmer.fromColors(
      baseColor: scheme.surfaceContainerHighest,
      highlightColor: scheme.surfaceContainerLow,
      child: Container(
        width: width,
        height: height,
        decoration: BoxDecoration(
          color: scheme.surfaceContainerHighest,
          borderRadius: BorderRadius.circular(radius),
        ),
      ),
    );
  }
}

class ListShimmer extends StatelessWidget {
  final int count;
  const ListShimmer({super.key, this.count = 6});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: List.generate(
        count,
        (_) => const Padding(
          padding: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Row(
            children: [
              ShimmerBox(width: 48, height: 48, radius: 8),
              SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    ShimmerBox(width: 200, height: 14, radius: 6),
                    SizedBox(height: 8),
                    ShimmerBox(width: 120, height: 12, radius: 6),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class GridShimmer extends StatelessWidget {
  final double size;
  const GridShimmer({super.key, this.size = 150});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: size + 56,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        itemCount: 4,
        separatorBuilder: (_, _) => const SizedBox(width: 12),
        itemBuilder: (_, _) => Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            ShimmerBox(width: size, height: size, radius: 16),
            const SizedBox(height: 8),
            ShimmerBox(width: size * 0.8, height: 14, radius: 6),
            const SizedBox(height: 6),
            ShimmerBox(width: size * 0.5, height: 12, radius: 6),
          ],
        ),
      ),
    );
  }
}

class EmptyPlaceholder extends StatelessWidget {
  final IconData icon;
  final String text;
  final Widget? action;
  const EmptyPlaceholder({
    super.key,
    required this.icon,
    required this.text,
    this.action,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              icon,
              size: 56,
              color: scheme.onSurfaceVariant.withValues(alpha: 0.6),
            ),
            const SizedBox(height: 12),
            Text(
              text,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyLarge
                  ?.copyWith(color: scheme.onSurfaceVariant),
            ),
            if (action != null) ...[const SizedBox(height: 16), action!],
          ],
        ),
      ),
    );
  }
}

class ErrorPlaceholder extends StatelessWidget {
  final String message;
  final VoidCallback? onRetry;
  const ErrorPlaceholder({super.key, required this.message, this.onRetry});

  @override
  Widget build(BuildContext context) {
    return EmptyPlaceholder(
      icon: Icons.cloud_off_rounded,
      text: message,
      action: onRetry != null
          ? FilledButton.tonal(onPressed: onRetry, child: const Text('Retry'))
          : null,
    );
  }
}

/// Translucent rounded container (DESIGN.md "Custom Cards").
class EchoCard extends StatelessWidget {
  final Widget child;
  final EdgeInsets padding;
  final EdgeInsets margin;
  final double radius;
  final VoidCallback? onTap;
  final Color? color;
  const EchoCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(16),
    this.margin = EdgeInsets.zero,
    this.radius = EchoTheme.cardRadius,
    this.onTap,
    this.color,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Padding(
      padding: margin,
      child: Material(
        color: color ?? scheme.translucentCard,
        borderRadius: BorderRadius.circular(radius),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(radius),
          child: Padding(padding: padding, child: child),
        ),
      ),
    );
  }
}

/// Round icon button on a translucent background used in headers.
class RoundIconButton extends StatelessWidget {
  final IconData icon;
  final VoidCallback? onPressed;
  final double size;
  final Color? color;
  final Color? background;
  final bool filled;
  const RoundIconButton({
    super.key,
    required this.icon,
    this.onPressed,
    this.size = 44,
    this.color,
    this.background,
    this.filled = false,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return SizedBox(
      width: size,
      height: size,
      child: Material(
        color:
            background ??
            (filled ? scheme.primary : scheme.surfaceContainerHigh),
        shape: const CircleBorder(),
        child: InkWell(
          customBorder: const CircleBorder(),
          onTap: onPressed,
          child: Icon(
            icon,
            size: size * 0.5,
            color: color ?? (filled ? scheme.onPrimary : scheme.onSurface),
          ),
        ),
      ),
    );
  }
}
