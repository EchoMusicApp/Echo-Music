import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math';

import 'package:audio_service/audio_service.dart';
import 'package:audio_session/audio_session.dart';
import 'package:flutter/foundation.dart';
import 'package:just_audio/just_audio.dart';

import '../data/database.dart';
import '../data/settings.dart';
import '../innertube/json_utils.dart';
import '../innertube/youtube.dart';
import '../innertube/youtube_client.dart';
import '../stream/stream_resolver.dart';
import 'media_metadata.dart';
import 'queues.dart';

/// The playback engine — port of the queue / history / radio behaviour of
/// `MusicService.kt` on top of `just_audio` + `audio_service`.
///
/// Items are resolved lazily (the stream URL is fetched only when a track
/// becomes current, like the Android `ResolvingDataSource`), the next item is
/// pre-resolved for fast transitions, and the queue persists across launches.
class EchoAudioHandler extends BaseAudioHandler with SeekHandler {
  EchoAudioHandler() {
    _init();
  }

  final AudioPlayer _player = AudioPlayer(useProxyForRequestHeaders: false);
  AudioPlayer get player => _player;

  final List<MediaMetadata> _items = [];
  int _index = -1;
  PlayQueue? _queue;
  String? _queueTitle;
  bool _shuffle = false;
  List<MediaMetadata>? _unshuffled;
  AudioServiceRepeatMode _repeat = AudioServiceRepeatMode.none;
  bool _loadingMore = false;
  int _consecutiveErrors = 0;
  int _loadGeneration = 0;

  final ValueNotifier<List<MediaMetadata>> queueItems = ValueNotifier(const []);
  final ValueNotifier<int> currentIndex = ValueNotifier(-1);
  final ValueNotifier<MediaMetadata?> currentMetadata = ValueNotifier(null);
  final ValueNotifier<String?> queueTitleNotifier = ValueNotifier(null);
  final ValueNotifier<bool> shuffleEnabled = ValueNotifier(false);
  final ValueNotifier<AudioServiceRepeatMode> repeatMode = ValueNotifier(
    AudioServiceRepeatMode.none,
  );
  final ValueNotifier<String?> error = ValueNotifier(null);
  final ValueNotifier<bool> isLoadingItem = ValueNotifier(false);
  final ValueNotifier<DateTime?> sleepTimerEnd = ValueNotifier(null);
  final ValueNotifier<bool> sleepAtEndOfSong = ValueNotifier(false);
  final ValueNotifier<ResolvedStream?> currentStream = ValueNotifier(null);

  /// Duration to show in the UI. AVPlayer reports twice the real length for
  /// YouTube's fragmented-MP4 audio, so when InnerTube's `lengthSeconds` is
  /// known and the player disagrees by more than 25 %, InnerTube wins.
  final ValueNotifier<Duration?> effectiveDuration = ValueNotifier(null);
  int _authoritativeSeconds = 0;
  bool _endGuardFired = false;

  Duration? _computeEffectiveDuration() {
    final reported = _player.duration;
    if (_authoritativeSeconds > 0) {
      final auth = Duration(seconds: _authoritativeSeconds);
      if (reported == null) return auth;
      final ratio = reported.inMilliseconds / auth.inMilliseconds;
      if (ratio > 1.25 || ratio < 0.75) return auth;
    }
    return reported;
  }

  void _refreshEffectiveDuration() {
    effectiveDuration.value = _computeEffectiveDuration();
    final item = mediaItem.valueOrNull;
    final d = effectiveDuration.value;
    if (item != null && d != null && item.duration != d) {
      mediaItem.add(item.copyWith(duration: d));
    }
  }

  Timer? _sleepTimer;
  Timer? _persistTimer;

  // Listening-history bookkeeping
  String? _historyItemId;
  Duration _lastPositionForHistory = Duration.zero;
  Duration _accumulatedPlay = Duration.zero;
  bool _historyRecorded = false;
  // The event saved after 10 s of play; finished with the real play time.
  Future<int?>? _historyEvent;

  Settings get _settings => Settings.instance;

