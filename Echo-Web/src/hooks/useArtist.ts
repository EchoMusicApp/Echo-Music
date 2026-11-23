import { useQuery } from '@tanstack/react-query'
import { InnerTubeClient } from '../services/innertube/client.ts'
import type { Album, Artist, Track } from '../types/domain.ts'

export interface ArtistDetailResult {
  artist: Artist
  topTracks: Track[]
  albums: Album[]
  singles: Album[]
  relatedArtists: Artist[]
}

export function useArtist(artistId?: string) {
  const normalizedId = artistId?.trim() || ''

  return useQuery<ArtistDetailResult, Error>({
    queryKey: ['artist', normalizedId],
    queryFn: () => InnerTubeClient.fetchArtist(normalizedId),
    enabled: normalizedId.length > 0,
    staleTime: 30 * 60 * 1000, // 30 minutes
  })
}
