import { useQuery } from '@tanstack/react-query'
import { InnerTubeClient } from '../services/innertube/client.ts'
import type { SponsorSegment } from '../types/audio.ts'
import type { StreamResolutionResult } from '../types/innertube.ts'

export interface TrackStreamData {
  streamResult: StreamResolutionResult
  sponsorBlockSegments: SponsorSegment[]
}

export function useTrackStream(videoId?: string) {
  const normalizedId = videoId?.trim() || ''

  return useQuery<TrackStreamData, Error>({
    queryKey: ['stream', normalizedId],
    queryFn: async () => {
      const [streamResult, sponsorBlockSegments] = await Promise.all([
        InnerTubeClient.resolveStream(normalizedId),
        InnerTubeClient.fetchSponsorBlock(normalizedId).catch(() => []),
      ])

      return {
        streamResult,
        sponsorBlockSegments,
      }
    },
    enabled: normalizedId.length > 0 && /^[a-zA-Z0-9_-]{11}$/.test(normalizedId),
    staleTime: 4 * 60 * 60 * 1000, // 4 hours
  })
}