  Future<void> _init() async {
    final session = await AudioSession.instance;
    await session.configure(const AudioSessionConfiguration.music());
    session.interruptionEventStream.listen((event) {
      if (event.begin) {
        if (event.type != AudioInterruptionType.duck) pause();
      } else if (event.type == AudioInterruptionType.pause) {
        play();
      }
    });
    session.becomingNoisyEventStream.listen((_) => pause());

    _player.playbackEventStream.listen(
      _broadcastState,
      onError: (Object e, StackTrace st) {
        debugPrint('playback error: $e');
      },
    );
    _player.playerStateStream.listen((state) {
      if (state.processingState == ProcessingState.completed) {
        _onCompleted();
      }
    });
    _player.positionStream.listen(_trackHistory);
    _player.durationStream.listen((d) {
      _refreshEffectiveDuration();
      final meta = currentMetadata.value;
      final eff = effectiveDuration.value;
      if (meta != null && meta.duration <= 0 && eff != null) {
        final updated = meta.copyWith(duration: eff.inSeconds);
        if (_index >= 0 && _index < _items.length) _items[_index] = updated;
        currentMetadata.value = updated;
        _publishQueue();
      }
    });

    if (_settings.rememberShuffleAndRepeat) {
      _shuffle = _settings.savedShuffle;
      shuffleEnabled.value = _shuffle;
      _repeat =
          AudioServiceRepeatMode.values[_settings.savedRepeat.clamp(0, 2)];
      repeatMode.value = _repeat;
      _applyLoopMode();
    }
    await _restoreQueue();
  }

  // -------------------------------------------------------------------
  // State broadcasting
  // -------------------------------------------------------------------

  void _broadcastState(PlaybackEvent event) {
    final playing = _player.playing;
    playbackState.add(
      playbackState.value.copyWith(
        controls: [
          MediaControl.skipToPrevious,
          if (playing) MediaControl.pause else MediaControl.play,
          MediaControl.skipToNext,
        ],
        systemActions: const {
          MediaAction.seek,
          MediaAction.seekForward,
          MediaAction.seekBackward,
        },
        androidCompactActionIndices: const [0, 1, 2],
        processingState: {
          ProcessingState.idle: AudioProcessingState.idle,
          ProcessingState.loading: AudioProcessingState.loading,
          ProcessingState.buffering: AudioProcessingState.buffering,
          ProcessingState.ready: AudioProcessingState.ready,
          ProcessingState.completed: AudioProcessingState.completed,
        }[_player.processingState]!,
        playing: playing,
        updatePosition: _player.position,
        bufferedPosition: _player.bufferedPosition,
        speed: _player.speed,
        queueIndex: _index,
        repeatMode: _repeat,
        shuffleMode: _shuffle
            ? AudioServiceShuffleMode.all
            : AudioServiceShuffleMode.none,
      ),
    );
  }

  void _publishQueue() {
    queueItems.value = List.unmodifiable(_items);
    currentIndex.value = _index;
    queue.add(_items.map((m) => m.toMediaItem()).toList());
    queueTitleNotifier.value = _queueTitle;
    queueTitle.add(_queueTitle ?? '');
    _schedulePersist();
  }

  // -------------------------------------------------------------------
  // Public queue API (port of MusicService.playQueue / playNext / addToQueue)
  // -------------------------------------------------------------------

  Future<void> playQueue(PlayQueue queue, {bool playWhenReady = true}) async {
    _queue = queue;
    bool cancelled() => !identical(_queue, queue);
    error.value = null;
    isLoadingItem.value = true;
    final preload = queue.preloadItem;
    if (preload != null) {
      _items
        ..clear()
        ..add(preload);
      _index = 0;
      _unshuffled = null;
      currentMetadata.value = preload;
      mediaItem.add(preload.toMediaItem());
      _publishQueue();
      await _loadCurrent(playWhenReady: playWhenReady);
    }
    try {
      var status = await queue.getInitialStatus();
      status = status.filtered(
        (m) =>
            !(_settings.hideExplicit && m.explicit) &&
            !(_settings.hideVideoSongs && m.isVideoSong),
      );
      if (cancelled()) return;
      if (status.items.isEmpty) {
        if (preload == null) error.value = 'Nothing to play';
        isLoadingItem.value = false;
        return;
      }
      _queueTitle = status.title;
      if (preload != null) {
        // Keep the already-playing preload item; merge the rest of the queue
        // around it so playback does not restart.
        final idx = status.items.indexWhere((m) => m.id == preload.id);
        final rest = [...status.items];
        if (idx >= 0) rest.removeAt(idx);
        _items
          ..clear()
          ..add(preload)
          ..addAll(idx >= 0 ? rest.sublist(0) : rest);
        _index = 0;
        _unshuffled = null;
        _publishQueue();
        isLoadingItem.value = false;
        _preResolveNext();
        return;
      }
      _items
        ..clear()
        ..addAll(status.items);
      _index = status.mediaItemIndex.clamp(0, _items.length - 1);
      _unshuffled = null;
      if (_shuffle) _applyShuffleOrder(keepCurrent: true);
      _publishQueue();
      await _loadCurrent(
        playWhenReady: playWhenReady,
        initialPosition: status.position,
      );
    } catch (e) {
      if (cancelled()) return;
      isLoadingItem.value = false;
      if (preload == null) error.value = 'Failed to load queue: $e';
      debugPrint('playQueue failed: $e');
    }
  }

