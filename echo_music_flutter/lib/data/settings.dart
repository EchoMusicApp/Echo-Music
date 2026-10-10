import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

enum DarkModePref { auto, on, off }

enum AudioQualityPrefSetting { auto, high, low }

enum PlayerBackgroundStyle { gradient, blur, plain }

enum LyricsTextPosition { left, center, right }

enum AppFont { system, outfit, plusJakartaSans }

enum DefaultTab { home, explore, library }

/// App settings backed by SharedPreferences — port of the DataStore keys in
/// `constants/PreferenceKeys.kt` that apply on iOS.
class Settings extends ChangeNotifier {
  Settings._(this._prefs);
  final SharedPreferences _prefs;

  static Settings? _instance;
  static Settings get instance => _instance!;

  static Future<Settings> init() async {
    final prefs = await SharedPreferences.getInstance();
    _instance = Settings._(prefs);
    return _instance!;
  }

  T _enum<T extends Enum>(String key, List<T> values, T def) {
    final s = _prefs.getString(key);
    if (s == null) return def;
    return values.firstWhere((e) => e.name == s, orElse: () => def);
  }

  Future<void> _set(String key, Object? value) async {
    if (value == null) {
      await _prefs.remove(key);
    } else if (value is bool) {
      await _prefs.setBool(key, value);
    } else if (value is int) {
      await _prefs.setInt(key, value);
    } else if (value is double) {
      await _prefs.setDouble(key, value);
    } else if (value is String) {
      await _prefs.setString(key, value);
    } else if (value is List<String>) {
      await _prefs.setStringList(key, value);
    } else if (value is Enum) {
      await _prefs.setString(key, value.name);
    }
    notifyListeners();
  }

  // --- Appearance -------------------------------------------------------
  DarkModePref get darkMode =>
      _enum('darkMode', DarkModePref.values, DarkModePref.auto);
  set darkMode(DarkModePref v) => _set('darkMode', v);

  bool get pureBlack => _prefs.getBool('pureBlack') ?? false;
  set pureBlack(bool v) => _set('pureBlack', v);

  static const defaultThemeColor = 0xFFED5564;
  int get themeColor =>
      _prefs.getInt('selectedThemeColor') ?? defaultThemeColor;
  set themeColor(int v) => _set('selectedThemeColor', v);

  AppFont get appFont => _enum('appFont', AppFont.values, AppFont.system);
  set appFont(AppFont v) => _set('appFont', v);

  DefaultTab get defaultTab =>
      _enum('defaultOpenTab', DefaultTab.values, DefaultTab.home);
  set defaultTab(DefaultTab v) => _set('defaultOpenTab', v);

  PlayerBackgroundStyle get playerBackground => _enum(
    'playerBackgroundStyle',
    PlayerBackgroundStyle.values,
    PlayerBackgroundStyle.gradient,
  );
  set playerBackground(PlayerBackgroundStyle v) =>
      _set('playerBackgroundStyle', v);

  double get thumbnailCornerRadius =>
      _prefs.getDouble('thumbnailCornerRadius') ?? 12;
  set thumbnailCornerRadius(double v) => _set('thumbnailCornerRadius', v);

  bool get hidePlayerThumbnail =>
      _prefs.getBool('hidePlayerThumbnail') ?? false;
  set hidePlayerThumbnail(bool v) => _set('hidePlayerThumbnail', v);

  bool get showLyricsOnPlayer => _prefs.getBool('showLyricsOnPlayer') ?? false;
  set showLyricsOnPlayer(bool v) => _set('showLyricsOnPlayer', v);

  LyricsTextPosition get lyricsTextPosition => _enum(
    'lyricsTextPosition',
    LyricsTextPosition.values,
    LyricsTextPosition.left,
  );
  set lyricsTextPosition(LyricsTextPosition v) => _set('lyricsTextPosition', v);

  bool get lyricsClickSeeks => _prefs.getBool('lyricsClick') ?? true;
  set lyricsClickSeeks(bool v) => _set('lyricsClick', v);

  // --- Content ----------------------------------------------------------
  bool get hideExplicit => _prefs.getBool('hideExplicit') ?? false;
  set hideExplicit(bool v) => _set('hideExplicit', v);

  bool get hideVideoSongs => _prefs.getBool('hideVideoSongs') ?? false;
  set hideVideoSongs(bool v) => _set('hideVideoSongs', v);

  bool get hideYoutubeShorts => _prefs.getBool('hideYoutubeShorts') ?? false;
  set hideYoutubeShorts(bool v) => _set('hideYoutubeShorts', v);

  String get contentLanguage => _prefs.getString('contentLanguage') ?? 'system';
  set contentLanguage(String v) => _set('contentLanguage', v);

  String get contentCountry => _prefs.getString('contentCountry') ?? 'system';
  set contentCountry(String v) => _set('contentCountry', v);

  // --- Player & audio ---------------------------------------------------
  AudioQualityPrefSetting get audioQuality => _enum(
    'audioQuality',
    AudioQualityPrefSetting.values,
    AudioQualityPrefSetting.auto,
  );
  set audioQuality(AudioQualityPrefSetting v) => _set('audioQuality', v);

  bool get persistentQueue => _prefs.getBool('persistentQueue') ?? true;
  set persistentQueue(bool v) => _set('persistentQueue', v);

  bool get autoLoadMore => _prefs.getBool('autoLoadMore') ?? true;
  set autoLoadMore(bool v) => _set('autoLoadMore', v);

  bool get autoSkipNextOnError => _prefs.getBool('autoSkipNextOnError') ?? true;
  set autoSkipNextOnError(bool v) => _set('autoSkipNextOnError', v);

