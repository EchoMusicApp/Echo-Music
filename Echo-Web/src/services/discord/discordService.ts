import type { Track } from '../../types/domain'

export async function updateDiscordPresence(
  track: Track,
  isPlaying: boolean,
  positionSeconds = 0,
): Promise<void> {
  try {
    await fetch('/api/discord/presence', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        track: track.title,
        artist: track.artistName || 'Unknown Artist',
        album: track.albumTitle || undefined,
        durationMs:
          track.durationMs || (track.durationSeconds ? track.durationSeconds * 1000 : undefined),
        positionMs: Math.round(positionSeconds * 1000),
        isPlaying,
      }),
    })
  } catch {
    // Non-fatal if server or Discord is unavailable
  }
}

export async function clearDiscordPresence(): Promise<void> {
  try {
    await fetch('/api/discord/clear', {
      method: 'POST',
    })
  } catch {
    // Non-fatal
  }
}