  Future<void> playItems(
    List<MediaMetadata> items, {
    int startIndex = 0,
    String? title,
  }) =>
      playQueue(ListQueue(title: title, items: items, startIndex: startIndex));

  Future<void> playNext(List<MediaMetadata> items) async {
    if (items.isEmpty) return;
    if (_items.isEmpty) {
      await playItems(items);
      return;
    }
    _items.insertAll(_index + 1, items);
    _publishQueue();
    _preResolveNext();
  }

  Future<void> addToQueue(List<MediaMetadata> items) async {
    if (items.isEmpty) return;
    if (_items.isEmpty) {
      await playItems(items);
      return;
    }
    _items.addAll(items);
    _publishQueue();
  }

  Future<void> removeFromQueue(int index) async {
    if (index < 0 || index >= _items.length) return;
    if (index == _index) {
      _items.removeAt(index);
      if (_items.isEmpty) {
        await stop();
        _index = -1;
        currentMetadata.value = null;
        _publishQueue();
        return;
      }
      _index = min(index, _items.length - 1);
      _publishQueue();
      await _loadCurrent(playWhenReady: _player.playing);
      return;
    }
    _items.removeAt(index);
    if (index < _index) _index--;
    _publishQueue();
  }

  Future<void> moveInQueue(int from, int to) async {
    if (from < 0 || from >= _items.length || to < 0 || to >= _items.length) {
      return;
    }
    final item = _items.removeAt(from);
    _items.insert(to, item);
    if (from == _index) {
      _index = to;
    } else if (from < _index && to >= _index) {
      _index--;
    } else if (from > _index && to <= _index) {
      _index++;
    }
    _publishQueue();
  }

  Future<void> clearQueue() async {
    _items.clear();
    _index = -1;
    _queue = null;
    _queueTitle = null;
    _unshuffled = null;
    await _player.stop();
    currentMetadata.value = null;
    mediaItem.add(null);
    _publishQueue();
  }

  /// Start a radio from the current song, keeping it playing (port of
  /// `startRadioSeamlessly`).
  Future<void> startRadioSeamlessly() async {
    final current = currentMetadata.value;
    if (current == null) return;
    final q = YouTubeQueue.radio(current);
    _queue = q;
    try {
      final status = await q.getInitialStatus();
      if (!identical(_queue, q)) return;
      final rest = status.items.where((m) => m.id != current.id).toList();
      _items
        ..removeRange(_index + 1, _items.length)
        ..addAll(rest);
      _queueTitle = status.title;
      _publishQueue();
      _preResolveNext();
    } catch (e) {
      debugPrint('radio failed: $e');
    }
  }

  // -------------------------------------------------------------------
  // Transport
  // -------------------------------------------------------------------

  @override
  Future<void> play() async {
    if (_player.processingState == ProcessingState.idle &&
        _index >= 0 &&
        _index < _items.length) {
      await _loadCurrent(playWhenReady: true);
      return;
    }
    if (_player.processingState == ProcessingState.completed) {
      await _player.seek(Duration.zero);
    } else if (_authoritativeSeconds > 0 &&
        _player.position.inMilliseconds >=
            _authoritativeSeconds * 1000 - 1500) {
      // Stopped by the end guard: restart instead of playing past the real end.
      await _player.seek(Duration.zero);
      _endGuardFired = false;
    }
    await _player.play();
  }

