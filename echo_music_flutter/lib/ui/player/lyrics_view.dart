import 'dart:async';

import 'package:flutter/material.dart';
import 'package:scrollable_positioned_list/scrollable_positioned_list.dart';

import '../../data/database.dart';
import '../../data/settings.dart';
import '../../lyrics/lyrics_helper.dart';
import '../../lyrics/lyrics_utils.dart';
import '../../playback/media_metadata.dart';
import '../../playback/player_controller.dart';

/// Synced lyrics with the current line highlighted and auto-scrolled
/// (port of the `Lyrics.kt` composable).
class LyricsView extends StatefulWidget {
  final MediaMetadata meta;
  final Color textColor;
  const LyricsView({super.key, required this.meta, required this.textColor});

  @override
  State<LyricsView> createState() => _LyricsViewState();
}

class _LyricsViewState extends State<LyricsView> {
  LyricsWithProvider? _result;
  List<LyricsEntry> _lines = const [];
  bool _synced = false;
  bool _loading = true;
  int _current = -1;
  int _offset = 0;
  StreamSubscription<Duration>? _sub;
  final _scroll = ItemScrollController();
  DateTime _userScrollUntil = DateTime.fromMillisecondsSinceEpoch(0);

  @override
  void initState() {
    super.initState();
    _load();
    _sub = player.handler.player.positionStream.listen(_onPosition);
  }

  @override
  void dispose() {
    _sub?.cancel();
    super.dispose();
  }

  Future<void> _load({bool refresh = false}) async {
    setState(() => _loading = true);
    final song = await AppDatabase.instance.song(widget.meta.id);
    _offset = song?.song.lyricsOffset ?? 0;
    final r = await LyricsHelper.instance.getLyrics(
      widget.meta,
      refresh: refresh,
    );
    if (!mounted) return;
    setState(() {
      _result = r;
      _loading = false;
      if (r.isNotFound) {
        _lines = const [];
        _synced = false;
      } else if (LyricsUtils.isSynced(r.lyrics)) {
        _lines = LyricsUtils.parseLyrics(r.lyrics);
        _synced = _lines.isNotEmpty;
        if (!_synced) _lines = const [];
      } else {
        _synced = false;
        _lines = const [];
      }
    });
  }

