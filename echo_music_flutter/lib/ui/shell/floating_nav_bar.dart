import 'dart:ui';

import 'package:flutter/material.dart';

/// iOS-26-style floating pill navigation bar with a standalone circular
/// search tab (port of `AppFloatingNavBar` / `FloatingNavigationToolbar`).
class FloatingNavBar extends StatelessWidget {
  final int currentIndex;
  final ValueChanged<int> onTap;
  final VoidCallback onSearch;
  final bool searchSelected;

  const FloatingNavBar({
    super.key,
    required this.currentIndex,
    required this.onTap,
    required this.onSearch,
    this.searchSelected = false,
  });

  static const items = [
    (Icons.home_outlined, Icons.home_rounded, 'Home'),
    (Icons.explore_outlined, Icons.explore_rounded, 'Explore'),
    (Icons.library_music_outlined, Icons.library_music_rounded, 'Library'),
  ];

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final glassColor = scheme.surfaceContainerHigh.withValues(alpha: 0.72);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Row(
        children: [
          Expanded(
            child: _Glass(
              color: glassColor,
              radius: 32,
              child: SizedBox(
                height: 64,
                child: Row(
                  children: [
                    for (var i = 0; i < items.length; i++)
                      Expanded(
                        child: _NavItem(
                          icon: items[i].$1,
                          activeIcon: items[i].$2,
                          label: items[i].$3,
                          selected: !searchSelected && currentIndex == i,
                          onTap: () => onTap(i),
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ),
          const SizedBox(width: 10),
          _Glass(
            color: searchSelected ? scheme.primary : glassColor,
            radius: 32,
            child: SizedBox(
              width: 64,
              height: 64,
              child: InkWell(
                onTap: onSearch,
                borderRadius: BorderRadius.circular(32),
                child: Icon(
                  Icons.search_rounded,
                  size: 28,
                  color: searchSelected ? scheme.onPrimary : scheme.onSurface,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _NavItem extends StatelessWidget {
  final IconData icon;
  final IconData activeIcon;
  final String label;
  final bool selected;
  final VoidCallback onTap;
  const _NavItem({
    required this.icon,
    required this.activeIcon,
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final color = selected
        ? scheme.onSurface
        : scheme.onSurfaceVariant.withValues(alpha: 0.8);
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(32),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 250),
        curve: Curves.easeOutCubic,
        margin: const EdgeInsets.all(6),
        decoration: BoxDecoration(
          color: selected
              ? scheme.surfaceContainerHighest.withValues(alpha: 0.9)
              : Colors.transparent,
          borderRadius: BorderRadius.circular(26),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(selected ? activeIcon : icon, color: color, size: 24),
            if (selected) ...[
              const SizedBox(width: 6),
              Text(
                label,
                style: Theme.of(context).textTheme.labelLarge
                    ?.copyWith(color: color, fontWeight: FontWeight.w700),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

/// Frosted glass container (the "liquid glass" look without RenderEffect).
class _Glass extends StatelessWidget {
  final Widget child;
  final Color color;
  final double radius;
  const _Glass({
    required this.child,
    required this.color,
    required this.radius,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return ClipRRect(
      borderRadius: BorderRadius.circular(radius),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 24, sigmaY: 24),
        child: Container(
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(radius),
            border: Border.all(
              color: scheme.outlineVariant.withValues(alpha: 0.25),
            ),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.12),
                blurRadius: 24,
                offset: const Offset(0, 8),
              ),
            ],
          ),
          child: Material(color: Colors.transparent, child: child),
        ),
      ),
    );
  }
}

/// Frosted glass surface for reuse elsewhere (mini player).
class GlassSurface extends StatelessWidget {
  final Widget child;
  final double radius;
  final Color? color;
  const GlassSurface({
    super.key,
    required this.child,
    this.radius = 24,
    this.color,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return _Glass(
      color: color ?? scheme.surfaceContainerHigh.withValues(alpha: 0.72),
      radius: radius,
      child: child,
    );
  }
}