  @override
  Future<void> pause() => _player.pause();

  @override
  Future<void> stop() async {
    await _player.stop();
    await _flushHistory();
    await super.stop();
  }

  @override
  Future<void> seek(Duration position) {
    var target = position;
    if (_authoritativeSeconds > 0) {
      final max =
          Duration(seconds: _authoritativeSeconds) -
          const Duration(milliseconds: 1500);
      if (target > max) target = max < Duration.zero ? Duration.zero : max;
    }
    return _player.seek(target);
  }

  @override
  Future<void> skipToNext() async {
    if (_items.isEmpty) return;
    await _flushHistory();
    if (_index + 1 < _items.length) {
      _index++;
    } else if (_repeat == AudioServiceRepeatMode.all) {
      _index = 0;
    } else if (await _loadMore()) {
      _index++;
    } else {
      return;
    }
    _publishQueue();
    await _loadCurrent(playWhenReady: true);
  }

  @override
  Future<void> skipToPrevious() async {
    if (_items.isEmpty) return;
    if (_player.position > const Duration(seconds: 3) || _index == 0) {
      await _player.seek(Duration.zero);
      if (!_player.playing) await _player.play();
      return;
    }
    await _flushHistory();
    _index--;
    _publishQueue();
    await _loadCurrent(playWhenReady: true);
  }

  @override
  Future<void> skipToQueueItem(int index) async {
    if (index < 0 || index >= _items.length) return;
    await _flushHistory();
    _index = index;
    _publishQueue();
    await _loadCurrent(playWhenReady: true);
  }

  @override
  Future<void> setRepeatMode(AudioServiceRepeatMode repeatMode) async {
    _repeat = repeatMode == AudioServiceRepeatMode.group
        ? AudioServiceRepeatMode.all
        : repeatMode;
    this.repeatMode.value = _repeat;
    _settings.savedRepeat = _repeat.index;
    _applyLoopMode();
    _broadcastState(_player.playbackEvent);
  }

  void _applyLoopMode() {
    _player.setLoopMode(
      _repeat == AudioServiceRepeatMode.one ? LoopMode.one : LoopMode.off,
    );
  }

  Future<void> toggleRepeat() async {
    switch (_repeat) {
      case AudioServiceRepeatMode.none:
        await setRepeatMode(AudioServiceRepeatMode.all);
      case AudioServiceRepeatMode.all:
        await setRepeatMode(AudioServiceRepeatMode.one);
      default:
        await setRepeatMode(AudioServiceRepeatMode.none);
    }
  }

  @override
  Future<void> setShuffleMode(AudioServiceShuffleMode shuffleMode) async {
    final enable = shuffleMode != AudioServiceShuffleMode.none;
    if (enable == _shuffle) return;
    _shuffle = enable;
    shuffleEnabled.value = enable;
    _settings.savedShuffle = enable;
    if (enable) {
      _applyShuffleOrder(keepCurrent: true);
    } else {
      _restoreOrder();
    }
    _publishQueue();
    _broadcastState(_player.playbackEvent);
  }

  Future<void> toggleShuffle() => setShuffleMode(
    _shuffle ? AudioServiceShuffleMode.none : AudioServiceShuffleMode.all,
  );

  void _applyShuffleOrder({required bool keepCurrent}) {
    if (_items.isEmpty) return;
    _unshuffled = List.of(_items);
    final current = (_index >= 0 && _index < _items.length)
        ? _items[_index]
        : null;
    final rest = List.of(_items);
    if (current != null) rest.removeAt(_index);
    rest.shuffle(Random());
    _items
      ..clear()
      ..addAll([?current, ...rest]);
    _index = current != null ? 0 : 0;
  }

  void _restoreOrder() {
    final original = _unshuffled;
    if (original == null) return;
    final current = (_index >= 0 && _index < _items.length)
        ? _items[_index]
        : null;
    // Keep items added during shuffle.
    final extra = _items
        .where((m) => !original.any((o) => identical(o, m) || o.id == m.id))
        .toList();
    _items
      ..clear()
      ..addAll(original)
      ..addAll(extra);
    _unshuffled = null;
    if (current != null) {
      final i = _items.indexWhere((m) => m.id == current.id);
      _index = i >= 0 ? i : 0;
    }
  }

