import { db, type MetadataCacheRecord } from '../db/echoDatabase.ts'

const inFlightRequests = new Map<string, Promise<unknown>>()
const memoryCache = new Map<string, { data: unknown; expiresAt: number; updatedAt: number }>()
const MEMORY_CACHE_MAX = 60
const DB_CACHE_MAX = 500

function setMemoryCache(key: string, data: unknown, ttlMs: number, updatedAt: number): void {
  if (memoryCache.size >= MEMORY_CACHE_MAX) {
    const oldestKey = memoryCache.keys().next().value
    if (oldestKey !== undefined) {
      memoryCache.delete(oldestKey)
    }
  }
  memoryCache.set(key, {
    data,
    expiresAt: Date.now() + ttlMs,
    updatedAt,
  })
}

export async function getCachedMetadata<T>(key: string): Promise<{ data: T; isStale: boolean } | null> {
  // 1. Check in-memory cache
  const mem = memoryCache.get(key)
  if (mem) {
    const isStale = Date.now() > mem.expiresAt
    return { data: mem.data as T, isStale }
  }

  // 2. Check IndexedDB
  try {
    const record = await db.metadataCache.get(key)
    if (record) {
      const isStale = Date.now() > record.expiresAt
      setMemoryCache(key, record.data, Math.max(10000, record.expiresAt - Date.now()), record.updatedAt)
      return { data: record.data as T, isStale }
    }
  } catch {
    // If IndexedDB read fails, proceed without cache
  }

  return null
}

export async function setCachedMetadata<T>(
  key: string,
  kind: MetadataCacheRecord['kind'],
  data: T,
  ttlMs: number,
): Promise<void> {
  const now = Date.now()
  const expiresAt = now + ttlMs

  setMemoryCache(key, data, ttlMs, now)

  try {
    await db.metadataCache.put({
      key,
      kind,
      data,
      updatedAt: now,
      expiresAt,
    })

    // Bounded cleanup
    const count = await db.metadataCache.count()
    if (count > DB_CACHE_MAX) {
      const expired = await db.metadataCache.where('expiresAt').below(now).limit(50).primaryKeys()
      if (expired.length > 0) {
        await db.metadataCache.bulkDelete(expired)
      }
    }
  } catch {
    // Non-fatal
  }
}

export async function invalidateMetadata(keyOrPrefix: string): Promise<void> {
  for (const k of memoryCache.keys()) {
    if (k.startsWith(keyOrPrefix)) {
      memoryCache.delete(k)
    }
  }
  try {
    const matches = await db.metadataCache
      .filter((rec) => rec.key.startsWith(keyOrPrefix))
      .primaryKeys()
    if (matches.length > 0) {
      await db.metadataCache.bulkDelete(matches)
    }
  } catch {
    // Non-fatal
  }
}

export async function clearAllMetadataCache(): Promise<void> {
  memoryCache.clear()
  try {
    await db.metadataCache.clear()
  } catch {
    // Non-fatal
  }
}

/**
 * Fetch with intelligent caching:
 * - Deduplicates simultaneous requests
 * - Stale-while-revalidate: returns cached content immediately while updating in background
 * - Offline fallback: returns expired cache when network fails
 */
export async function fetchWithCache<T>(
  key: string,
  kind: MetadataCacheRecord['kind'],
  fetcher: () => Promise<T>,
  ttlMs = 15 * 60 * 1000, // 15 mins default
): Promise<T> {
  // 1. Check existing cached entry
  const cached = await getCachedMetadata<T>(key)
  if (cached) {
    if (!cached.isStale) {
      return cached.data
    }

    // Stale: trigger background revalidation without blocking caller
    void executeFetch(key, kind, fetcher, ttlMs).catch(() => {})
    return cached.data
  }

  // 2. Request deduplication for in-flight requests
  const existing = inFlightRequests.get(key)
  if (existing) {
    return existing as Promise<T>
  }

  const promise = executeFetch(key, kind, fetcher, ttlMs)
    .catch(async (err) => {
      // Fallback: If fetch fails, check if we have any stale cached data
      const staleFallback = await getCachedMetadata<T>(key)
      if (staleFallback) {
        return staleFallback.data
      }
      throw err
    })
    .finally(() => {
      inFlightRequests.delete(key)
    })

  inFlightRequests.set(key, promise)
  return promise
}

async function executeFetch<T>(
  key: string,
  kind: MetadataCacheRecord['kind'],
  fetcher: () => Promise<T>,
  ttlMs: number,
): Promise<T> {
  const freshData = await fetcher()
  if (freshData !== undefined && freshData !== null) {
    await setCachedMetadata(key, kind, freshData, ttlMs)
  }
  return freshData
}
