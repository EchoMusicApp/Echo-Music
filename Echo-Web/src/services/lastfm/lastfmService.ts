import type { Track } from '../../types/domain'

export interface LastFmSession {
  sessionKey: string
  username: string
}

export async function getLastFmAuthUrl(callbackUrl?: string): Promise<string> {
  const cb = callbackUrl || window.location.origin + '/settings?tab=integrations'
  const res = await fetch(`/api/lastfm/auth-url?callback=${encodeURIComponent(cb)}`)
  if (!res.ok) {
    const err = (await res.json().catch(() => ({}))) as { error?: string }
    throw new Error(err.error || 'Last.fm authentication service is not configured on the server')
  }
  const data = (await res.json()) as { url: string }
  return data.url
}

export async function fetchLastFmSession(token: string): Promise<LastFmSession> {
  const res = await fetch(`/api/lastfm/session?token=${encodeURIComponent(token)}`)
  if (!res.ok) {
    const err = (await res.json().catch(() => ({}))) as { error?: string }
    throw new Error(err.error || 'Failed to authenticate with Last.fm')
  }
  return (await res.json()) as LastFmSession
}

export async function updateLastFmNowPlaying(track: Track, sessionKey: string): Promise<boolean> {
  try {
    const res = await fetch('/api/lastfm/now-playing', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionKey,
        track: track.title,
        artist: track.artistName || 'Unknown Artist',
        album: track.albumTitle || undefined,
        duration: track.durationSeconds || Math.round((track.durationMs || 180000) / 1000),
      }),
    })
    return res.ok
  } catch {
    return false
  }
}

export async function scrobbleLastFmTrack(
  track: Track,
  sessionKey: string,
  timestamp?: number,
): Promise<boolean> {
  try {
    const res = await fetch('/api/lastfm/scrobble', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionKey,
        track: track.title,
        artist: track.artistName || 'Unknown Artist',
        album: track.albumTitle || undefined,
        duration: track.durationSeconds || Math.round((track.durationMs || 180000) / 1000),
        timestamp: timestamp || Math.floor(Date.now() / 1000),
      }),
    })
    return res.ok
  } catch {
    return false
  }
}
