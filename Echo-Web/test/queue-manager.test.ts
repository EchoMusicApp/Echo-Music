import test from 'node:test'
import assert from 'node:assert'
import { QueueManager } from '../src/engine/QueueManager.ts'
import type { Track } from '../src/types/audio.ts'

function createMockTrack(id: string, title: string): Track {
  return {
    id,
    title,
    artistName: 'Artist ' + id,
    artistId: 'art-' + id,
    albumTitle: 'Album ' + id,
    albumId: 'alb-' + id,
    albumImage: 'https://example.com/art.jpg',
    artwork: 'https://example.com/art.jpg',
    durationMs: 180000,
    durationSeconds: 180,
    playCount: 0,
  }
}

test('QueueManager: initial state is empty', () => {
  const qm = new QueueManager()
  const detailed = qm.getDetailedQueue()

  assert.strictEqual(detailed.currentTrack, null)
  assert.strictEqual(detailed.upNext.length, 0)
  assert.strictEqual(detailed.sourceUpcoming.length, 0)
  assert.strictEqual(detailed.history.length, 0)
  assert.strictEqual(detailed.totalUpcomingCount, 0)
})

test('QueueManager: setSourceQueue initializes active track and upcoming list', () => {
  const qm = new QueueManager()
  const tracks = [createMockTrack('1', 'Track 1'), createMockTrack('2', 'Track 2'), createMockTrack('3', 'Track 3')]

  const active = qm.setSourceQueue(tracks, 0)
  assert.strictEqual(active?.id, '1')

  const detailed = qm.getDetailedQueue()
  assert.strictEqual(detailed.currentTrack?.id, '1')
  assert.strictEqual(detailed.sourceUpcoming.length, 2)
  assert.strictEqual(detailed.sourceUpcoming[0]?.id, '2')
  assert.strictEqual(detailed.sourceUpcoming[1]?.id, '3')
  assert.strictEqual(detailed.totalUpcomingCount, 2)
})

test('QueueManager: setSourceQueue with startIndex sets active track and upcoming source slice', () => {
  const qm = new QueueManager()
  const tracks = [createMockTrack('1', 'Track 1'), createMockTrack('2', 'Track 2'), createMockTrack('3', 'Track 3')]

  const active = qm.setSourceQueue(tracks, 1)
  assert.strictEqual(active?.id, '2')

  const detailed = qm.getDetailedQueue()
  assert.strictEqual(detailed.currentTrack?.id, '2')
  assert.strictEqual(detailed.sourceUpcoming.length, 1)
  assert.strictEqual(detailed.sourceUpcoming[0]?.id, '3')
})

test('QueueManager: Up Next takes precedence over source queue', () => {
  const qm = new QueueManager()
  const albumTracks = [createMockTrack('1', 'Album 1'), createMockTrack('2', 'Album 2')]
  qm.setSourceQueue(albumTracks, 0)

  // User queues a track
  qm.addToQueue(createMockTrack('u1', 'User Queue 1'))
  qm.playNext(createMockTrack('p1', 'Play Next 1'))

  const detailed = qm.getDetailedQueue()
  assert.strictEqual(detailed.upNext.length, 2)
  assert.strictEqual(detailed.upNext[0]?.id, 'p1')
  assert.strictEqual(detailed.upNext[1]?.id, 'u1')

  // Next track should be Play Next 1
  const t1 = qm.next()
  assert.strictEqual(t1.track?.id, 'p1')

  // Next track should be User Queue 1
  const t2 = qm.next()
  assert.strictEqual(t2.track?.id, 'u1')

  // Next track should be Album 2 from source queue
  const t3 = qm.next()
  assert.strictEqual(t3.track?.id, '2')
})

test('QueueManager: previous() does NOT inject source tracks into upNext', () => {
  const qm = new QueueManager()
  const tracks = [createMockTrack('1', 'Track 1'), createMockTrack('2', 'Track 2'), createMockTrack('3', 'Track 3')]
  qm.setSourceQueue(tracks, 0)

  // Advance to track 2
  qm.next()
  assert.strictEqual(qm.currentTrack?.id, '2')

  // Advance to track 3
  qm.next()
  assert.strictEqual(qm.currentTrack?.id, '3')

  // Step back to track 2 with playback time < 3s
  const prev1 = qm.previous(1.0)
  assert.strictEqual(prev1.track?.id, '2')

  // Verify upNext is still empty! (Old bug was prepending track 3 into upNext)
  const detailed = qm.getDetailedQueue()
  assert.strictEqual(detailed.upNext.length, 0, 'upNext should remain empty after previous()')
  assert.strictEqual(detailed.sourceUpcoming.length, 1)
  assert.strictEqual(detailed.sourceUpcoming[0]?.id, '3')
})

test('QueueManager: reordering and removing from Up Next', () => {
  const qm = new QueueManager()
  qm.playTrackDirectly(createMockTrack('0', 'Active'))
  qm.addToQueue(createMockTrack('u1', 'U1'))
  qm.addToQueue(createMockTrack('u2', 'U2'))
  qm.addToQueue(createMockTrack('u3', 'U3'))

  // Reorder index 0 (u1) to index 2
  qm.moveUpNext(0, 2)
  let detailed = qm.getDetailedQueue()
  assert.strictEqual(detailed.upNext[0]?.id, 'u2')
  assert.strictEqual(detailed.upNext[1]?.id, 'u3')
  assert.strictEqual(detailed.upNext[2]?.id, 'u1')

  // Remove u3 (index 1)
  qm.removeUpNext(1)
  detailed = qm.getDetailedQueue()
  assert.strictEqual(detailed.upNext.length, 2)
  assert.strictEqual(detailed.upNext[0]?.id, 'u2')
  assert.strictEqual(detailed.upNext[1]?.id, 'u1')
})

