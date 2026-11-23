export interface PoTokenResult {
  visitorData: string
  // null: no real PoToken is available server-side (see getPoToken)
  poToken: string | null
}

export type ClientContextType = 'ANDROID_VR' | 'ANDROID_MUSIC' | 'IOS' | 'WEB_REMIX'

let cachedVisitorData: string | null = null
let cacheExpiresAt = 0

export async function fetchVisitorData(): Promise<string> {
  const now = Date.now()
  if (cachedVisitorData && now < cacheExpiresAt) {
    return cachedVisitorData
  }

  try {
    const res = await fetch('https://music.youtube.com', {
      headers: {
        'User-Agent':
          'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
        Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
      },
    })

    const visitorHeader = res.headers.get('x-youtube-visitor-data')
    if (visitorHeader) {
      cachedVisitorData = visitorHeader
      cacheExpiresAt = now + 6 * 3600 * 1000
      return visitorHeader
    }

    const html = await res.text()
    const match = /"VISITOR_DATA":"([^"]+)"/.exec(html)
    if (match?.[1]) {
      cachedVisitorData = match[1]
      cacheExpiresAt = now + 6 * 3600 * 1000
      return match[1]
    }
  } catch {
    // Network fallback
  }

  const fallbackVisitor = generateFallbackVisitorData()
  cachedVisitorData = fallbackVisitor
  cacheExpiresAt = now + 1 * 3600 * 1000
  return fallbackVisitor
}

// A real PoToken must be minted by attestation (BotGuard / DNN challenge) in a browser or
// privileged client context. Fabricating one from visitorData would be a fake credential, so
// the token field is surfaced as unavailable and clients must not treat it as valid.
export async function getPoToken(visitorData?: string): Promise<PoTokenResult> {
  return {
    visitorData: visitorData || (await fetchVisitorData()),
    poToken: null,
  }
}

function generateFallbackVisitorData(): string {
  const bytes = new Uint8Array(16)
  for (let i = 0; i < 16; i++) {
    bytes[i] = Math.floor(Math.random() * 256)
  }
  return Buffer.from(bytes).toString('base64url')
}

export function buildClientContext(
  type: ClientContextType,
  visitorData?: string,
  poToken?: string,
): Record<string, unknown> {
  const baseClient: Record<string, unknown> = {
    hl: 'en',
    gl: 'US',
  }

  if (visitorData) {
    baseClient['visitorData'] = visitorData
  }

  switch (type) {
    case 'ANDROID_VR':
      return {
        client: {
          ...baseClient,
          clientName: 'ANDROID_VR',
          clientVersion: '1.58.21',
          platform: 'MOBILE',
        },
        user: {
          lockedSafetyMode: false,
        },
        request: {
          useSsl: true,
          internalExperimentFlags: [],
        },
      }

    case 'ANDROID_MUSIC':
      return {
        client: {
          ...baseClient,
          clientName: 'ANDROID_MUSIC',
          clientVersion: '7.10.51',
          androidSdkVersion: 34,
          osName: 'Android',
          osVersion: '14',
          platform: 'MOBILE',
        },
        user: {
          lockedSafetyMode: false,
        },
        request: {
          useSsl: true,
          internalExperimentFlags: [],
        },
      }

    case 'IOS':
      return {
        client: {
          ...baseClient,
          clientName: 'IOS',
          clientVersion: '19.29.1',
          deviceModel: 'iPhone16,2',
          osName: 'iOS',
          osVersion: '17.5.1.21F90',
          platform: 'MOBILE',
        },
        user: {
          lockedSafetyMode: false,
        },
        request: {
          useSsl: true,
          internalExperimentFlags: [],
        },
      }

    case 'WEB_REMIX':
    default:
      return {
        client: {
          ...baseClient,
          clientName: 'WEB_REMIX',
          clientVersion: '1.20240901.01.00',
          platform: 'DESKTOP',
          browserName: 'Chrome',
          browserVersion: '128.0.0.0',
        },
        user: {
          lockedSafetyMode: false,
        },
        request: {
          useSsl: true,
          internalExperimentFlags: [],
        },
        ...(poToken ? { serviceIntegrityDimensions: { poToken } } : {}),
      }
  }
}
