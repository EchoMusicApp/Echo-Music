import 'dart:io';
import 'dart:ui';

import 'package:audio_service/audio_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:just_audio_media_kit/just_audio_media_kit.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';

import 'app.dart';
import 'data/database.dart';
import 'data/download_manager.dart';
import 'data/settings.dart';
import 'data/sync.dart';
import 'innertube/youtube.dart';
import 'innertube/youtube_client.dart';
import 'playback/audio_handler.dart';
import 'playback/player_controller.dart';
import 'stream/stream_resolver.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  if (Platform.isWindows || Platform.isLinux) {
    // No native sqflite or just_audio plugins here: use SQLite over FFI and
    // play through libmpv (media_kit).
    sqfliteFfiInit();
    databaseFactory = databaseFactoryFfi;
    JustAudioMediaKit.ensureInitialized();
  }
  final settings = await Settings.init();
  await AppDatabase.instance.db;

  // InnerTube session (port of the App class initialisation).
  final yt = YouTube.instance;
  final locale = PlatformDispatcher.instance.locale;
  final hl = settings.contentLanguage == 'system'
      ? locale.languageCode
      : settings.contentLanguage;
  final gl = settings.contentCountry == 'system'
      ? (locale.countryCode ?? 'US')
      : settings.contentCountry;
  yt.locale = YouTubeLocale(gl: gl, hl: hl);
  yt.innerTube.useLoginForBrowse = settings.useLoginForBrowse;
  if (settings.innerTubeCookie.isNotEmpty) yt.cookie = settings.innerTubeCookie;
  if (settings.dataSyncId.isNotEmpty) yt.dataSyncId = settings.dataSyncId;
  if (settings.visitorData.isNotEmpty) {
    yt.visitorData = settings.visitorData;
  } else {
    // Needed by every stream client; cheap (~200 ms) so do it before first paint.
    try {
      final v = await yt.refreshVisitorData().timeout(
        const Duration(seconds: 6),
      );
      await settings.setVisitorData(v);
    } catch (_) {}
  }

  StreamResolver.instance.quality = switch (settings.audioQuality) {
    AudioQualityPrefSetting.auto => AudioQualityPref.auto,
    AudioQualityPrefSetting.high => AudioQualityPref.high,
    AudioQualityPrefSetting.low => AudioQualityPref.low,
  };

  EchoAudioHandler handler;
  try {
    handler = await AudioService.init(
      builder: () => EchoAudioHandler(),
      config: const AudioServiceConfig(
        androidNotificationChannelId: 'echo.music.channel.audio',
        androidNotificationChannelName: 'Echo Music',
        androidNotificationOngoing: true,
        androidStopForegroundOnPause: true,
      ),
    );
  } on MissingPluginException {
    // audio_service has no Windows implementation: play without OS media
    // controls rather than failing to start.
    handler = EchoAudioHandler();
  }
  PlayerController.instance.handler = handler;
  await DownloadManager.instance.load();

  runApp(const EchoApp());

  // Background warm-ups.
  if (settings.isLoggedIn && settings.ytmSync) {
    Future.delayed(
      const Duration(seconds: 5),
      () => SyncManager.instance.syncAll(),
    );
  }
}
