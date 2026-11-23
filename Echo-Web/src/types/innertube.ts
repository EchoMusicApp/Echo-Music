export interface StreamFormat {
  itag: number
  mimeType: string
  bitrate: number
  averageBitrate?: number | undefined
  contentLength?: string | undefined
  quality: string
  qualityLabel?: string | undefined
  audioQuality?: string | undefined
  approxDurationMs?: string | undefined
  audioSampleRate?: string | undefined
  audioChannels?: number | undefined
  loudnessDb?: number | undefined
  url?: string | undefined
  cipher?: string | undefined
  signatureCipher?: string | undefined
}

export interface InnerTubePlayerResponse {
  playabilityStatus: {
    status: string
    reason?: string
    playableInEmbed?: boolean
  }
  videoDetails: {
    videoId: string
    title: string
    lengthSeconds: string
    channelId: string
    isOwnerViewing?: boolean
    shortDescription?: string
    isCrawlable?: boolean
    thumbnail: {
      thumbnails: Array<{
        url: string
        width: number
        height: number
      }>
    }
    author: string
    viewCount?: string
  }
  streamingData?: {
    expiresInSeconds: string
    formats: StreamFormat[]
    adaptiveFormats: StreamFormat[]
  }
  storyboards?: unknown
  microformat?: unknown
}

export interface StreamResolutionResult {
  videoId: string
  title: string
  artist: string
  durationMs: number
  streamUrl: string
  format: StreamFormat
  allAudioFormats: StreamFormat[]
  expiresInSeconds: number
  source: 'opus' | 'aac' | 'fallback'
}

export interface RadioContinuationResult {
  tracks: Array<{
    id: string
    title: string
    artistId: string
    artistName: string
    albumId: string
    albumTitle: string
    albumImage: string
    durationMs: number
    artwork: string
    explicit: boolean
    playCount: number
    videoId: string
  }>
  continuationToken?: string
}