test('QueueManager: reordering and removing from Source queue', () => {
  const qm = new QueueManager()
  const tracks = [
    createMockTrack('1', 'T1'),
    createMockTrack('2', 'T2'),
    createMockTrack('3', 'T3'),
    createMockTrack('4', 'T4'),
  ]
  qm.setSourceQueue(tracks, 0)

  // Move index 0 ('2') to index 2 ('4')
  qm.moveSourceQueueItem(0, 2)
  let detailed = qm.getDetailedQueue()
  assert.strictEqual(detailed.sourceUpcoming[0]?.id, '3')
  assert.strictEqual(detailed.sourceUpcoming[1]?.id, '4')
  assert.strictEqual(detailed.sourceUpcoming[2]?.id, '2')

  // Remove item at index 1 ('4')
  qm.removeSourceQueueItem(1)
  detailed = qm.getDetailedQueue()
  assert.strictEqual(detailed.sourceUpcoming.length, 2)
  assert.strictEqual(detailed.sourceUpcoming[0]?.id, '3')
  assert.strictEqual(detailed.sourceUpcoming[1]?.id, '2')
})

test('QueueManager: RepeatMode behaviors', () => {
  const qm = new QueueManager()
  const tracks = [createMockTrack('1', 'T1'), createMockTrack('2', 'T2')]
  qm.setSourceQueue(tracks, 0)

  // Repeat ONE
  qm.setRepeatMode('ONE')
  const nextTrack = qm.next()
  assert.strictEqual(nextTrack.track?.id, '1', 'Repeat ONE returns current track')

  // Repeat OFF
  qm.setRepeatMode('OFF')
  const next2 = qm.next()
  assert.strictEqual(next2.track?.id, '2')
  const next3 = qm.next()
  assert.strictEqual(next3.track, null, 'Repeat OFF returns null track at end of queue')

  // Repeat ALL
  qm.setSourceQueue(tracks, 0)
  qm.setRepeatMode('ALL')
  assert.strictEqual(qm.next().track?.id, '2')
  const looped = qm.next()
  assert.strictEqual(looped.track?.id, '1', 'Repeat ALL loops back to start of queue')
})

test('QueueManager: Shuffle toggling preserves current track and restores original order', () => {
  const qm = new QueueManager()
  const tracks = [
    createMockTrack('1', 'T1'),
    createMockTrack('2', 'T2'),
    createMockTrack('3', 'T3'),
    createMockTrack('4', 'T4'),
    createMockTrack('5', 'T5'),
  ]
  qm.setSourceQueue(tracks, 0)

  qm.setShuffle(true)
  assert.strictEqual(qm.currentTrack?.id, '1', 'Current track remains active when shuffling')
  assert.strictEqual(qm.getDetailedQueue().sourceUpcoming.length, 4)

  qm.setShuffle(false)
  const restored = qm.getDetailedQueue().sourceUpcoming
  assert.deepStrictEqual(restored.map((t) => t.id), ['2', '3', '4', '5'], 'Unshuffle restores deterministic album order')
})

test('QueueManager: Clear operations', () => {
  const qm = new QueueManager()
  const tracks = [createMockTrack('1', 'T1'), createMockTrack('2', 'T2')]
  qm.setSourceQueue(tracks, 0)
  qm.addToQueue(createMockTrack('u1', 'U1'))

  assert.strictEqual(qm.getDetailedQueue().totalUpcomingCount, 2)

  // Clear Up Next only
  qm.clearUpNext()
  assert.strictEqual(qm.getDetailedQueue().upNext.length, 0)
  assert.strictEqual(qm.getDetailedQueue().sourceUpcoming.length, 1)

  // Re-add to Up Next and Clear Entire Queue
  qm.addToQueue(createMockTrack('u2', 'U2'))
  qm.clearEntireQueue()
  assert.strictEqual(qm.getDetailedQueue().upNext.length, 0)
  assert.strictEqual(qm.getDetailedQueue().sourceUpcoming.length, 0)
  assert.strictEqual(qm.currentTrack?.id, '1', 'Current track remains playing after clearing queue')
})

test('QueueManager: snapshot and restore fidelity', () => {
  const qm = new QueueManager()
  const tracks = [createMockTrack('1', 'T1'), createMockTrack('2', 'T2'), createMockTrack('3', 'T3')]
  qm.setSourceQueue(tracks, 1)
  qm.addToQueue(createMockTrack('u1', 'U1'))
  qm.setRepeatMode('ALL')
  qm.setShuffle(true)

  const snapshot = qm.snapshotQueue(15)
  assert.strictEqual(snapshot.currentTrack?.id, '2')
  assert.strictEqual(snapshot.repeatMode, 'ALL')
  assert.strictEqual(snapshot.isShuffle, true)
  assert.strictEqual(snapshot.currentTime, 15)

  const restoredQm = new QueueManager()
  restoredQm.restoreQueue(snapshot)

  assert.strictEqual(restoredQm.currentTrack?.id, '2')
  assert.strictEqual(restoredQm.repeatMode, 'ALL')
  assert.strictEqual(restoredQm.isShuffle, true)
  const detailed = restoredQm.getDetailedQueue()
  assert.strictEqual(detailed.upNext.length, 1)
  assert.strictEqual(detailed.upNext[0]?.id, 'u1')
})
