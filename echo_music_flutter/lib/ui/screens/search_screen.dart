import 'dart:async';

import 'package:flutter/material.dart';

import '../../data/database.dart';
import '../../data/settings.dart';
import '../../innertube/models/yt_item.dart';
import '../../innertube/pages/pages.dart';
import '../../innertube/youtube.dart';
import '../../playback/player_controller.dart';
import '../components/common.dart';
import '../components/items.dart';

/// Search — history, live suggestions, summary results and filtered
/// paginated results (port of `SearchScreen.kt` / `OnlineSearchScreen.kt`).
class SearchScreen extends StatefulWidget {
  final String? initialQuery;
  const SearchScreen({super.key, this.initialQuery});

  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen>
    with AutomaticKeepAliveClientMixin {
  final _controller = TextEditingController();
  final _focus = FocusNode();
  final _scroll = ScrollController();
  Timer? _debounce;
  SearchSuggestions? _suggestions;
  List<String> _history = const [];
  SearchSummaryPage? _summary;
  SearchResult? _filtered;
  int? _filterIndex;
  String _submitted = '';
  bool _loading = false;
  bool _loadingMore = false;
  String? _error;

  static const _filters = [
    ('Songs', YouTube.filterSong),
    ('Videos', YouTube.filterVideo),
    ('Albums', YouTube.filterAlbum),
    ('Artists', YouTube.filterArtist),
    ('Playlists', YouTube.filterCommunityPlaylist),
    ('Featured', YouTube.filterFeaturedPlaylist),
  ];

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();
    _loadHistory();
    _controller.addListener(_onChanged);
    _scroll.addListener(_onScroll);
    if (widget.initialQuery != null) {
      _controller.text = widget.initialQuery!;
      _submit(widget.initialQuery!);
    } else {
      WidgetsBinding.instance.addPostFrameCallback(
        (_) => _focus.requestFocus(),
      );
    }
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _controller.dispose();
    _focus.dispose();
    _scroll.dispose();
    super.dispose();
  }

  Future<void> _loadHistory([String? q]) async {
    final h = await AppDatabase.instance.searchHistory(query: q);
    if (mounted) setState(() => _history = h);
  }

  void _onChanged() {
    final q = _controller.text;
    if (q == _submitted && _summary != null) return;
    if (_submitted.isNotEmpty && q != _submitted) {
      setState(() {
        _summary = null;
        _filtered = null;
        _submitted = '';
      });
    }
    _debounce?.cancel();
    if (q.trim().isEmpty) {
      setState(() => _suggestions = null);
      _loadHistory();
      return;
    }
    _loadHistory(q);
    _debounce = Timer(const Duration(milliseconds: 300), () async {
      try {
        final s = await YouTube.instance.searchSuggestions(q);
        if (mounted && _controller.text == q) setState(() => _suggestions = s);
      } catch (_) {}
    });
  }

