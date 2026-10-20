import 'package:flutter/material.dart';

import '../../innertube/models/yt_item.dart';
import '../screens/album_screen.dart';
import '../screens/artist_items_screen.dart';
import '../screens/artist_screen.dart';
import '../screens/browse_screen.dart';
import '../screens/history_screen.dart';
import '../screens/local_playlist_screen.dart';
import '../screens/login_screen.dart';
import '../screens/online_playlist_screen.dart';
import '../screens/search_screen.dart';
import '../settings/settings_screen.dart';

/// Navigation helper: each bottom tab owns a nested [Navigator]; detail
/// screens push onto the current tab's stack. The full-screen player is an
/// overlay controlled by [playerExpanded].
class AppNavigator {
  AppNavigator._();

  static final rootKey = GlobalKey<NavigatorState>();
  static final tabKeys = List.generate(4, (_) => GlobalKey<NavigatorState>());
  static final currentTab = ValueNotifier<int>(0);
  static final playerExpanded = ValueNotifier<bool>(false);
  static final queueRequested = ValueNotifier<int>(0);

  static NavigatorState get _nav =>
      tabKeys[currentTab.value].currentState ?? rootKey.currentState!;

  static Future<T?> push<T>(Widget page, {bool fullscreenDialog = false}) {
    playerExpanded.value = false;
    return _nav.push<T>(
      MaterialPageRoute(
        builder: (_) => page,
        fullscreenDialog: fullscreenDialog,
      ),
    );
  }

  static Future<T?> pushRoot<T>(Widget page, {bool fullscreenDialog = false}) {
    playerExpanded.value = false;
    return rootKey.currentState!.push<T>(
      MaterialPageRoute(
        builder: (_) => page,
        fullscreenDialog: fullscreenDialog,
      ),
    );
  }

  static void pop<T>([T? result]) => _nav.pop(result);

  static bool popToRoot() {
    final nav = tabKeys[currentTab.value].currentState;
    if (nav == null || !nav.canPop()) return false;
    nav.popUntil((r) => r.isFirst);
    return true;
  }

  static void openPlayer() => playerExpanded.value = true;
  static void closePlayer() => playerExpanded.value = false;
  static void openQueue() {
    playerExpanded.value = true;
    queueRequested.value++;
  }

  static void openAlbum(String browseId, {AlbumItem? album}) =>
      push(AlbumScreen(browseId: browseId, initial: album));
  static void openArtist(String id) => push(ArtistScreen(artistId: id));
  static void openArtistItems(BrowseEndpoint endpoint, {String? title}) =>
      push(ArtistItemsScreen(endpoint: endpoint, title: title));
  static void openPlaylist(String id, {PlaylistItem? playlist}) =>
      push(OnlinePlaylistScreen(playlistId: id, initial: playlist));
  static void openLocalPlaylist(String id) =>
      push(LocalPlaylistScreen(playlistId: id));
  static void openLikedSongs() =>
      push(const LocalPlaylistScreen(playlistId: LocalPlaylistScreen.likedId));
  static void openDownloaded() => push(
    const LocalPlaylistScreen(playlistId: LocalPlaylistScreen.downloadedId),
  );
  static void openTopSongs() =>
      push(const LocalPlaylistScreen(playlistId: LocalPlaylistScreen.topId));
  static void openBrowse(BrowseEndpoint endpoint, {String? title}) =>
      push(BrowseScreen(endpoint: endpoint, title: title));
  static void openHistory() => push(const HistoryScreen());
  static void openSearch({String? query}) =>
      push(SearchScreen(initialQuery: query));
  static void openSettings() => pushRoot(const SettingsScreen());
  static void openLogin() =>
      pushRoot(const LoginScreen(), fullscreenDialog: true);

  /// Open the right screen for any [YTItem].
  static void openItem(YTItem item) {
    if (item is AlbumItem) {
      openAlbum(item.browseId, album: item);
    } else if (item is ArtistItem) {
      openArtist(item.id);
    } else if (item is PlaylistItem) {
      openPlaylist(item.id, playlist: item);
    }
  }
}
