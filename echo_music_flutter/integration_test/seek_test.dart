import 'package:echo_music/innertube/models/yt_item.dart';
import 'package:echo_music/main.dart' as app;
import 'package:echo_music/playback/player_controller.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:just_audio/just_audio.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('seek near the end and observe completion', (tester) async {
    await app.main();
    await tester.pump(const Duration(seconds: 3));
    final handler = player.handler;
    const song = SongItem(
      id: '4D7u5KF7SP8',
      title: 'Get Lucky',
      artists: [Artist(name: 'Daft Punk')],
      thumbnail: '',
      duration: 370,
    );
    await player.playSongItems([song]);
    for (var i = 0; i < 15; i++) {
      await tester.pump(const Duration(seconds: 1));
      if (handler.player.processingState == ProcessingState.ready) break;
    }
    // ignore: avoid_print
    print(
      '[test] ready dur=${handler.player.duration} pos=${handler.player.position}',
    );
    await handler.seek(const Duration(seconds: 362));
    for (var i = 0; i < 20; i++) {
      await tester.pump(const Duration(seconds: 1));
      final p = handler.player;
      // ignore: avoid_print
      print(
        '[test] t=$i state=${p.processingState} playing=${p.playing} pos=${p.position} buffered=${p.bufferedPosition} current=${handler.currentMetadata.value?.id}',
      );
      if (handler.currentMetadata.value?.id != '4D7u5KF7SP8') break;
    }
    // The seek lands within the last couple of seconds, so playback must have
    // moved on: either to the next queued track or to the completed state.
    expect(
      handler.currentMetadata.value?.id != '4D7u5KF7SP8' ||
          handler.player.processingState == ProcessingState.completed,
      isTrue,
      reason: 'playback did not reach the end of the track after seeking',
    );
  }, timeout: const Timeout(Duration(minutes: 4)));
}