  @override
  Future<void> setSpeed(double speed) => _player.setSpeed(speed);

  @override
  Future<void> onTaskRemoved() async {
    await _flushHistory();
    await super.onTaskRemoved();
  }

  // -------------------------------------------------------------------
  // Loading
  // -------------------------------------------------------------------

  Future<void> _loadCurrent({
    required bool playWhenReady,
    Duration? initialPosition,
  }) async {
    if (_index < 0 || _index >= _items.length) return;
    final gen = ++_loadGeneration;
    final meta = _items[_index];
    currentMetadata.value = meta;
    mediaItem.add(meta.toMediaItem());
    error.value = null;
    isLoadingItem.value = true;
    _startHistory(meta);
    _authoritativeSeconds = meta.duration > 0 ? meta.duration : 0;
    _endGuardFired = false;
    _refreshEffectiveDuration();
    try {
      AudioSource source;
      if (meta.isLocal && meta.localPath != null) {
        source = AudioSource.file(meta.localPath!, tag: meta.toMediaItem());
        currentStream.value = null;
      } else {
        final local = await AppDatabase.instance.downloadPath(meta.id);
        // Stream instead if the downloaded file has gone missing.
        if (local != null && await File(local).exists()) {
          source = AudioSource.file(local, tag: meta.toMediaItem());
          currentStream.value = null;
        } else {
          debugPrint('[player] resolving ${meta.id}');
          final stream = await StreamResolver.instance.resolve(meta.id);
          debugPrint('[player] resolved ${meta.id} -> ${stream.clientName}');
          if (gen != _loadGeneration) {
            debugPrint(
              '[player] stale load for ${meta.id} (gen $gen vs $_loadGeneration)',
            );
            return;
          }
          currentStream.value = stream;
          source = AudioSource.uri(
            Uri.parse(stream.url),
            headers: stream.headers,
            tag: meta.toMediaItem(),
          );
          if (stream.durationSeconds != null && stream.durationSeconds! > 0) {
            _authoritativeSeconds = stream.durationSeconds!;
            if (meta.duration <= 0) {
              _items[_index] = meta.copyWith(duration: stream.durationSeconds);
              currentMetadata.value = _items[_index];
            }
          }
          _applyNormalization(stream.loudnessDb);
        }
      }
      if (gen != _loadGeneration) return;
      debugPrint('[player] setAudioSource ${meta.id} gen=$gen');
      final duration = await _player.setAudioSource(
        source,
        initialPosition: initialPosition,
      );
      debugPrint(
        '[player] loaded ${meta.id} duration=$duration state=${_player.processingState} gen=$gen/$_loadGeneration',
      );
      if (gen != _loadGeneration) return;
      _consecutiveErrors = 0;
      isLoadingItem.value = false;
      if (playWhenReady) {
        unawaited(_player.play());
      }
      _preResolveNext();
      _registerRemotePlayback(meta);
      unawaited(AppDatabase.instance.insertSong(meta));
    } on PlayerException catch (e) {
      if (gen != _loadGeneration) return;
      debugPrint('PlayerException ${e.code}: ${e.message}');
      await _handleLoadError(
        meta,
        '${e.message}',
        playWhenReady,
        refusedCode: e.code,
      );
    } catch (e) {
      if (gen != _loadGeneration) return;
      debugPrint('load error: $e');
      await _handleLoadError(meta, '$e', playWhenReady);
    }
  }

  Future<void> _handleLoadError(
    MediaMetadata meta,
    String message,
    bool playWhenReady, {
    int? refusedCode,
  }) async {
    isLoadingItem.value = false;
    _consecutiveErrors++;
    final stream = currentStream.value;
    if (stream != null && _consecutiveErrors == 1) {
      // Googlevideo refused the minted URL → exclude that client and retry once.
      StreamResolver.instance.onRefused(meta.id, stream.url);
      await _loadCurrent(playWhenReady: playWhenReady);
      return;
    }
    error.value = message;
    if (_settings.autoSkipNextOnError &&
        _consecutiveErrors < 4 &&
        _index + 1 < _items.length) {
      await skipToNext();
    }
  }