  Future<void> _submit(String query) async {
    final q = query.trim();
    if (q.isEmpty) return;
    _focus.unfocus();
    _controller.value = TextEditingValue(
      text: q,
      selection: TextSelection.collapsed(offset: q.length),
    );
    if (!Settings.instance.pauseSearchHistory) {
      await AppDatabase.instance.addSearchHistory(q);
    }
    setState(() {
      _submitted = q;
      _loading = true;
      _error = null;
      _summary = null;
      _filtered = null;
      _suggestions = null;
    });
    try {
      if (_filterIndex == null) {
        final page = await YouTube.instance.searchSummary(q);
        if (!mounted || _submitted != q) return;
        setState(() {
          _summary = page;
          _loading = false;
        });
      } else {
        final r = await YouTube.instance.search(q, _filters[_filterIndex!].$2);
        if (!mounted || _submitted != q) return;
        setState(() {
          _filtered = r;
          _loading = false;
        });
      }
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = '$e';
        _loading = false;
      });
    }
  }

  void _onScroll() {
    if (_scroll.position.pixels > _scroll.position.maxScrollExtent - 600) {
      _loadMore();
    }
  }

  Future<void> _loadMore() async {
    final f = _filtered;
    if (f == null || f.continuation == null || _loadingMore) return;
    _loadingMore = true;
    try {
      final more = await YouTube.instance.searchContinuation(f.continuation!);
      if (!mounted) return;
      setState(
        () => _filtered = SearchResult(
          items: [...f.items, ...more.items],
          continuation: more.continuation,
        ),
      );
    } catch (_) {
    } finally {
      _loadingMore = false;
    }
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final theme = Theme.of(context);
    final settings = Settings.instance;
    final showResults = _submitted.isNotEmpty;
    return Scaffold(
      appBar: AppBar(
        titleSpacing: 0,
        leading: widget.initialQuery != null ? const BackButton() : null,
        title: Padding(
          padding: EdgeInsets.only(
            right: 12,
            left: widget.initialQuery != null ? 0 : 16,
          ),
          child: TextField(
            controller: _controller,
            focusNode: _focus,
            textInputAction: TextInputAction.search,
            onSubmitted: _submit,
            decoration: InputDecoration(
              hintText: 'Search YouTube Music…',
              prefixIcon: const Icon(Icons.search_rounded),
              suffixIcon: _controller.text.isNotEmpty
                  ? IconButton(
                      icon: const Icon(Icons.close_rounded),
                      onPressed: () {
                        _controller.clear();
                        setState(() {
                          _summary = null;
                          _filtered = null;
                          _submitted = '';
                          _suggestions = null;
                        });
                        _focus.requestFocus();
                      },
                    )
                  : null,
              isDense: true,
            ),
          ),
        ),
      ),
      body: Column(
        children: [
          if (showResults)
            Padding(
              padding: const EdgeInsets.only(top: 4, bottom: 4),
              child: ChipsRow(
                labels: _filters.map((f) => f.$1).toList(),
                selected: _filterIndex,
                onSelected: (i) {
                  setState(() => _filterIndex = i);
                  _submit(_submitted);
                },
              ),
            ),
          Expanded(child: _body(theme, settings)),
        ],
      ),
    );
  }

  Widget _body(ThemeData theme, Settings settings) {
    final bottom = MediaQuery.paddingOf(context).bottom + 16;
    if (_submitted.isNotEmpty) {
      if (_loading) return const ListShimmer(count: 8);
      if (_error != null) {
        return ErrorPlaceholder(
          message: _error!,
          onRetry: () => _submit(_submitted),
        );
      }
      final summary = _summary;
      if (summary != null) {
        final sections = summary.summaries
            .map(
              (s) => SearchSummary(
                title: s.title,
                items: s.items
                    .filterExplicit(settings.hideExplicit)
                    .filterVideoSongs(
                      settings.hideVideoSongs && s.title != 'Videos',
                    )
                    .filterYoutubeShorts(settings.hideYoutubeShorts),
              ),
            )
            .where((s) => s.items.isNotEmpty)
            .toList();
        if (sections.isEmpty) {
          return const EmptyPlaceholder(
            icon: Icons.search_off_rounded,
            text: 'No results',
          );
        }
        return ListView.builder(
          controller: _scroll,
          padding: EdgeInsets.only(bottom: bottom),
          itemCount: sections.length,
          itemBuilder: (context, i) {
            final s = sections[i];
            final songs = s.items.whereType<SongItem>().toList();
            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                NavigationTitle(
                  title: s.title,
                  onPlayAll: songs.length > 1
                      ? () => player.playSongItems(songs, title: s.title)
                      : null,
                ),
                for (final it in s.items)
                  YTItemTile(
                    item: it,
                    songContext: it is SongItem && songs.length > 1
                        ? songs
                        : null,
                    contextTitle: s.title,
                  ),
              ],
            );
          },
        );
      }
      final filtered = _filtered;
      if (filtered != null) {
        final items = filtered.items
            .filterExplicit(settings.hideExplicit)
            .filterVideoSongs(settings.hideVideoSongs && _filterIndex != 1)
            .filterYoutubeShorts(settings.hideYoutubeShorts);
        if (items.isEmpty) {
          return const EmptyPlaceholder(
            icon: Icons.search_off_rounded,
            text: 'No results',
          );
        }
        final songs = items.whereType<SongItem>().toList();
        return ListView.builder(
          controller: _scroll,
          padding: EdgeInsets.only(bottom: bottom),
          itemCount: items.length + (filtered.continuation != null ? 1 : 0),
          itemBuilder: (context, i) {
            if (i == items.length) {
              return const Padding(
                padding: EdgeInsets.all(16),
                child: Center(child: CircularProgressIndicator()),
              );
            }
            return YTItemTile(
              item: items[i],
              songContext: items[i] is SongItem ? songs : null,
              contextTitle: 'Search',
            );
          },
        );
      }
      return const SizedBox.shrink();
    }

    // Idle / typing state
    final q = _controller.text.trim();
    final suggestions = _suggestions;
    return ListView(
      padding: EdgeInsets.only(bottom: bottom),
      keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
      children: [
        if (_history.isNotEmpty && q.isEmpty)
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    'RECENT SEARCHES',
                    style: theme.textTheme.labelLarge?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                      fontWeight: FontWeight.w800,
                      letterSpacing: 0.6,
                    ),
                  ),
                ),
                TextButton(
                  onPressed: () async {
                    await AppDatabase.instance.clearSearchHistory();
                    _loadHistory();
                  },
                  child: const Text('Clear'),
                ),
              ],
            ),
          ),
        for (final h in _history)
          ListTile(
            leading: const Icon(Icons.history_rounded),
            title: Text(h),
            trailing: IconButton(
              icon: const Icon(Icons.north_west_rounded, size: 18),
              onPressed: () {
                _controller.text = h;
                _controller.selection = TextSelection.collapsed(
                  offset: h.length,
                );
              },
            ),
            onTap: () => _submit(h),
            onLongPress: () async {
              await AppDatabase.instance.deleteSearchHistory(h);
              _loadHistory(q.isEmpty ? null : q);
            },
          ),
        if (suggestions != null) ...[
          for (final s in suggestions.queries.where(
            (s) => !_history.contains(s),
          ))
            ListTile(
              leading: const Icon(Icons.search_rounded),
              title: Text(s),
              trailing: IconButton(
                icon: const Icon(Icons.north_west_rounded, size: 18),
                onPressed: () {
                  _controller.text = s;
                  _controller.selection = TextSelection.collapsed(
                    offset: s.length,
                  );
                },
              ),
              onTap: () => _submit(s),
            ),
          if (suggestions.recommendedItems.isNotEmpty) const Divider(),
          for (final it
              in suggestions.recommendedItems
                  .filterExplicit(settings.hideExplicit)
                  .filterVideoSongs(settings.hideVideoSongs))
            YTItemTile(item: it),
        ],
      ],
    );
  }
}
