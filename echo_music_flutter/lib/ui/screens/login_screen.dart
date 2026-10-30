import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';

import '../../data/settings.dart';
import '../../data/sync.dart';
import '../../innertube/youtube.dart';
import '../../stream/stream_resolver.dart';

/// Google sign-in inside a WebView; captures the music.youtube.com cookies,
/// visitorData and dataSyncId (port of `LoginScreen.kt`).
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  bool _completing = false;
  String? _status;

  static const _loginUrl =
      'https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com';
  static const _userAgent =
      'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1';

  Future<void> _onLoadStop(
    InAppWebViewController controller,
    WebUri? url,
  ) async {
    final u = url?.toString() ?? '';
    if (!u.startsWith('https://music.youtube.com') || _completing) return;
    _completing = true;
    setState(() => _status = 'Finishing sign in…');
    try {
      final cookies = await CookieManager.instance().getCookies(
        url: WebUri('https://music.youtube.com'),
      );
      final cookieString = cookies
          .map((c) => '${c.name}=${c.value}')
          .join('; ');
      if (!cookieString.contains('SAPISID')) {
        _completing = false;
        setState(() => _status = null);
        return;
      }
      final visitor = (await controller.evaluateJavascript(
        source: 'window.yt && window.yt.config_ ? window.yt.config_.VISITOR_DATA : null',
      ))?.toString();
      final dataSync = (await controller.evaluateJavascript(
        source: 'window.yt && window.yt.config_ ? window.yt.config_.DATASYNC_ID : null',
      ))?.toString();
      final visitorData = (visitor == null || visitor == 'null')
          ? (YouTube.instance.visitorData ?? '')
          : visitor;
      final dataSyncId = (dataSync == null || dataSync == 'null')
          ? ''
          : dataSync.split('||').first;

      final yt = YouTube.instance;
      yt.cookie = cookieString;
      yt.dataSyncId = dataSyncId.isEmpty ? null : dataSyncId;
      if (visitorData.isNotEmpty) yt.visitorData = visitorData;
      final info = await yt.accountInfo();
      await Settings.instance.saveAccount(
        cookie: cookieString,
        visitorData: visitorData,
        dataSyncId: dataSyncId,
        name: info.name,
        email: info.email,
        channelHandle: info.channelHandle,
        avatarUrl: info.thumbnailUrl,
      );
      StreamResolver.instance.clearCache();
      SyncManager.instance.syncAll();
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      _completing = false;
      if (mounted) setState(() => _status = 'Sign-in validation failed: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Sign in'),
        leading: IconButton(
          icon: const Icon(Icons.close_rounded),
          onPressed: () => Navigator.of(context).pop(false),
        ),
      ),
      // flutter_inappwebview has no Linux implementation.
      body: Platform.isLinux
          ? const Center(
              child: Padding(
                padding: EdgeInsets.all(32),
                child: Text(
                  'Signing in isn\'t available on Linux yet. '
                  'Everything else works without an account.',
                  textAlign: TextAlign.center,
                ),
              ),
            )
          : Column(
              children: [
                if (_status != null)
                  Padding(
                    padding: const EdgeInsets.all(12),
                    child: Row(
                      children: [
                        const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        ),
                        const SizedBox(width: 12),
                        Expanded(child: Text(_status!)),
                      ],
                    ),
                  ),
                Expanded(
                  child: InAppWebView(
                    initialUrlRequest: URLRequest(url: WebUri(_loginUrl)),
                    initialSettings: InAppWebViewSettings(
                      javaScriptEnabled: true,
                      userAgent: _userAgent,
                      sharedCookiesEnabled: true,
                      thirdPartyCookiesEnabled: true,
                    ),
                    onWebViewCreated: (c) async {
                      await CookieManager.instance().deleteAllCookies();
                    },
                    onLoadStop: _onLoadStop,
                  ),
                ),
              ],
            ),
    );
  }
}
