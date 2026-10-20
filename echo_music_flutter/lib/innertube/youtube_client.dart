/// InnerTube client identities, ported from `models/YouTubeClient.kt` and
/// the stream-fetch header rules in `utils/PlayerClient.kt`.
class YouTubeLocale {
  final String gl;
  final String hl;
  const YouTubeLocale({required this.gl, required this.hl});
}

class YouTubeClient {
  final String clientName;
  final String clientVersion;
  final String clientId;
  final String userAgent;
  final String? osName;
  final String? osVersion;
  final String? deviceMake;
  final String? deviceModel;
  final String? androidSdkVersion;
  final String? friendlyName;
  final bool loginSupported;
  final bool loginRequired;
  final bool useSignatureTimestamp;
  final bool isEmbedded;
  final bool useWebPoTokens;

  /// Origin header for browser-shaped clients; native clients send none.
  final String? origin;

  const YouTubeClient({
    required this.clientName,
    required this.clientVersion,
    required this.clientId,
    required this.userAgent,
    this.osName,
    this.osVersion,
    this.deviceMake,
    this.deviceModel,
    this.androidSdkVersion,
    this.friendlyName,
    this.loginSupported = false,
    this.loginRequired = false,
    this.useSignatureTimestamp = false,
    this.isEmbedded = false,
    this.useWebPoTokens = false,
    this.origin,
  });

  static const userAgentWeb =
      'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0';
  static const originYouTubeMusic = 'https://music.youtube.com';
  static const refererYouTubeMusic = '$originYouTubeMusic/';
  static const apiUrlYouTubeMusic = '$originYouTubeMusic/youtubei/v1/';
  static const originYouTube = 'https://www.youtube.com';

  Map<String, dynamic> toContext(
    YouTubeLocale locale,
    String? visitorData,
    String? dataSyncId,
  ) {
    final client = <String, dynamic>{
      'clientName': clientName,
      'clientVersion': clientVersion,
      if (osName != null) 'osName': osName,
      if (osVersion != null) 'osVersion': osVersion,
      if (deviceMake != null) 'deviceMake': deviceMake,
      if (deviceModel != null) 'deviceModel': deviceModel,
      if (androidSdkVersion != null) 'androidSdkVersion': androidSdkVersion,
      'gl': locale.gl,
      'hl': locale.hl,
      'visitorData': ?visitorData,
    };
    return {
      'client': client,
      'request': {'internalExperimentFlags': [], 'useSsl': true},
      'user': {
        'lockedSafetyMode': false,
        if (loginSupported && dataSyncId != null) 'onBehalfOfUser': dataSyncId,
      },
    };
  }

  /// Headers the *media* request must carry for a googlevideo URL this client
  /// minted (googlevideo compares them against `c=`/`cver=` in the URL).
  Map<String, String> mediaHeaders() => {
    'User-Agent': userAgent,
    'Origin': ?origin,
    if (origin != null) 'Referer': '$origin/',
  };

  static const web = YouTubeClient(
    clientName: 'WEB',
    clientVersion: '2.20260213.00.00',
    clientId: '1',
    userAgent: userAgentWeb,
    origin: originYouTube,
  );

  static const webRemix = YouTubeClient(
    clientName: 'WEB_REMIX',
    clientVersion: '1.20260213.01.00',
    clientId: '67',
    userAgent: userAgentWeb,
    loginSupported: true,
    useSignatureTimestamp: true,
    useWebPoTokens: true,
    origin: originYouTubeMusic,
  );

  static const tvHtml5 = YouTubeClient(
    clientName: 'TVHTML5',
    clientVersion: '7.20260213.00.00',
    clientId: '7',
    userAgent: 'Mozilla/5.0(SMART-TV; Linux; Tizen 4.0.0.2) AppleWebkit/605.1.15 (KHTML, like Gecko) SamsungBrowser/9.2 TV Safari/605.1.15',
    loginSupported: true,
    loginRequired: true,
    useSignatureTimestamp: true,
    useWebPoTokens: true,
    origin: originYouTube,
  );

