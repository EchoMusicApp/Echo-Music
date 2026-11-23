import test from 'node:test'
import assert from 'node:assert'
import {
  getCachedMetadata,
  setCachedMetadata,
  fetchWithCache,
  invalidateMetadata,
} from '../src/services/cache/metadataCache.ts'

test('MetadataCache: setCachedMetadata stores in memory cache and getCachedMetadata retrieves it', async () => {
  const key = 'test:artist:1'
  const mockArtist = { id: 'art-1', name: 'Radiohead' }

  await setCachedMetadata(key, 'artist', mockArtist, 60_000)

  const cached = await getCachedMetadata<typeof mockArtist>(key)
  assert.ok(cached !== null)
  assert.strictEqual(cached.data.id, 'art-1')
  assert.strictEqual(cached.data.name, 'Radiohead')
  assert.strictEqual(cached.isStale, false)
})

test('MetadataCache: expired TTL marks record as stale', async () => {
  const key = 'test:album:stale'
  const mockAlbum = { id: 'alb-1', title: 'OK Computer' }

  // Set with -100ms TTL so it expires immediately
  await setCachedMetadata(key, 'album', mockAlbum, -100)

  const cached = await getCachedMetadata<typeof mockAlbum>(key)
  assert.ok(cached !== null)
  assert.strictEqual(cached.data.id, 'alb-1')
  assert.strictEqual(cached.isStale, true, 'Record should be flagged stale past its TTL')
})

test('MetadataCache: in-flight request deduplication collapses concurrent identical fetches', async () => {
  const key = 'test:dedup:concurrent'
  let invocationCount = 0

  const slowFetcher = async () => {
    invocationCount++
    await new Promise((resolve) => setTimeout(resolve, 50))
    return { payload: 'resolved_data', count: invocationCount }
  }

  // Fire 4 identical calls concurrently
  const [r1, r2, r3, r4] = await Promise.all([
    fetchWithCache(key, 'shelf', slowFetcher, 60_000),
    fetchWithCache(key, 'shelf', slowFetcher, 60_000),
    fetchWithCache(key, 'shelf', slowFetcher, 60_000),
    fetchWithCache(key, 'shelf', slowFetcher, 60_000),
  ])

  assert.strictEqual(invocationCount, 1, 'Underlying fetcher should only be called once for concurrent calls')
  assert.strictEqual(r1.payload, 'resolved_data')
  assert.strictEqual(r2.payload, 'resolved_data')
  assert.strictEqual(r3.payload, 'resolved_data')
  assert.strictEqual(r4.payload, 'resolved_data')
})

test('MetadataCache: invalidateMetadata clears cached records', async () => {
  const key = 'test:invalidate:1'
  await setCachedMetadata(key, 'playlist', { id: 'pl-1' }, 60_000)

  let cached = await getCachedMetadata(key)
  assert.ok(cached !== null)

  await invalidateMetadata(key)

  cached = await getCachedMetadata(key)
  assert.strictEqual(cached, null, 'Cache record should be null after invalidation')
})
