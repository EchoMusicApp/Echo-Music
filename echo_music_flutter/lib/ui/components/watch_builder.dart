import 'package:flutter/material.dart';

import '../../data/database.dart';

/// Subscribes once to a reactive database query (see [AppDatabase.watch])
/// and rebuilds on changes. Unlike `StreamBuilder(stream: db.watch(...))`
/// inside `build`, the subscription survives parent rebuilds, so there is no
/// flicker or duplicate query. Pass [watchKey] to re-query when inputs change.
class WatchBuilder<T> extends StatefulWidget {
  final Future<T> Function() query;
  final Widget Function(BuildContext context, T? data) builder;
  final Object? watchKey;

  const WatchBuilder({
    super.key,
    required this.query,
    required this.builder,
    this.watchKey,
  });

  @override
  State<WatchBuilder<T>> createState() => _WatchBuilderState<T>();
}

class _WatchBuilderState<T> extends State<WatchBuilder<T>> {
  late Stream<T> _stream;

  @override
  void initState() {
    super.initState();
    _stream = AppDatabase.instance.watch(widget.query);
  }

  @override
  void didUpdateWidget(covariant WatchBuilder<T> old) {
    super.didUpdateWidget(old);
    if (old.watchKey != widget.watchKey) {
      _stream = AppDatabase.instance.watch(widget.query);
    }
  }

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<T>(
      stream: _stream,
      builder: (context, snap) => widget.builder(context, snap.data),
    );
  }
}
