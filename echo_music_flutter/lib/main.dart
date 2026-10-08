import 'dart:ui';

import 'package:audio_service/audio_service.dart';
import 'package:flutter/material.dart';

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

  final handler = await AudioService.init(
    builder: () => EchoAudioHandler(),
    config: const AudioServiceConfig(
      androidNotificationChannelId: 'echo.music.channel.audio',
      androidNotificationChannelName: 'Echo Music',
      androidNotificationOngoing: true,
      androidStopForegroundOnPause: true,
    ),
  );
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
