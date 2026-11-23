import test from 'node:test'
import assert from 'node:assert'
import {
  parseDurationToSeconds,
  normalizeThumbnailUrl,
  parseHomeFeed,
  parseAlbumDetails,
  parseSearchResults,
} from '../src/services/innertube/parser.ts'

test('Parser: parseDurationToSeconds parses various timestamp formats correctly', () => {
  assert.strictEqual(parseDurationToSeconds('3:45'), 225)
  assert.strictEqual(parseDurationToSeconds('0:30'), 30)
  assert.strictEqual(parseDurationToSeconds('1:15:30'), 4530)
  assert.strictEqual(parseDurationToSeconds('45'), 45)
  assert.strictEqual(parseDurationToSeconds(''), 180, 'Empty string should fall back to 180s default')
  assert.strictEqual(parseDurationToSeconds(undefined), 180, 'Undefined should fall back to 180s default')
  assert.strictEqual(parseDurationToSeconds('invalid'), 180, 'Invalid string should fall back to 180s default')
})

test('Parser: normalizeThumbnailUrl sanitizes and resizes artwork URLs', () => {
  const url1 = 'https://lh3.googleusercontent.com/test=w120-h120'
  assert.strictEqual(normalizeThumbnailUrl(url1, 300), 'https://lh3.googleusercontent.com/test=w300-h300')

  const url2 = '//lh3.googleusercontent.com/test=s120'
  assert.strictEqual(normalizeThumbnailUrl(url2, 544), 'https://lh3.googleusercontent.com/test=s544')

  const empty = normalizeThumbnailUrl('')
  assert.ok(empty.startsWith('data:image/svg+xml'), 'Empty URLs should return placeholder SVG')
})

test('Parser: parseHomeFeed handles both single-column and two-column payloads without throwing', () => {
  // Empty payload
  assert.deepStrictEqual(parseHomeFeed(null), [])
  assert.deepStrictEqual(parseHomeFeed({}), [])

  // Two column payload mock
  const mockFeed = {
    contents: {
      twoColumnBrowseResultsRenderer: {
        secondaryContents: {
          sectionListRenderer: {
            contents: [
              {
                musicCarouselShelfRenderer: {
                  header: {
                    musicCarouselShelfBasicHeaderRenderer: {
                      title: { runs: [{ text: 'Quick Picks' }] },
                    },
                  },
                  contents: [
                    {
                      musicTwoRowItemRenderer: {
                        navigationEndpoint: {
                          watchEndpoint: { videoId: 'v123' },
                        },
                        title: { runs: [{ text: 'Song One' }] },
                        subtitle: {
                          runs: [
                            {
                              text: 'Artist One',
                              navigationEndpoint: { browseEndpoint: { browseId: 'art-1' } },
                            },
                          ],
                        },
                        thumbnailRenderer: {
                          musicThumbnailRenderer: {
                            thumbnail: {
                              thumbnails: [{ url: 'https://example.com/art.jpg' }],
                            },
                          },
                        },
                      },
                    },
                  ],
                },
              },
            ],
          },
        },
      },
    },
  }

  const shelves = parseHomeFeed(mockFeed)
  assert.strictEqual(shelves.length, 1)
  assert.strictEqual(shelves[0]?.title, 'Quick Picks')
  assert.strictEqual(shelves[0]?.items.length, 1)
  assert.strictEqual(shelves[0]?.items[0]?.id, 'v123')
})

test('Parser: parseAlbumDetails parses album header and tracklist gracefully', () => {
  const mockAlbumData = {
    contents: {
      twoColumnBrowseResultsRenderer: {
        secondaryContents: {
          sectionListRenderer: {
            contents: [
              {
                musicShelfRenderer: {
                  contents: [
                    {
                      musicResponsiveListItemRenderer: {
                        playlistItemData: { videoId: 't1' },
                        flexColumns: [
                          {
                            musicResponsiveListItemFlexColumnRenderer: {
                              text: { runs: [{ text: 'Track 1' }] },
                            },
                          },
                          {
                            musicResponsiveListItemFlexColumnRenderer: {
                              text: {
                                runs: [
                                  { text: 'Artist A', navigationEndpoint: { browseEndpoint: { browseId: 'art-a' } } },
                                ],
                              },
                            },
                          },
                        ],
                        fixedColumns: [
                          {
                            musicResponsiveListItemFixedColumnRenderer: {
                              text: { runs: [{ text: '3:20' }] },
                            },
                          },
                        ],
                      },
                    },
                  ],
                },
              },
            ],
          },
        },
      },
    },
    header: {
      musicDetailHeaderRenderer: {
        title: { runs: [{ text: 'Sample Album' }] },
        subtitle: { runs: [{ text: 'Artist A' }] },
      },
    },
  }

  const { album, tracks } = parseAlbumDetails(mockAlbumData)
  assert.strictEqual(album.title, 'Sample Album')
  assert.strictEqual(album.artistName, 'Artist A')
  assert.strictEqual(tracks.length, 1)
  assert.strictEqual(tracks[0]?.id, 't1')
  assert.strictEqual(tracks[0]?.title, 'Track 1')
  assert.strictEqual(tracks[0]?.durationSeconds, 200)
})

test('Parser: parseSearchResults categorizes songs, albums, and artists', () => {
  const emptyRes = parseSearchResults({})
  assert.deepStrictEqual(emptyRes.tracks, [])
  assert.deepStrictEqual(emptyRes.albums, [])
  assert.deepStrictEqual(emptyRes.artists, [])
  assert.deepStrictEqual(emptyRes.playlists, [])
})
