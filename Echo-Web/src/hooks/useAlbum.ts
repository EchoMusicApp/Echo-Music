import { useQuery } from '@tanstack/react-query'
import { InnerTubeClient } from '../services/innertube/client.ts'
import type { Album, Track } from '../types/domain.ts'

export interface AlbumDetailResult {
  album: Album
  tracks: Track[]
}

export function useAlbum(albumId?: string) {
  const normalizedId = albumId?.trim() || ''

  return useQuery<AlbumDetailResult, Error>({
    queryKey: ['album', normalizedId],
    queryFn: () => InnerTubeClient.fetchAlbum(normalizedId),
    enabled: normalizedId.length > 0,
    staleTime: 30 * 60 * 1000, // 30 minutes
  })
}
