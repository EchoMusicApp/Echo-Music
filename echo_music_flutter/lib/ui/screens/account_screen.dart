import 'package:flutter/material.dart';

import '../../core/utils.dart';
import '../../data/settings.dart';
import '../../data/sync.dart';
import '../../innertube/models/yt_item.dart';
import '../../innertube/pages/pages.dart';
import '../../innertube/youtube.dart';
import '../components/common.dart';
import '../components/items.dart';
import '../components/thumbnail.dart';
import '../shell/app_navigator.dart';
import 'explore_screen.dart';

/// Account page: YouTube Music library when signed in (port of
/// `AccountScreen.kt`), sign-in call to action otherwise.
class AccountScreen extends StatefulWidget {
  const AccountScreen({super.key});

  @override
  State<AccountScreen> createState() => _AccountScreenState();
}

class _AccountScreenState extends State<AccountScreen> {
  LibraryPage? _playlists;
  LibraryPage? _albums;
  LibraryPage? _artists;
  String? _error;

  @override
  void initState() {
    super.initState();
    if (Settings.instance.isLoggedIn) _load();
  }

  Future<void> _load() async {
    setState(() => _error = null);
    try {
      final yt = YouTube.instance;
      final results = await Future.wait([
        yt.library('FEmusic_liked_playlists'),
        yt
            .library('FEmusic_liked_albums')
            .catchError((_) => const LibraryPage(items: [])),
        yt
            .library('FEmusic_library_corpus_artists')
            .catchError((_) => const LibraryPage(items: [])),
      ]);
      if (!mounted) return;
      setState(() {
        _playlists = results[0];
        _albums = results[1];
        _artists = results[2];
      });
    } catch (e) {
      if (mounted) setState(() => _error = '$e');
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return ListenableBuilder(
      listenable: Settings.instance,
      builder: (context, _) {
        final s = Settings.instance;
        if (!s.isLoggedIn) {
          return Scaffold(
            appBar: AppBar(title: const Text('Account')),
            body: EmptyPlaceholder(
              icon: Icons.account_circle_outlined,
              text: 'Sign in with your Google account to sync your YouTube Music library, likes and history.',
              action: FilledButton.icon(
                onPressed: () async {
                  final ok = await AppNavigator.pushRoot<bool>(
                    const _LoginLauncher(),
                    fullscreenDialog: true,
                  );
                  if (ok == true && mounted) _load();
                },
                icon: const Icon(Icons.login_rounded),
                label: const Text('Sign in'),
              ),
            ),
          );
        }
        return Scaffold(
          appBar: AppBar(
            title: const Text('Account'),
            actions: [
              ListenableBuilder(
                listenable: SyncManager.instance,
                builder: (context, _) => IconButton(
                  tooltip: 'Sync library',
                  onPressed: SyncManager.instance.syncing
                      ? null
                      : () => SyncManager.instance.syncAll(),
                  icon: SyncManager.instance.syncing
                      ? const SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.sync_rounded),
                ),
              ),
            ],
          ),
          body: RefreshIndicator(
            onRefresh: _load,
            child: ListView(
              padding: EdgeInsets.only(
                bottom: MediaQuery.paddingOf(context).bottom + 16,
              ),
              children: [
                Padding(
                  padding: const EdgeInsets.all(16),
                  child: EchoCard(
                    child: Row(
                      children: [
                        EchoImage(
                          url: s.accountAvatarUrl,
                          width: 56,
                          height: 56,
                          circle: true,
                          resize: 224,
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                s.accountName,
                                style: theme.textTheme.titleMedium?.copyWith(
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                              Text(
                                s.accountChannelHandle.isNotEmpty
                                    ? s.accountChannelHandle
                                    : s.accountEmail,
                                style: theme.textTheme.bodyMedium?.copyWith(
                                  color: theme.colorScheme.onSurfaceVariant,
                                ),
                              ),
                            ],
                          ),
                        ),
                        TextButton(
                          onPressed: () async {
                            await Settings.instance.clearAccount();
                            YouTube.instance.cookie = null;
                            YouTube.instance.dataSyncId = null;
                            if (context.mounted) {
                              showSnack(context, 'Signed out');
                            }
                          },
                          child: const Text('Sign out'),
                        ),
                      ],
                    ),
                  ),
                ),
                ListenableBuilder(
                  listenable: SyncManager.instance,
                  builder: (context, _) => SyncManager.instance.status == null
                      ? const SizedBox.shrink()
                      : Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 16),
                          child: Text(
                            SyncManager.instance.status!,
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: theme.colorScheme.onSurfaceVariant,
                            ),
                          ),
                        ),
                ),
                if (_error != null)
                  ErrorPlaceholder(message: _error!, onRetry: _load)
                else if (_playlists == null)
                  const GridShimmer()
                else ...[
                  if (_playlists!.items.isNotEmpty)
                    SectionCarousel(
                      title: 'Your playlists',
                      items: _playlists!.items,
                      onMore: () => AppNavigator.push(
                        _LibraryListScreen(
                          title: 'Your playlists',
                          browseId: 'FEmusic_liked_playlists',
                        ),
                      ),
                    ),
                  if (_albums != null && _albums!.items.isNotEmpty)
                    SectionCarousel(
                      title: 'Saved albums',
                      items: _albums!.items,
                      onMore: () => AppNavigator.push(
                        _LibraryListScreen(
                          title: 'Saved albums',
                          browseId: 'FEmusic_liked_albums',
                        ),
                      ),
                    ),
                  if (_artists != null && _artists!.items.isNotEmpty)
                    SectionCarousel(
                      title: 'Subscriptions',
                      items: _artists!.items,
                      onMore: () => AppNavigator.push(
                        _LibraryListScreen(
                          title: 'Subscriptions',
                          browseId: 'FEmusic_library_corpus_artists',
                        ),
                      ),
                    ),
                  ListTile(
                    leading: const Icon(Icons.favorite_rounded),
                    title: const Text('Liked songs on YouTube Music'),
                    trailing: const Icon(Icons.chevron_right_rounded),
                    onTap: () => AppNavigator.openPlaylist(
                      'LM',
                      playlist: const PlaylistItem(
                        id: 'LM',
                        title: 'Liked Music',
                      ),
                    ),
                  ),
                ],
              ],
            ),
          ),
        );
      },
    );
  }
}

