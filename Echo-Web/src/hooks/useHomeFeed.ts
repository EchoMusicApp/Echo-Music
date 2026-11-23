import { useQuery } from '@tanstack/react-query'
import { InnerTubeClient } from '../services/innertube/client.ts'
import type { HomeShelf, PlaylistSummary } from '../types/domain.ts'

export interface ThemedHomeCategory {
  id: 'bollywood' | 'hollywood' | 'romance' | 'soothing'
  title: string
  subtitle: string
  query: string
}

export const THEMED_HOME_CATEGORIES: ThemedHomeCategory[] = [
  {
    id: 'bollywood',
    title: 'Bollywood Essentials',
    subtitle: 'Timeless Hindi anthems, cinematic classics, and chartbusters',
    query: 'Bollywood Hits Playlists',
  },
  {
    id: 'hollywood',
    title: 'Hollywood & Global Pop',
    subtitle: 'International billboard hits, pop favorites, and global sounds',
    query: 'Hollywood Pop Hits Playlists',
  },
  {
    id: 'romance',
    title: 'Romantic Melodies',
    subtitle: 'Heartfelt love songs, warm acoustics, and timeless ballads',
    query: 'Romantic Love Songs Playlists',
  },
  {
    id: 'soothing',
    title: 'Soothing & Chill',
    subtitle: 'Gentle acoustic sessions, calm rhythms, and peaceful soundscapes',
    query: 'Soothing Acoustic Chill Playlists',
  },
]

export interface ThemedShelf {
  id: ThemedHomeCategory['id']
  title: string
  subtitle: string
  items: PlaylistSummary[]
}

export function useThemedHomeShelves() {
  return useQuery<ThemedShelf[], Error>({
    queryKey: ['themed-home-shelves'],
    queryFn: async () => {
      const results = await Promise.all(
        THEMED_HOME_CATEGORIES.map(async (cat) => {
          const res = await InnerTubeClient.search(cat.query, 'playlists')
          return {
            id: cat.id,
            title: cat.title,
            subtitle: cat.subtitle,
            items: res.playlists.slice(0, 8),
          }
        }),
      )
      return results
    },
    staleTime: 30 * 60 * 1000,
  })
}

export function useHomeFeed() {
  return useQuery<HomeShelf[], Error>({
    queryKey: ['home', 'feed'],
    queryFn: () => InnerTubeClient.fetchHomeFeed(),
    staleTime: 10 * 60 * 1000, // 10 minutes
  })
}

export function useCharts(countryCode?: string) {
  return useQuery<HomeShelf[], Error>({
    queryKey: ['charts', countryCode || 'default'],
    queryFn: () => InnerTubeClient.fetchCharts(countryCode),
    staleTime: 15 * 60 * 1000,
  })
}

export function useMoodsAndGenres() {
  return useQuery<HomeShelf[], Error>({
    queryKey: ['moods-and-genres'],
    queryFn: () => InnerTubeClient.fetchMoodsAndGenres(),
    staleTime: 30 * 60 * 1000,
  })
}

export function useTrending() {
  return useQuery<HomeShelf[], Error>({
    queryKey: ['trending'],
    queryFn: () => InnerTubeClient.fetchTrending(),
    staleTime: 10 * 60 * 1000,
  })
}
