import 'package:echo_music/main.dart' as app;
import 'package:echo_music/playback/player_controller.dart';
import 'package:echo_music/ui/player/mini_player.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

Future<void> settle(WidgetTester tester, [int ms = 1500]) async {
  await tester.pump(Duration(milliseconds: ms));
  await tester.pump(const Duration(milliseconds: 300));
}

void main() {
  final binding = IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('walk through the app and play a song', (tester) async {
    await app.main();
    await settle(tester, 4000);
    await binding.takeScreenshot('01_home');

    // Explore tab
    await tester.tap(find.byIcon(Icons.explore_outlined));
    await settle(tester, 4000);
    await binding.takeScreenshot('02_explore');

    // Library tab
    await tester.tap(find.byIcon(Icons.library_music_outlined));
    await settle(tester, 1500);
    await binding.takeScreenshot('03_library');

    // Search
    await tester.tap(find.byIcon(Icons.search_rounded).last);
    await settle(tester, 1000);
    await tester.enterText(find.byType(TextField).first, 'daft punk get lucky');
    await tester.testTextInput.receiveAction(TextInputAction.done);
    await settle(tester, 5000);
    await binding.takeScreenshot('04_search');

    // Tap the first song row (first ListTile-like row with the song title)
    final songRow = find.textContaining('Get Lucky').first;
    expect(songRow, findsWidgets);
    await tester.tap(songRow, warnIfMissed: false);
    await settle(tester, 9000);
    await binding.takeScreenshot('05_mini_player');

    final handler = player.handler;
    // ignore: avoid_print
    print(
      'current: ${handler.currentMetadata.value?.title} stream=${handler.currentStream.value?.clientName} '
      'playing=${handler.player.playing} state=${handler.player.processingState} pos=${handler.player.position} err=${handler.error.value}',
    );

    // Open full player
    await tester.tap(
      find.byIcon(Icons.skip_next_rounded).first,
      warnIfMissed: false,
    );
    await settle(tester, 6000);
    // ignore: avoid_print
    print(
      'after skip: ${handler.currentMetadata.value?.title} playing=${handler.player.playing} pos=${handler.player.position} err=${handler.error.value}',
    );
    await tester.tap(find.byType(MiniPlayer), warnIfMissed: false);
    await settle(tester, 2500);
    await binding.takeScreenshot('06_full_player');

    await tester.tap(find.text('Lyrics'), warnIfMissed: false);
    await settle(tester, 6000);
    await binding.takeScreenshot('07_lyrics');

    await tester.tap(find.text('Queue'), warnIfMissed: false);
    await settle(tester, 2000);
    await binding.takeScreenshot('08_queue');
    // ignore: avoid_print
    print(
      'final: playing=${handler.player.playing} pos=${handler.player.position} dur=${handler.player.duration} queue=${handler.queueItems.value.length}',
    );
    expect(handler.player.position.inSeconds, greaterThan(2));
  }, timeout: const Timeout(Duration(minutes: 6)));
}