class _LoginLauncher extends StatelessWidget {
  const _LoginLauncher();
  @override
  Widget build(BuildContext context) => const _LoginProxy();
}

class _LoginProxy extends StatelessWidget {
  const _LoginProxy();
  @override
  Widget build(BuildContext context) {
    // Indirection keeps login_screen's webview import out of this file's hot path.
    return const LoginScreenEntry();
  }
}

class LoginScreenEntry extends StatelessWidget {
  const LoginScreenEntry({super.key});
  @override
  Widget build(BuildContext context) => loginScreenBuilder();
}

/// Resolved in app.dart to avoid a direct circular import.
Widget Function() loginScreenBuilder = () => const SizedBox.shrink();

class _LibraryListScreen extends StatefulWidget {
  final String title;
  final String browseId;
  const _LibraryListScreen({required this.title, required this.browseId});

  @override
  State<_LibraryListScreen> createState() => _LibraryListScreenState();
}

class _LibraryListScreenState extends State<_LibraryListScreen> {
  LibraryPage? _page;
  String? _error;

  @override
  void initState() {
    super.initState();
    YouTube.instance
        .libraryCompleted(widget.browseId)
        .then((p) {
          if (mounted) setState(() => _page = p);
        })
        .catchError((e) {
          if (mounted) setState(() => _error = '$e');
        });
  }

  @override
  Widget build(BuildContext context) {
    final page = _page;
    return Scaffold(
      appBar: AppBar(title: Text(widget.title)),
      body: _error != null
          ? ErrorPlaceholder(message: _error!)
          : page == null
          ? const Center(child: CircularProgressIndicator())
          : page.items.every((i) => i is SongItem)
          ? ListView.builder(
              padding: EdgeInsets.only(
                bottom: MediaQuery.paddingOf(context).bottom + 16,
              ),
              itemCount: page.items.length,
              itemBuilder: (context, i) => YTItemTile(
                item: page.items[i],
                songContext: page.items.whereType<SongItem>().toList(),
              ),
            )
          : ItemGrid(items: page.items),
    );
  }
}