  void _onPosition(Duration pos) {
    if (!_synced || _lines.isEmpty) return;
    final idx = LyricsUtils.findCurrentLineIndex(
      _lines,
      pos.inMilliseconds + _offset,
    );
    if (idx != _current) {
      setState(() => _current = idx);
      if (idx >= 0 &&
          DateTime.now().isAfter(_userScrollUntil) &&
          _scroll.isAttached) {
        _scroll.scrollTo(
          index: idx,
          duration: const Duration(milliseconds: 400),
          curve: Curves.easeOutCubic,
          alignment: 0.35,
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final color = widget.textColor;
    final theme = Theme.of(context);
    if (_loading) {
      return Center(child: CircularProgressIndicator(color: color));
    }
    final r = _result;
    if (r == null || r.isNotFound) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.lyrics_outlined,
              color: color.withValues(alpha: 0.6),
              size: 48,
            ),
            const SizedBox(height: 12),
            Text(
              'No lyrics found',
              style: theme.textTheme.titleMedium?.copyWith(color: color),
            ),
            const SizedBox(height: 12),
            FilledButton.tonal(
              onPressed: () => _pickLyrics(context),
              child: const Text('Search lyrics'),
            ),
          ],
        ),
      );
    }
    final align = switch (Settings.instance.lyricsTextPosition) {
      LyricsTextPosition.left => TextAlign.left,
      LyricsTextPosition.center => TextAlign.center,
      LyricsTextPosition.right => TextAlign.right,
    };
    Widget body;
    if (!_synced) {
      body = SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(24, 24, 24, 48),
        child: Text(
          r.lyrics,
          textAlign: align,
          style: theme.textTheme.titleLarge?.copyWith(
            color: color,
            fontWeight: FontWeight.w700,
            height: 1.5,
          ),
        ),
      );
    } else {
      body = NotificationListener<UserScrollNotification>(
        onNotification: (n) {
          _userScrollUntil = DateTime.now().add(const Duration(seconds: 3));
          return false;
        },
        child: ScrollablePositionedList.builder(
          itemScrollController: _scroll,
          itemCount: _lines.length + 1,
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 24),
          itemBuilder: (context, i) {
            if (i == _lines.length) return const SizedBox(height: 240);
            final line = _lines[i];
            final isCurrent = i == _current;
            final past = i < _current;
            return InkWell(
              onTap: Settings.instance.lyricsClickSeeks
                  ? () => player.handler.seek(
                      Duration(
                        milliseconds: (line.time - _offset).clamp(0, 1 << 31),
                      ),
                    )
                  : null,
              borderRadius: BorderRadius.circular(12),
              child: AnimatedDefaultTextStyle(
                duration: const Duration(milliseconds: 250),
                style: theme.textTheme.headlineSmall!.copyWith(
                  color: isCurrent
                      ? color
                      : color.withValues(alpha: past ? 0.35 : 0.5),
                  fontWeight: FontWeight.w800,
                  fontSize: line.isBackground ? 18 : 26,
                  height: 1.25,
                ),
                child: Padding(
                  padding: EdgeInsets.symmetric(
                    vertical: line.isBackground ? 4 : 10,
                    horizontal: 4,
                  ),
                  child: Text(line.text, textAlign: align),
                ),
              ),
            );
          },
        ),
      );
    }
    return Stack(
      children: [
        ShaderMask(
          shaderCallback: (rect) => const LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              Colors.transparent,
              Colors.black,
              Colors.black,
              Colors.transparent,
            ],
            stops: [0, 0.08, 0.9, 1],
          ).createShader(rect),
          blendMode: BlendMode.dstIn,
          child: body,
        ),
        Positioned(
          right: 8,
          bottom: 4,
          child: Row(
            children: [
              if (_synced)
                _SmallChip(
                  label:
                      '${_offset >= 0 ? '+' : ''}${(_offset / 1000).toStringAsFixed(1)}s',
                  color: color,
                  onTap: () => _adjustOffset(context),
                ),
              const SizedBox(width: 6),
              _SmallChip(
                label: r.provider,
                color: color,
                onTap: () => _pickLyrics(context),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Future<void> _adjustOffset(BuildContext context) async {
    var value = _offset.toDouble();
    await showModalBottomSheet<void>(
      context: context,
      useRootNavigator: true,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setSheet) => SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 0, 20, 16),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  'Lyrics offset: ${(value / 1000).toStringAsFixed(1)}s',
                  style: Theme.of(ctx).textTheme.titleMedium,
                ),
                Slider(
                  value: value,
                  min: -5000,
                  max: 5000,
                  divisions: 100,
                  onChanged: (v) => setSheet(() => value = v),
                ),
                Row(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: [
                    TextButton(
                      onPressed: () => setSheet(() => value = 0),
                      child: const Text('Reset'),
                    ),
                    FilledButton(
                      onPressed: () async {
                        await AppDatabase.instance.setLyricsOffset(
                          widget.meta.id,
                          value.round(),
                        );
                        if (ctx.mounted) Navigator.of(ctx).pop();
                      },
                      child: const Text('Apply'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
    final song = await AppDatabase.instance.song(widget.meta.id);
    if (mounted) setState(() => _offset = song?.song.lyricsOffset ?? 0);
  }

  Future<void> _pickLyrics(BuildContext context) async {
    final results = await LyricsHelper.instance.getAllLyrics(widget.meta);
    if (!context.mounted) return;
    final chosen = await showModalBottomSheet<LyricsWithProvider>(
      context: context,
      useRootNavigator: true,
      isScrollControlled: true,
      builder: (ctx) => SafeArea(
        child: ConstrainedBox(
          constraints: BoxConstraints(
            maxHeight: MediaQuery.sizeOf(ctx).height * 0.8,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 0, 20, 8),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        'Choose lyrics',
                        style: Theme.of(ctx).textTheme.titleLarge,
                      ),
                    ),
                    TextButton(
                      onPressed: () async {
                        await _load(refresh: true);
                        if (ctx.mounted) Navigator.of(ctx).pop();
                      },
                      child: const Text('Refresh'),
                    ),
                  ],
                ),
              ),
              Flexible(
                child: results.isEmpty
                    ? const Padding(
                        padding: EdgeInsets.all(24),
                        child: Text('No results from any provider'),
                      )
                    : ListView.builder(
                        shrinkWrap: true,
                        itemCount: results.length,
                        itemBuilder: (_, i) {
                          final r = results[i];
                          final preview = LyricsUtils.isSynced(r.lyrics)
                              ? LyricsUtils.parseLyrics(r.lyrics)
                                    .take(3)
                                    .map((e) => e.text)
                                    .join(' / ')
                              : r.lyrics.split('\n').take(3).join(' / ');
                          return ListTile(
                            leading: Icon(
                              LyricsUtils.isSynced(r.lyrics)
                                  ? Icons.sync_rounded
                                  : Icons.notes_rounded,
                            ),
                            title: Text(r.provider),
                            subtitle: Text(
                              preview,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                            ),
                            onTap: () => Navigator.of(ctx).pop(r),
                          );
                        },
                      ),
              ),
            ],
          ),
        ),
      ),
    );
    if (chosen != null) {
      await LyricsHelper.instance.setLyrics(widget.meta.id, chosen);
      await _load();
    }
  }
}

class _SmallChip extends StatelessWidget {
  final String label;
  final Color color;
  final VoidCallback onTap;
  const _SmallChip({
    required this.label,
    required this.color,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: color.withValues(alpha: 0.15),
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
          child: Text(
            label,
            style: TextStyle(
              color: color,
              fontSize: 11,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
      ),
    );
  }
}
