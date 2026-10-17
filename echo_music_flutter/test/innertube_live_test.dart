// Live smoke tests against InnerTube (network required). Run with:
//   flutter test test/innertube_live_test.dart
import 'package:echo_music/innertube/models/yt_item.dart';
import 'package:echo_music/innertube/youtube.dart';
import 'package:echo_music/innertube/youtube_client.dart';
import 'package:echo_music/lyrics/lyrics_utils.dart';
import 'package:echo_music/lyrics/providers/lrclib.dart';
import 'package:echo_music/stream/stream_resolver.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  final yt = YouTube.instance;
  yt.locale = const YouTubeLocale(gl: 'US', hl: 'en');

  setUpAll(() async {
    try {
      await yt.refreshVisitorData();
      // ignore: avoid_print
      print('visitorData: ${yt.visitorData}');
    } catch (e) {
      // ignore: avoid_print
      print('visitorData failed: $e');
    }
  });

  test('home page parses sections', () async {
    final home = await yt.home();
    // ignore: avoid_print
    print(
      'home: ${home.sections.length} sections, chips=${home.chips?.length}, cont=${home.continuation != null}',
    );
    for (final s in home.sections.take(6)) {
      // ignore: avoid_print
      print(
        '  - ${s.title}: ${s.items.length} items (${s.items.first.runtimeType})',
      );
    }
    expect(home.sections, isNotEmpty);
  });

  test('search summary + filtered search', () async {
    final summary = await yt.searchSummary('daft punk get lucky');
    // ignore: avoid_print
    print(
      'summary: ${summary.summaries.map((s) => '${s.title}(${s.items.length})').join(', ')}',
    );
    expect(summary.summaries, isNotEmpty);
    final songs = await yt.search('daft punk', YouTube.filterSong);
    // ignore: avoid_print
    print(
      'songs: ${songs.items.length}, first=${(songs.items.first as SongItem).title} by ${(songs.items.first as SongItem).artistsText} dur=${(songs.items.first as SongItem).duration}',
    );
    expect(songs.items.whereType<SongItem>(), isNotEmpty);
    final artists = await yt.search('daft punk', YouTube.filterArtist);
    expect(artists.items.whereType<ArtistItem>(), isNotEmpty);
    final albums = await yt.search(
      'random access memories',
      YouTube.filterAlbum,
    );
    expect(albums.items.whereType<AlbumItem>(), isNotEmpty);
    final album = albums.items.whereType<AlbumItem>().first;
    // ignore: avoid_print
    print(
      'album: ${album.title} browse=${album.browseId} playlist=${album.playlistId}',
    );
  });

  test('album, artist, playlist, next', () async {
    final albums = await yt.search(
      'random access memories daft punk',
      YouTube.filterAlbum,
    );
    final albumItem = albums.items.whereType<AlbumItem>().first;
    final album = await yt.album(albumItem.browseId);
    // ignore: avoid_print
    print(
      'album page: ${album.album.title} (${album.album.year}) songs=${album.songs.length} other=${album.otherVersions.length}',
    );
    expect(album.songs, isNotEmpty);
    expect(album.songs.first.duration, isNotNull);

    final artistId = album.album.artists?.firstWhere((a) => a.id != null).id;
    if (artistId != null) {
      final artist = await yt.artist(artistId);
      // ignore: avoid_print
      print(
        'artist: ${artist.artist.title} subs=${artist.subscriberCountText} sections=${artist.sections.map((s) => '${s.title}(${s.items.length})').join(', ')}',
      );
      expect(artist.sections, isNotEmpty);
    }

    final next = await yt.next(WatchEndpoint(videoId: album.songs.first.id));
    // ignore: avoid_print
    print(
      'next: title=${next.title} items=${next.items.length} idx=${next.currentIndex} cont=${next.continuation != null} lyrics=${next.lyricsEndpoint?.browseId}',
    );
    expect(next.items.length, greaterThan(5));

    final charts = await yt.charts();
    // ignore: avoid_print
    print(
      'charts: ${charts.sections.map((s) => '${s.title}(${s.items.length})').join(', ')}',
    );
    final explore = await yt.explore();
    // ignore: avoid_print
    print(
      'explore: albums=${explore.newReleaseAlbums.length} moods=${explore.moodAndGenres.length}',
    );
    expect(explore.moodAndGenres, isNotEmpty);
    final moods = await yt.moodAndGenres();
    expect(moods, isNotEmpty);
    final browse = await yt.browse(
      moods.first.items.first.endpoint.browseId,
      moods.first.items.first.endpoint.params,
    );
    // ignore: avoid_print
    print(
      'browse ${moods.first.items.first.title}: ${browse.items.map((i) => '${i.title}(${i.items.length})').join(', ')}',
    );
    expect(browse.items, isNotEmpty);
  });

  test('stream resolution', () async {
    final songs = await yt.search('daft punk get lucky', YouTube.filterSong);
    final song = songs.items.whereType<SongItem>().first;
    final stream = await StreamResolver.instance.resolve(song.id);
    // ignore: avoid_print
    print(
      'stream for ${song.title}: client=${stream.clientName} mime=${stream.mimeType} br=${stream.bitrate} len=${stream.contentLength} url=${stream.url.substring(0, 60)}...',
    );
    expect(stream.url, startsWith('https://'));
  }, timeout: const Timeout(Duration(minutes: 2)));

  test('lyrics', () async {
    final lrc = await LrcLibProvider.instance.getLyrics(
      'Get Lucky',
      'Daft Punk',
      369,
    );
    // ignore: avoid_print
    print(
      'lrclib synced=${lrc != null && LyricsUtils.isSynced(lrc)} lines=${lrc == null ? 0 : LyricsUtils.parseLyrics(lrc).length}',
    );
    expect(lrc, isNotNull);
  });
}