  static const ios = YouTubeClient(
    clientName: 'IOS',
    clientVersion: '21.03.1',
    clientId: '5',
    userAgent: 'com.google.ios.youtube/21.03.1 (iPhone16,2; U; CPU iOS 18_2 like Mac OS X;)',
    osName: 'iPhone',
    osVersion: '18.2.22C152',
    deviceMake: 'Apple',
    deviceModel: 'iPhone16,2',
  );

  static const ipadOs = YouTubeClient(
    clientName: 'IOS',
    clientVersion: '21.03.3',
    clientId: '5',
    userAgent: 'com.google.ios.youtube/21.03.3 (iPad7,6; U; CPU iPadOS 17_7_10 like Mac OS X; en-US)',
    osName: 'iPadOS',
    osVersion: '17.7.10.21H450',
    deviceMake: 'Apple',
    deviceModel: 'iPad7,6',
    friendlyName: 'iPadOS',
  );

  /// Current ANDROID_VR pin (yt-dlp / YouTube.js). Returns direct-url
  /// formats; requires a visitorData.
  static const androidVr = YouTubeClient(
    clientName: 'ANDROID_VR',
    clientVersion: '1.65.10',
    clientId: '28',
    userAgent: 'com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip',
    osName: 'Android',
    osVersion: '12L',
    deviceMake: 'Oculus',
    deviceModel: 'Quest 3',
    androidSdkVersion: '32',
    friendlyName: 'Android VR 1.65',
  );

  static const androidVrLegacy = YouTubeClient(
    clientName: 'ANDROID_VR',
    clientVersion: '1.43.32',
    clientId: '28',
    userAgent: 'com.google.android.apps.youtube.vr.oculus/1.43.32 (Linux; U; Android 12; en_US; Quest 3; Build/SQ3A.220605.009.A1; Cronet/107.0.5284.2)',
    osName: 'Android',
    osVersion: '12',
    deviceMake: 'Oculus',
    deviceModel: 'Quest 3',
    androidSdkVersion: '32',
    friendlyName: 'Android VR 1.43',
  );

  /// Internal unreleased client; measured by the Android app to serve whole
  /// files with fully readable URLs.
  static const visionOs = YouTubeClient(
    clientName: 'VISIONOS',
    clientVersion: '0.1',
    clientId: '101',
    userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15',
    osName: 'visionOS',
    osVersion: '1.3.21O771',
    deviceMake: 'Apple',
    deviceModel: 'RealityDevice14,1',
    friendlyName: 'visionOS',
  );

  static const androidMusic = YouTubeClient(
    clientName: 'ANDROID_MUSIC',
    clientVersion: '8.39.42',
    clientId: '21',
    userAgent: 'com.google.android.apps.youtube.music/8.39.42 (Linux; U; Android 15; en_US; Pixel 9 Pro; Build/AP4A.250205.002) gzip',
    osName: 'Android',
    osVersion: '15',
    deviceMake: 'Google',
    deviceModel: 'Pixel 9 Pro',
    androidSdkVersion: '35',
    loginSupported: true,
  );

  static const android = YouTubeClient(
    clientName: 'ANDROID',
    clientVersion: '21.03.38',
    clientId: '3',
    userAgent:
        'com.google.android.youtube/21.03.38 (Linux; U; Android 14) gzip',
    osName: 'Android',
    osVersion: '14',
    androidSdkVersion: '34',
  );

  /// Picks the client whose identity a googlevideo URL carries (`c=` param) so
  /// the media fetch can be dressed as that client.
  static YouTubeClient forStreamUrl(String url) {
    final uri = Uri.tryParse(url);
    final name = uri?.queryParameters['c']?.toUpperCase();
    final version = uri?.queryParameters['cver'];
    if (name == null) return ios;
    if (name.startsWith('IOS')) return ios;
    if (name == 'ANDROID_VR') {
      return version == androidVrLegacy.clientVersion
          ? androidVrLegacy
          : androidVr;
    }
    if (name == 'ANDROID_MUSIC') return androidMusic;
    if (name.startsWith('ANDROID')) return android;
    if (name.startsWith('TVHTML5')) return tvHtml5;
    if (name == 'WEB_REMIX') return webRemix;
    if (name.startsWith('WEB') || name == 'MWEB') return web;
    if (name == 'VISIONOS') return visionOs;
    return ios;
  }
}
