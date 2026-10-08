import 'package:flutter/material.dart';

String formatDuration(int seconds) {
  if (seconds < 0) return '--:--';
  final h = seconds ~/ 3600;
  final m = (seconds % 3600) ~/ 60;
  final s = seconds % 60;
  if (h > 0) {
    return '$h:${m.toString().padLeft(2, '0')}:${s.toString().padLeft(2, '0')}';
  }
  return '$m:${s.toString().padLeft(2, '0')}';
}

String formatDurationMs(Duration d) => formatDuration(d.inSeconds);

String makeTimeString(int totalSeconds) {
  final h = totalSeconds ~/ 3600;
  final m = (totalSeconds % 3600) ~/ 60;
  if (h > 0) return '$h hr $m min';
  return '$m min';
}

String joinByBullet(List<String?> parts) =>
    parts.where((p) => p != null && p.isNotEmpty).join(' • ');

/// Port of `String.resize(w, h)` for YouTube image URLs.
String resizeThumbnail(String? url, int size) {
  if (url == null) return '';
  if (url.startsWith('content://') || url.startsWith('file://')) return url;
  if (url.contains('googleusercontent.com') ||
      url.contains('ggpht.com') ||
      url.contains('ytimg.com')) {
    if (url.contains('=w')) {
      return url.replaceAll(
        RegExp(r'=w\d+-h\d+[^&]*'),
        '=w$size-h$size-l90-rj',
      );
    }
    if (url.contains('=s')) {
      return url.replaceAll(RegExp(r'=s\d+[^&]*'), '=s$size-l90-rj');
    }
    if (!url.contains('ytimg.com')) return '$url=w$size-h$size-l90-rj';
  }
  return url;
}

String formatCount(int n) {
  if (n >= 1000000000) return '${(n / 1000000000).toStringAsFixed(1)}B';
  if (n >= 1000000) return '${(n / 1000000).toStringAsFixed(1)}M';
  if (n >= 1000) return '${(n / 1000).toStringAsFixed(1)}K';
  return '$n';
}

String formatBytes(int bytes) {
  if (bytes >= 1 << 30) return '${(bytes / (1 << 30)).toStringAsFixed(2)} GB';
  if (bytes >= 1 << 20) return '${(bytes / (1 << 20)).toStringAsFixed(1)} MB';
  if (bytes >= 1 << 10) return '${(bytes / (1 << 10)).toStringAsFixed(0)} KB';
  return '$bytes B';
}

void showSnack(BuildContext context, String message) {
  final messenger = ScaffoldMessenger.maybeOf(context);
  messenger?.hideCurrentSnackBar();
  messenger?.showSnackBar(
    SnackBar(
      content: Text(message),
      behavior: SnackBarBehavior.floating,
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 150),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
    ),
  );
}

String relativeDay(DateTime d) {
  final now = DateTime.now();
  final today = DateTime(now.year, now.month, now.day);
  final day = DateTime(d.year, d.month, d.day);
  final diff = today.difference(day).inDays;
  if (diff == 0) return 'Today';
  if (diff == 1) return 'Yesterday';
  if (diff < 7) return '$diff days ago';
  const months = [
    'Jan',
    'Feb',
    'Mar',
    'Apr',
    'May',
    'Jun',
    'Jul',
    'Aug',
    'Sep',
    'Oct',
    'Nov',
    'Dec',
  ];
  return '${months[d.month - 1]} ${d.day}${d.year != now.year ? ', ${d.year}' : ''}';
}
