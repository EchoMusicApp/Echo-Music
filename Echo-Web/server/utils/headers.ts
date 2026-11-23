import type { ClientContextType } from './potoken.ts'

export interface ClientHeadersOptions {
  clientType?: ClientContextType
  visitorData?: string
}

export function getInnerTubeHeaders(options: ClientHeadersOptions = {}): Record<string, string> {
  const { clientType = 'WEB_REMIX', visitorData } = options
  const isAndroid = clientType === 'ANDROID_MUSIC'
  const isIos = clientType === 'IOS'
  const isAndroidVr = clientType === 'ANDROID_VR'

  let userAgent =
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36'
  let clientName = '67' // WEB_REMIX
  let clientVersion = '1.20241001.01.00'

  if (isAndroid) {
    userAgent = 'com.google.android.apps.youtube.music/7.10.51 (Linux; U; Android 14)'
    clientName = '21'
    clientVersion = '7.10.51'
  } else if (isIos) {
    userAgent = 'com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X)'
    clientName = '5'
    clientVersion = '19.29.1'
  } else if (isAndroidVr) {
    userAgent = 'Mozilla/5.0 (Linux; U; Android 12; Oculus Quest 2)'
    clientName = '28'
    clientVersion = '1.58.21'
  }

  const headers: Record<string, string> = {
    'User-Agent': userAgent,
    Origin: 'https://music.youtube.com',
    Referer: 'https://music.youtube.com/',
    'Content-Type': 'application/json',
    'X-YouTube-Client-Name': clientName,
    'X-YouTube-Client-Version': clientVersion,
  }

  if (visitorData) {
    headers['X-YouTube-Visitor-Data'] = visitorData
  }

  return headers
}