  void _applyNormalization(double? loudnessDb) {
    if (!_settings.audioNormalization || loudnessDb == null) {
      _player.setVolume(1.0);
      return;
    }
    // Target -14 LUFS like the Android app's AudioNormalization; only attenuate.
    final gainDb = (-14.0 - loudnessDb).clamp(-20.0, 0.0);
    final linear = pow(10, gainDb / 20).toDouble();
    _player.setVolume(linear.clamp(0.2, 1.0));
  }

  void _preResolveNext() {
    if (_index + 1 >= _items.length) {
      if (_settings.autoLoadMore) unawaited(_loadMore());
      return;
    }
    final next = _items[_index + 1];
    if (next.isLocal) return;
    unawaited(
      StreamResolver.instance
          .resolve(next.id)
          .catchError((Object e) {
            debugPrint('pre-resolve failed: $e');
            return Future<ResolvedStream>.error(e);
          })
          .then((_) {}, onError: (_) {}),
    );
    if (_items.length - _index <= 5 && _settings.autoLoadMore) {
      unawaited(_loadMore());
    }
  }

  Future<bool> _loadMore() async {
    final q = _queue;
    if (q == null || _loadingMore || !q.hasNextPage()) return false;
    if (_repeat == AudioServiceRepeatMode.all) return false;
    _loadingMore = true;
    try {
      var items = await q.nextPage();
      items = items
          .where(
            (m) =>
                !(_settings.hideExplicit && m.explicit) &&
                !(_settings.hideVideoSongs && m.isVideoSong),
          )
          .toList();
      final existing = _items.map((m) => m.id).toSet();
      items = items.where((m) => !existing.contains(m.id)).toList();
      if (items.isEmpty) return false;
      _items.addAll(items);
      _publishQueue();
      return true;
    } catch (e) {
      debugPrint('loadMore failed: $e');
      return false;
    } finally {
      _loadingMore = false;
    }
  }

  Future<void> _onCompleted() async {
    if (_repeat == AudioServiceRepeatMode.one) {
      return; // handled by LoopMode.one
    }
    await _flushHistory();
    if (sleepAtEndOfSong.value) {
      sleepAtEndOfSong.value = false;
      await _player.pause();
      await _player.seek(Duration.zero);
      return;
    }
    if (_index + 1 < _items.length) {
      _index++;
    } else if (_repeat == AudioServiceRepeatMode.all && _items.isNotEmpty) {
      _index = 0;
    } else if (await _loadMore()) {
      _index++;
    } else {
      await _player.pause();
      return;
    }
    _publishQueue();
    await _loadCurrent(playWhenReady: true);
  }

  // -------------------------------------------------------------------
  // History
  // -------------------------------------------------------------------

  void _startHistory(MediaMetadata meta) {
    _historyItemId = meta.id;
    _lastPositionForHistory = Duration.zero;
    _accumulatedPlay = Duration.zero;
    _historyRecorded = false;
    _historyEvent = null;
  }

  void _trackHistory(Duration position) {
    _endGuard(position);
    if (!_player.playing || _historyItemId == null) return;
    final delta = position - _lastPositionForHistory;
    if (delta > Duration.zero && delta < const Duration(seconds: 2)) {
      _accumulatedPlay += delta;
    }
    _lastPositionForHistory = position;
    if (!_historyRecorded && _accumulatedPlay >= const Duration(seconds: 10)) {
      _historyRecorded = true;
      if (!_settings.pauseListenHistory) {
        _historyEvent = AppDatabase.instance
            .addEvent(_historyItemId!, 0)
            .then<int?>((id) => id)
            .catchError((Object e) {
              debugPrint('history event failed: $e');
              return null;
            });
      }
    }
  }

  /// AVPlayer may never emit `completed` when its (wrong) duration exceeds the
  /// real stream length: it stalls at EOF instead. Advance manually.
  void _endGuard(Duration position) {
    if (_endGuardFired || _authoritativeSeconds <= 0) return;
    final reported = _player.duration;
    if (reported == null || reported.inSeconds <= _authoritativeSeconds + 2) {
      return;
    }
    final nearEnd =
        position.inMilliseconds >= _authoritativeSeconds * 1000 - 700;
    final stalled =
        _player.processingState == ProcessingState.buffering &&
        position.inMilliseconds >= _authoritativeSeconds * 1000 - 3000;
    if (nearEnd || stalled) {
      _endGuardFired = true;
      debugPrint(
        '[player] end guard: pos=$position auth=${_authoritativeSeconds}s reported=$reported',
      );
      unawaited(_onCompleted());
    }
  }

