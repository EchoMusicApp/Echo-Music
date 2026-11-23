import type { SponsorSegment } from '../types/audio'

export interface SponsorBlockOptions {
  enabled?: boolean
  categories?: string[]
  pollIntervalMs?: number
  apiBaseUrl?: string
}

export class SponsorBlock {
  private isEnabled = true
  private categories: string[] = ['music_offtopic', 'intro', 'outro', 'preview']
  private pollIntervalMs = 250
  private apiBaseUrl = '/api/sponsorblock'

  private pollTimer: number | null = null
  private currentSegments: SponsorSegment[] = []
  private currentVideoId: string | null = null
  private segmentCache = new Map<string, SponsorSegment[]>()
  private skippedSegmentIds = new Set<string>()

  private getCurrentTime: (() => number) | null = null
  private onSeek: ((timeSeconds: number) => void) | null = null
  public onSegmentSkipped?: (segment: SponsorSegment) => void

  constructor(options: SponsorBlockOptions = {}) {
    this.isEnabled = options.enabled ?? true
    if (options.categories) {
      this.categories = options.categories
    }
    if (options.pollIntervalMs) {
      this.pollIntervalMs = options.pollIntervalMs
    }
    if (options.apiBaseUrl) {
      this.apiBaseUrl = options.apiBaseUrl
    }
  }

  public setEnabled(enabled: boolean): void {
    this.isEnabled = enabled
    if (!enabled && this.pollTimer !== null) {
      this.stopPolling()
    } else if (enabled && this.pollTimer === null && this.currentVideoId) {
      this.startPolling()
    }
  }

  public getEnabled(): boolean {
    return this.isEnabled
  }

  public setCategories(categories: string[]): void {
    this.categories = categories
  }

  public attachPlayer(getCurrentTime: () => number, onSeek: (timeSeconds: number) => void): void {
    this.getCurrentTime = getCurrentTime
    this.onSeek = onSeek
  }

  public async loadSegmentsForVideo(videoId: string): Promise<SponsorSegment[]> {
    this.stopPolling()
    this.currentVideoId = videoId
    this.currentSegments = []
    this.skippedSegmentIds.clear()

    if (!videoId) return []

    const cached = this.segmentCache.get(videoId)
    if (cached) {
      if (this.currentVideoId !== videoId) return []
      this.currentSegments = cached
      if (this.isEnabled) this.startPolling()
      return cached
    }

    try {
      const url = this.apiBaseUrl.includes('/api/sponsorblock')
        ? `${this.apiBaseUrl}/${encodeURIComponent(videoId)}`
        : `${this.apiBaseUrl}/skipSegments?videoID=${encodeURIComponent(videoId)}&categories=${encodeURIComponent(JSON.stringify(this.categories))}`

      const response = await fetch(url)
      if (this.currentVideoId !== videoId) {
        return []
      }

      if (!response.ok) {
        if (response.status === 404) {
          this.segmentCache.set(videoId, [])
          return []
        }
        return []
      }

      const data = (await response.json()) as Array<{
        category: string
        segment?: [number, number]
        start?: number
        end?: number
        UUID?: string
        uuid?: string
      }>

      if (this.currentVideoId !== videoId) {
        return []
      }

      const parsed: SponsorSegment[] = data
        .map((item) => {
          if (Array.isArray(item.segment) && item.segment.length === 2) {
            const seg: SponsorSegment = {
              category: item.category,
              start: item.segment[0],
              end: item.segment[1],
            }
            const uuid = item.UUID || item.uuid
            if (uuid) {
              seg.uuid = uuid
            }
            return seg
          }
          if (typeof item.start === 'number' && typeof item.end === 'number') {
            const seg: SponsorSegment = {
              category: item.category,
              start: item.start,
              end: item.end,
            }
            const uuid = item.UUID || item.uuid
            if (uuid) {
              seg.uuid = uuid
            }
            return seg
          }
          return null
        })
        .filter((item): item is SponsorSegment => item !== null && item.end > item.start)
        .sort((a, b) => a.start - b.start)

      this.segmentCache.set(videoId, parsed)
      this.currentSegments = parsed

      if (this.isEnabled) {
        this.startPolling()
      }

      return parsed
    } catch {
      return []
    }
  }

  public setManualSegments(segments: SponsorSegment[]): void {
    this.stopPolling()
    this.skippedSegmentIds.clear()
    this.currentSegments = [...segments]
      .filter((s) => s.end > s.start)
      .sort((a, b) => a.start - b.start)
    if (this.isEnabled) {
      this.startPolling()
    }
  }

  public checkSegments(currentTime: number): SponsorSegment | null {
    if (!this.isEnabled || this.currentSegments.length === 0) return null

    for (const seg of this.currentSegments) {
      const segId = seg.uuid || `${seg.start}-${seg.end}`
      if (this.skippedSegmentIds.has(segId)) continue

      if (currentTime >= seg.start && currentTime < seg.end - 0.05) {
        return seg
      }
    }
    return null
  }

  public startPolling(): void {
    if (this.pollTimer !== null) return

    this.pollTimer = window.setInterval(() => {
      if (!this.isEnabled || !this.getCurrentTime || !this.onSeek) return

      const time = this.getCurrentTime()
      const segToSkip = this.checkSegments(time)

      if (segToSkip) {
        const segId = segToSkip.uuid || `${segToSkip.start}-${segToSkip.end}`
        this.skippedSegmentIds.add(segId)
        // Micro-offset of +0.05s guarantees we clear the segment boundary to avoid stutter loops
        const targetTime = Math.max(0, segToSkip.end + 0.05)
        this.onSeek(targetTime)
        this.onSegmentSkipped?.(segToSkip)
      }
    }, this.pollIntervalMs)
  }

  public stopPolling(): void {
    if (this.pollTimer !== null) {
      window.clearInterval(this.pollTimer)
      this.pollTimer = null
    }
  }

  public destroy(): void {
    this.stopPolling()
    this.currentSegments = []
    this.skippedSegmentIds.clear()
    this.segmentCache.clear()
    this.getCurrentTime = null
    this.onSeek = null
  }
}