  bool get rememberShuffleAndRepeat =>
      _prefs.getBool('rememberShuffleAndRepeat') ?? true;
  set rememberShuffleAndRepeat(bool v) => _set('rememberShuffleAndRepeat', v);

  bool get shufflePlaylistFirst =>
      _prefs.getBool('shufflePlaylistFirst') ?? false;
  set shufflePlaylistFirst(bool v) => _set('shufflePlaylistFirst', v);

  bool get audioNormalization => _prefs.getBool('audioNormalization') ?? true;
  set audioNormalization(bool v) => _set('audioNormalization', v);

  bool get savedShuffle => _prefs.getBool('shuffleMode') ?? false;
  set savedShuffle(bool v) => _set('shuffleMode', v);

  int get savedRepeat => _prefs.getInt('repeatMode') ?? 0;
  set savedRepeat(int v) => _set('repeatMode', v);

  bool get autoDownloadOnLike => _prefs.getBool('autoDownloadOnLike') ?? false;
  set autoDownloadOnLike(bool v) => _set('autoDownloadOnLike', v);

  // --- Lyrics providers -------------------------------------------------
  bool get enableLrcLib => _prefs.getBool('enableLrclib') ?? true;
  set enableLrcLib(bool v) => _set('enableLrclib', v);
  bool get enableKugou => _prefs.getBool('enableKugou') ?? true;
  set enableKugou(bool v) => _set('enableKugou', v);
  bool get enableBetterLyrics => _prefs.getBool('enableBetterLyrics') ?? true;
  set enableBetterLyrics(bool v) => _set('enableBetterLyrics', v);
  bool get enableYouTubeLyrics => _prefs.getBool('enableYouTubeLyrics') ?? true;
  set enableYouTubeLyrics(bool v) => _set('enableYouTubeLyrics', v);
  bool get fetchFasterLyrics => _prefs.getBool('fetchFasterLyrics') ?? false;
  set fetchFasterLyrics(bool v) => _set('fetchFasterLyrics', v);

  // --- Privacy ----------------------------------------------------------
  bool get pauseListenHistory => _prefs.getBool('pauseListenHistory') ?? false;
  set pauseListenHistory(bool v) => _set('pauseListenHistory', v);

  bool get pauseSearchHistory => _prefs.getBool('pauseSearchHistory') ?? false;
  set pauseSearchHistory(bool v) => _set('pauseSearchHistory', v);

  bool get pauseRemoteListenHistory =>
      _prefs.getBool('pauseRemoteListenHistory') ?? false;
  set pauseRemoteListenHistory(bool v) => _set('pauseRemoteListenHistory', v);

  // --- Account ----------------------------------------------------------
  String get innerTubeCookie => _prefs.getString('innerTubeCookie') ?? '';
  String get visitorData => _prefs.getString('visitorData') ?? '';
  String get dataSyncId => _prefs.getString('dataSyncId') ?? '';
  String get accountName => _prefs.getString('accountName') ?? '';
  String get accountEmail => _prefs.getString('accountEmail') ?? '';
  String get accountChannelHandle =>
      _prefs.getString('accountChannelHandle') ?? '';
  String get accountAvatarUrl => _prefs.getString('accountAvatarUrl') ?? '';
  bool get isLoggedIn => innerTubeCookie.contains('SAPISID');
  bool get ytmSync => _prefs.getBool('ytmSync') ?? true;
  set ytmSync(bool v) => _set('ytmSync', v);
  bool get useLoginForBrowse => _prefs.getBool('useLoginForBrowse') ?? true;
  set useLoginForBrowse(bool v) => _set('useLoginForBrowse', v);

  Future<void> saveAccount({
    required String cookie,
    required String visitorData,
    required String dataSyncId,
    required String name,
    String? email,
    String? channelHandle,
    String? avatarUrl,
  }) async {
    await _prefs.setString('innerTubeCookie', cookie);
    await _prefs.setString('visitorData', visitorData);
    await _prefs.setString('dataSyncId', dataSyncId);
    await _prefs.setString('accountName', name);
    await _prefs.setString('accountEmail', email ?? '');
    await _prefs.setString('accountChannelHandle', channelHandle ?? '');
    await _prefs.setString('accountAvatarUrl', avatarUrl ?? '');
    notifyListeners();
  }

  Future<void> setVisitorData(String v) => _set('visitorData', v);

  Future<void> clearAccount() async {
    for (final k in [
      'innerTubeCookie',
      'dataSyncId',
      'accountName',
      'accountEmail',
      'accountChannelHandle',
      'accountAvatarUrl',
    ]) {
      await _prefs.remove(k);
    }
    notifyListeners();
  }

  // --- Misc -------------------------------------------------------------
  bool get isFirstRun => _prefs.getBool('isFirstRun') ?? true;
  set isFirstRun(bool v) => _set('isFirstRun', v);

  String get songSortType => _prefs.getString('songSortType') ?? 'CREATE_DATE';
  set songSortType(String v) => _set('songSortType', v);
  bool get songSortDescending => _prefs.getBool('songSortDescending') ?? true;
  set songSortDescending(bool v) => _set('songSortDescending', v);

  String get libraryFilter => _prefs.getString('libraryFilter') ?? 'PLAYLISTS';
  set libraryFilter(String v) => _set('libraryFilter', v);

  String? get persistedQueue => _prefs.getString('persistedQueue');
  Future<void> setPersistedQueue(String? json) async {
    if (json == null) {
      await _prefs.remove('persistedQueue');
    } else {
      await _prefs.setString('persistedQueue', json);
    }
  }

  int get maxImageCacheMb => _prefs.getInt('maxImageCacheSize') ?? 512;
  set maxImageCacheMb(int v) => _set('maxImageCacheSize', v);
}
