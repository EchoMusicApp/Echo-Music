import { Hono } from 'hono'
import type { Track } from '../../src/types/audio.ts'
import { buildClientContext, fetchVisitorData } from '../utils/potoken.ts'

export const radioRouter = new Hono()

radioRouter.get('/continuation', async (c) => {
  const videoId = c.req.query('videoId')
  const continuationToken = c.req.query('continuation')

  if (!videoId && !continuationToken) {
    return c.json({ error: 'videoId or continuation parameter is required' }, 400)
  }

  try {
    const radioData = await fetchInnerTubeRadio(videoId, continuationToken)
    return c.json(radioData)
  } catch (err: unknown) {
    return c.json(
      {
        error: 'Failed to fetch radio continuation',
        message: err instanceof Error ? err.message : String(err),
      },
      502,
    )
  }
})

interface RunItem {
  text?: string
  navigationEndpoint?: {
    browseEndpoint?: {
      browseId?: string
    }
  }
}

interface InnerTubeItem {
  playlistPanelVideoRenderer?: {
    videoId?: string
    title?: { runs?: Array<{ text?: string }> }
    longBylineText?: {
      runs?: RunItem[]
    }
    shortBylineText?: {
      runs?: RunItem[]
    }
    thumbnail?: { thumbnails?: Array<{ url?: string }> }
    lengthText?: { runs?: Array<{ text?: string }> }
  }
}

async function fetchInnerTubeRadio(
  videoId?: string,
  continuationToken?: string,
): Promise<{ tracks: Track[]; continuationToken?: string }> {
  const visitorData = await fetchVisitorData()
  const context = buildClientContext('WEB_REMIX', visitorData)

  const payload: Record<string, unknown> = {
    context,
  }

  if (continuationToken) {
    payload['continuation'] = continuationToken
  } else if (videoId) {
    payload['videoId'] = videoId
    payload['playlistId'] = `RDAMVM${videoId}`
    payload['isAudioOnly'] = true
  }

  const res = await fetch('https://music.youtube.com/youtubei/v1/next', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Origin: 'https://music.youtube.com',
      Referer: 'https://music.youtube.com/',
      'User-Agent':
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
      'X-YouTube-Client-Name': '67',
      'X-YouTube-Client-Version': '1.20240901.01.00',
    },
    body: JSON.stringify(payload),
  })

  if (!res.ok) {
    throw new Error(`InnerTube next returned HTTP ${res.status}`)
  }

  const data = (await res.json()) as Record<string, unknown>
  const tracks: Track[] = []

  const rawContents = extractPlaylistPanelContents(data)

  if (Array.isArray(rawContents)) {
    for (const item of rawContents as InnerTubeItem[]) {
      const renderer = item.playlistPanelVideoRenderer
      if (!renderer?.videoId) continue

      const trackVideoId = renderer.videoId
      const title = renderer.title?.runs?.[0]?.text || 'Unknown Track'

      let artistName = 'Unknown Artist'
      let artistId = `artist-${trackVideoId}`
      let albumTitle = 'Single'
      const albumId = `album-${trackVideoId}`

      const bylineRuns = renderer.longBylineText?.runs || renderer.shortBylineText?.runs
      if (Array.isArray(bylineRuns) && bylineRuns.length > 0) {
        const firstRun = bylineRuns[0]
        if (firstRun?.text) {
          artistName = firstRun.text
          const browseId = firstRun.navigationEndpoint?.browseEndpoint?.browseId
          if (browseId) artistId = browseId
        }
        if (bylineRuns.length >= 3 && bylineRuns[2]?.text) {
          albumTitle = bylineRuns[2].text
        }
      }

      const thumbnails = renderer.thumbnail?.thumbnails
      const artwork =
        thumbnails && thumbnails.length > 0 ? thumbnails[thumbnails.length - 1]?.url || '' : ''

      const lengthStr = renderer.lengthText?.runs?.[0]?.text
      const durationMs = parseTimeStringToMs(lengthStr)

      tracks.push({
        id: trackVideoId,
        title,
        artistId,
        artistName,
        albumId,
        albumTitle,
        albumImage: artwork,
        durationMs,
        artwork,
        explicit: false,
        playCount: 0,
        videoId: trackVideoId,
      })
    }
  }

  const nextContinuationToken = extractNextContinuationToken(data)

  const result: { tracks: Track[]; continuationToken?: string } = {
    tracks,
  }
  if (nextContinuationToken) {
    result.continuationToken = nextContinuationToken
  }

  return result
}

function extractPlaylistPanelContents(data: Record<string, unknown>): unknown[] | null {
  const contContents = data['continuationContents'] as Record<string, unknown> | undefined
  if (contContents) {
    const playlistCont = contContents['playlistPanelContinuation'] as
      Record<string, unknown> | undefined
    if (playlistCont?.['contents']) {
      return playlistCont['contents'] as unknown[]
    }
  }

  const contents = data['contents'] as Record<string, unknown> | undefined
  const singleCol = contents?.['singleColumnMusicWatchNextResultsRenderer'] as
    Record<string, unknown> | undefined
  const tabbed = singleCol?.['tabbedRenderer'] as Record<string, unknown> | undefined
  const watchNext = tabbed?.['watchNextTabbedResultsRenderer'] as
    Record<string, unknown> | undefined
  const tabs = watchNext?.['tabs'] as Array<Record<string, unknown>> | undefined
  const firstTab = tabs?.[0]?.['tabRenderer'] as Record<string, unknown> | undefined
  const tabContent = firstTab?.['content'] as Record<string, unknown> | undefined
  const queue = tabContent?.['musicQueueRenderer'] as Record<string, unknown> | undefined
  const queueContent = queue?.['content'] as Record<string, unknown> | undefined
  const playlistPanel = queueContent?.['playlistPanelRenderer'] as
    Record<string, unknown> | undefined

  if (playlistPanel?.['contents']) {
    return playlistPanel['contents'] as unknown[]
  }

  return null
}

function extractNextContinuationToken(data: Record<string, unknown>): string | undefined {
  const contContents = data['continuationContents'] as Record<string, unknown> | undefined
  const playlistCont = contContents?.['playlistPanelContinuation'] as
    Record<string, unknown> | undefined
  const continuations = playlistCont?.['continuations'] as
    Array<Record<string, unknown>> | undefined
  const nextContData = continuations?.[0]?.['nextRadioContinuationData'] as
    Record<string, unknown> | undefined
  if (typeof nextContData?.['continuation'] === 'string') {
    return nextContData['continuation']
  }
  return undefined
}

function parseTimeStringToMs(timeStr?: string): number {
  if (!timeStr) return 180000
  const parts = timeStr.split(':').map((p) => Number.parseInt(p, 10))
  if (parts.length === 2) {
    const mins = parts[0] ?? 0
    const secs = parts[1] ?? 0
    return (mins * 60 + secs) * 1000
  }
  if (parts.length === 3) {
    const hrs = parts[0] ?? 0
    const mins = parts[1] ?? 0
    const secs = parts[2] ?? 0
    return (hrs * 3600 + mins * 60 + secs) * 1000
  }
  return 180000
}
