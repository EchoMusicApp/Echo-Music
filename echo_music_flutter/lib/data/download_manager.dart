import 'dart:async';
import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

import '../playback/media_metadata.dart';
import '../stream/stream_resolver.dart';
import 'database.dart';

enum DownloadState { queued, downloading, completed, failed }

class DownloadProgress {
  final DownloadState state;
  final double progress;
  const DownloadProgress(this.state, this.progress);
}

/// Offline downloads (port of `DownloadUtil` / `ExoDownloadService` on a
/// plain HTTP download into the app's documents directory).
class DownloadManager extends ChangeNotifier {
  DownloadManager._();
  static final DownloadManager instance = DownloadManager._();

  final Dio _dio = Dio(
    BaseOptions(
      connectTimeout: const Duration(seconds: 20),
      receiveTimeout: const Duration(minutes: 10),
    ),
  );

  final Map<String, DownloadProgress> _progress = {};
  final Map<String, CancelToken> _tokens = {};
  final List<MediaMetadata> _pending = [];
  int _active = 0;
  static const _maxParallel = 2;

  Map<String, DownloadProgress> get progress => Map.unmodifiable(_progress);
  DownloadProgress? stateOf(String id) => _progress[id];

  Set<String> _downloaded = {};
  bool _loaded = false;

  Future<void> load() async {
    if (_loaded) return;
    _downloaded = (await AppDatabase.instance.allDownloadPaths()).keys.toSet();
    for (final id in _downloaded) {
      _progress[id] = const DownloadProgress(DownloadState.completed, 1);
    }
    _loaded = true;
    notifyListeners();
  }

  bool isDownloaded(String id) => _downloaded.contains(id);

  Future<Directory> _dir() async {
    final base = await getApplicationDocumentsDirectory();
    final dir = Directory(p.join(base.path, 'downloads'));
    if (!await dir.exists()) await dir.create(recursive: true);
    return dir;
  }

  Future<void> download(MediaMetadata m) async {
    await load();
    if (_downloaded.contains(m.id) ||
        _progress[m.id]?.state == DownloadState.downloading) {
      return;
    }
    if (_pending.any((e) => e.id == m.id)) return;
    _progress[m.id] = const DownloadProgress(DownloadState.queued, 0);
    _pending.add(m);
    notifyListeners();
    _pump();
  }

  Future<void> downloadAll(List<MediaMetadata> items) async {
    for (final m in items) {
      await download(m);
    }
  }

  void _pump() {
    while (_active < _maxParallel && _pending.isNotEmpty) {
      final m = _pending.removeAt(0);
      _active++;
      _run(m).whenComplete(() {
        _active--;
        _pump();
      });
    }
  }

  Future<void> _run(MediaMetadata m) async {
    final token = CancelToken();
    _tokens[m.id] = token;
    _progress[m.id] = const DownloadProgress(DownloadState.downloading, 0);
    notifyListeners();
    try {
      final stream = await StreamResolver.instance.resolve(m.id);
      final ext = stream.mimeType.contains('webm') ? 'webm' : 'm4a';
      final dir = await _dir();
      final path = p.join(dir.path, '${m.id}.$ext');
      await _dio.download(
        stream.url,
        path,
        cancelToken: token,
        options: Options(headers: stream.headers),
        onReceiveProgress: (received, total) {
          if (total > 0) {
            _progress[m.id] = DownloadProgress(
              DownloadState.downloading,
              received / total,
            );
            notifyListeners();
          }
        },
      );
      final bytes = await File(path).length();
      await AppDatabase.instance.insertSong(m);
      await AppDatabase.instance.putDownload(
        m.id,
        path,
        stream.mimeType,
        bytes,
      );
      _downloaded.add(m.id);
      _progress[m.id] = const DownloadProgress(DownloadState.completed, 1);
    } catch (e) {
      debugPrint('download failed ${m.id}: $e');
      if (token.isCancelled) {
        _progress.remove(m.id);
      } else {
        _progress[m.id] = const DownloadProgress(DownloadState.failed, 0);
      }
    } finally {
      _tokens.remove(m.id);
      notifyListeners();
    }
  }

  Future<void> remove(String id) async {
    _tokens[id]?.cancel();
    _pending.removeWhere((e) => e.id == id);
    final path = await AppDatabase.instance.downloadPath(id);
    if (path != null) {
      final f = File(path);
      if (await f.exists()) await f.delete();
    }
    await AppDatabase.instance.removeDownload(id);
    _downloaded.remove(id);
    _progress.remove(id);
    notifyListeners();
  }

  Future<void> removeAll() async {
    for (final id in _downloaded.toList()) {
      await remove(id);
    }
  }
}
