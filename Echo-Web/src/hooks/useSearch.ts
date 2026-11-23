import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  InnerTubeClient,
  type SearchFilterKey,
  type SearchResultCatalog,
} from '../services/innertube/client.ts'

export function useDebounce<T>(value: T, delayMs = 250): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value)

  useEffect(() => {
    const handler = window.setTimeout(() => {
      setDebouncedValue(value)
    }, delayMs)

    return () => {
      window.clearTimeout(handler)
    }
  }, [value, delayMs])

  return debouncedValue
}

export function useSearch(query: string, filter: SearchFilterKey = 'all') {
  const trimmed = query.trim()
  const debounced = useDebounce(trimmed, 250)

  return useQuery<SearchResultCatalog, Error>({
    queryKey: ['search', debounced, filter],
    queryFn: () => InnerTubeClient.search(debounced, filter),
    enabled: debounced.length > 0,
    staleTime: 5 * 60 * 1000,
  })
}

export function useSearchSuggestions(query: string) {
  const trimmed = query.trim()
  const debounced = useDebounce(trimmed, 150)

  return useQuery<string[], Error>({
    queryKey: ['search', 'suggestions', debounced],
    queryFn: () => InnerTubeClient.fetchSuggestions(debounced),
    enabled: debounced.length > 0,
    staleTime: 60 * 1000, // 1 minute
    gcTime: 5 * 60 * 1000, // 5 minutes
  })
}

const RECENT_SEARCHES_KEY = 'echo_recent_searches_v1'

export function getStoredRecentSearches(): string[] {
  try {
    const raw = localStorage.getItem(RECENT_SEARCHES_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed)) {
        return parsed.filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
      }
    }
  } catch {
    // Non-fatal
  }
  return []
}

export function saveRecentSearch(query: string): string[] {
  const trimmed = query.trim()
  if (!trimmed) return getStoredRecentSearches()

  const existing = getStoredRecentSearches().filter((q) => q.toLowerCase() !== trimmed.toLowerCase())
  const updated = [trimmed, ...existing].slice(0, 10)
  try {
    localStorage.setItem(RECENT_SEARCHES_KEY, JSON.stringify(updated))
  } catch {
    // Non-fatal
  }
  return updated
}

export function removeRecentSearch(queryToRemove: string): string[] {
  const existing = getStoredRecentSearches().filter((q) => q.toLowerCase() !== queryToRemove.toLowerCase())
  try {
    localStorage.setItem(RECENT_SEARCHES_KEY, JSON.stringify(existing))
  } catch {
    // Non-fatal
  }
  return existing
}

export function clearAllRecentSearches(): void {
  try {
    localStorage.removeItem(RECENT_SEARCHES_KEY)
  } catch {
    // Non-fatal
  }
}
