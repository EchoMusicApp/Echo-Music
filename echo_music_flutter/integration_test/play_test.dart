import 'package:echo_music/innertube/youtube.dart';
import 'package:echo_music/main.dart' as app;
import 'package:echo_music/playback/player_controller.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:just_audio/just_audio.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('play a song directly through the handler', (tester) async {
    await app.main();
    await tester.pump(const Duration(seconds: 3));
    final handler = player.handler;
    handler.player.playbackEventStream.listen(
      (e) {},
      onError: (Object e) {
        // ignore: avoid_print
        print('[test] playbackEvent error: $e');
      },
    );
    final songs = await YouTube.instance.search(
      'daft punk get lucky',
      YouTube.filterSong,
    );
    final song = songs.items.first;
    // ignore: avoid_print
    debugPrint('[test] playing ${song.title}');
    // ignore: avoid_print
    player
        .playSong(song as dynamic, context: null)
        .catchError((Object e) => print('[test] playSong error $e'));
    for (var i = 0; i < 20; i++) {
      await tester.pump(const Duration(seconds: 1));
      final p = handler.player;
      // ignore: avoid_print
      print(
        '[test] t=$i state=${p.processingState} playing=${p.playing} pos=${p.position.inMilliseconds} dur=${p.duration} loading=${handler.isLoadingItem.value} err=${handler.error.value} queue=${handler.queueItems.value.length} stream=${handler.currentStream.value?.clientName}',
      );
      if (p.position.inSeconds >= 3 && p.playing) break;
    }
    expect(handler.player.processingState, ProcessingState.ready);
    expect(handler.player.position.inSeconds, greaterThan(1));
  }, timeout: const Timeout(Duration(minutes: 4)));
}