  Future<void> _flushHistory() async {
    final id = _historyItemId;
    if (id == null) return;
    final ms = _accumulatedPlay.inMilliseconds;
    final event = _historyEvent;
    _historyItemId = null;
    _historyEvent = null;
    _accumulatedPlay = Duration.zero;
    if (ms >= 10000 && !_settings.pauseListenHistory) {
      try {
        final eventId = await event;
        if (eventId != null) {
          await AppDatabase.instance.finishEvent(eventId, id, ms);
        } else {
          await AppDatabase.instance.addEvent(id, ms);
        }
      } catch (e) {
        debugPrint('history flush failed: $e');
      }
    }
  }

  void _registerRemotePlayback(MediaMetadata meta) {
    final yt = YouTube.instance;
    if (!yt.isLoggedIn || _settings.pauseRemoteListenHistory || meta.isLocal) {
      return;
    }
    unawaited(() async {
      try {
        final res = await yt.player(meta.id, client: YouTubeClient.webRemix);
        final url = js(res, [
          'playbackTracking',
          'videostatsPlaybackUrl',
          'baseUrl',
        ]);
        if (url != null) await yt.registerPlayback(url);
      } catch (e) {
        debugPrint('registerPlayback failed: $e');
      }
    }());
  }

  // -------------------------------------------------------------------
  // Sleep timer
  // -------------------------------------------------------------------

  void startSleepTimer(Duration duration) {
    _sleepTimer?.cancel();
    sleepAtEndOfSong.value = false;
    sleepTimerEnd.value = DateTime.now().add(duration);
    _sleepTimer = Timer(duration, () {
      sleepTimerEnd.value = null;
      pause();
    });
  }

  void sleepAtEnd() {
    _sleepTimer?.cancel();
    sleepTimerEnd.value = null;
    sleepAtEndOfSong.value = true;
  }

  void cancelSleepTimer() {
    _sleepTimer?.cancel();
    sleepTimerEnd.value = null;
    sleepAtEndOfSong.value = false;
  }

  // -------------------------------------------------------------------
  // Persistence
  // -------------------------------------------------------------------

  void _schedulePersist() {
    _persistTimer?.cancel();
    _persistTimer = Timer(const Duration(seconds: 2), _persistQueue);
  }

  Future<void> _persistQueue() async {
    if (!_settings.persistentQueue) return;
    if (_items.isEmpty) {
      await _settings.setPersistedQueue(null);
      return;
    }
    final data = {
      'title': _queueTitle,
      'index': _index,
      'position': _player.position.inMilliseconds,
      'items': _items.map((m) => m.toJson()).toList(),
    };
    await _settings.setPersistedQueue(jsonEncode(data));
  }

  Future<void> _restoreQueue() async {
    if (!_settings.persistentQueue) return;
    final raw = _settings.persistedQueue;
    if (raw == null) return;
    try {
      final data = jsonDecode(raw) as Map<String, dynamic>;
      final items = (data['items'] as List)
          .map(
            (e) => MediaMetadata.fromJson(Map<String, dynamic>.from(e as Map)),
          )
          .toList();
      if (items.isEmpty) return;
      _items
        ..clear()
        ..addAll(items);
      _queueTitle = data['title'] as String?;
      _index = (data['index'] as int? ?? 0).clamp(0, items.length - 1);
      currentMetadata.value = _items[_index];
      mediaItem.add(_items[_index].toMediaItem());
      _publishQueue();
      final pos = Duration(milliseconds: data['position'] as int? ?? 0);
      // Load lazily but paused so the mini player shows the last track.
      unawaited(_loadCurrent(playWhenReady: false, initialPosition: pos));
    } catch (e) {
      debugPrint('restore queue failed: $e');
    }
  }

  Future<void> savePositionNow() => _persistQueue();
}
