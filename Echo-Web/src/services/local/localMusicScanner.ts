import { db, type LocalFileRecord } from '../db/echoDatabase'
import type { Track } from '../../types/domain'

export interface ParsedAudioMeta {
  title: string
  artist: string
  album: string
  year?: string | undefined
  genre?: string | undefined
  artworkBlob?: Blob | undefined
}

/**
 * Lightweight pure TypeScript ID3v2 / ID3v1 parser for browser environments
 */
export async function parseAudioMetadata(file: File): Promise<ParsedAudioMeta> {
  const fallbackTitle = file.name.replace(/\.[^/.]+$/, '').replace(/_/g, ' ')
  const defaultMeta: ParsedAudioMeta = {
    title: fallbackTitle,
    artist: 'Local Artist',
    album: 'Local Music',
  }

  try {
    // Read the first 128KB which contains standard ID3v2 headers and APIC frames
    const headerBuffer = await file.slice(0, 131072).arrayBuffer()
    const view = new DataView(headerBuffer)

    // Check for ID3v2 header: "ID3"
    if (view.getUint8(0) === 0x49 && view.getUint8(1) === 0x44 && view.getUint8(2) === 0x33) {
      const version = view.getUint8(3)
      // Synchsafe integer tag size
      const tagSize =
        ((view.getUint8(6) & 0x7f) << 21) |
        ((view.getUint8(7) & 0x7f) << 14) |
        ((view.getUint8(8) & 0x7f) << 7) |
        (view.getUint8(9) & 0x7f)

      let offset = 10
      const maxOffset = Math.min(tagSize + 10, headerBuffer.byteLength - 10)

      while (offset < maxOffset) {
        // Read 4-character Frame ID
        let frameId = ''
        for (let i = 0; i < 4; i++) {
          const charCode = view.getUint8(offset + i)
          if (charCode >= 32 && charCode <= 126) {
            frameId += String.fromCharCode(charCode)
          }
        }

        if (frameId.length < 4 || /[^A-Z0-9]/.test(frameId)) {
          break
        }

        let frameSize = 0
        if (version === 4) {
          // ID3v2.4 synchsafe
          frameSize =
            ((view.getUint8(offset + 4) & 0x7f) << 21) |
            ((view.getUint8(offset + 5) & 0x7f) << 14) |
            ((view.getUint8(offset + 6) & 0x7f) << 7) |
            (view.getUint8(offset + 7) & 0x7f)
        } else {
          // ID3v2.3 standard int
          frameSize = view.getUint32(offset + 4)
        }

        if (frameSize <= 0 || offset + 10 + frameSize > headerBuffer.byteLength) {
          break
        }

        const frameDataOffset = offset + 10
        if (frameId === 'TIT2') {
          defaultMeta.title = decodeTextFrame(view, frameDataOffset, frameSize) || defaultMeta.title
        } else if (frameId === 'TPE1') {
          defaultMeta.artist =
            decodeTextFrame(view, frameDataOffset, frameSize) || defaultMeta.artist
        } else if (frameId === 'TALB') {
          defaultMeta.album = decodeTextFrame(view, frameDataOffset, frameSize) || defaultMeta.album
        } else if (frameId === 'TYER' || frameId === 'TDRC') {
          defaultMeta.year = decodeTextFrame(view, frameDataOffset, frameSize)
        } else if (frameId === 'TCON') {
          defaultMeta.genre = decodeTextFrame(view, frameDataOffset, frameSize)
        } else if (frameId === 'APIC') {
          const picBlob = extractApicPicture(view, frameDataOffset, frameSize)
          if (picBlob) {
            defaultMeta.artworkBlob = picBlob
          }
        }

        offset += 10 + frameSize
      }
    }
  } catch {
    // Graceful fallback to defaultMeta
  }

  return defaultMeta
}

function decodeTextFrame(view: DataView, offset: number, size: number): string {
  if (size <= 1) return ''
  const encoding = view.getUint8(offset)
  const bytes = new Uint8Array(view.buffer, view.byteOffset + offset + 1, size - 1)

  try {
    if (encoding === 0) {
      return new TextDecoder('latin1').decode(bytes).replace(/\0/g, '').trim()
    } else if (encoding === 1 || encoding === 2) {
      return new TextDecoder('utf-16').decode(bytes).replace(/\0/g, '').trim()
    } else {
      return new TextDecoder('utf-8').decode(bytes).replace(/\0/g, '').trim()
    }
  } catch {
    return ''
  }
}

function extractApicPicture(view: DataView, offset: number, size: number): Blob | null {
  try {
    let p = offset + 1
    let mimeType = ''
    while (p < offset + size) {
      const b = view.getUint8(p++)
      if (b === 0) break
      mimeType += String.fromCharCode(b)
    }

    if (!mimeType) mimeType = 'image/jpeg'
    p++

    while (p < offset + size) {
      const b = view.getUint8(p++)
      if (b === 0) break
    }

    const imgArrayBuffer = view.buffer.slice(view.byteOffset + p, view.byteOffset + offset + size)
    if (imgArrayBuffer.byteLength > 0) {
      return new Blob([imgArrayBuffer as ArrayBuffer], { type: mimeType })
    }
  } catch {
    // Ignore picture extraction failure
  }
  return null
}

export function getAudioDuration(file: File): Promise<number> {
  return new Promise((resolve) => {
    const url = URL.createObjectURL(file)
    const audio = new Audio()
    audio.preload = 'metadata'

    const cleanup = () => {
      URL.revokeObjectURL(url)
      audio.removeEventListener('loadedmetadata', onLoaded)
      audio.removeEventListener('error', onError)
    }

    const onLoaded = () => {
      const dur = Math.round(audio.duration) || 0
      cleanup()
      resolve(dur)
    }

    const onError = () => {
      cleanup()
      resolve(0)
    }

    audio.addEventListener('loadedmetadata', onLoaded)
    audio.addEventListener('error', onError)
    audio.src = url
  })
}

export async function importLocalFile(file: File): Promise<LocalFileRecord> {
  const meta = await parseAudioMetadata(file)
  const durationSeconds = await getAudioDuration(file)
  const id = `local_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`

  const record: LocalFileRecord = {
    id,
    fileName: file.name,
    title: meta.title,
    artist: meta.artist,
    album: meta.album,
    durationSeconds,
    year: meta.year,
    genre: meta.genre,
    artworkBlob: meta.artworkBlob,
    audioBlob: file,
    mimeType: file.type || 'audio/mpeg',
    addedAt: Date.now(),
    fileSize: file.size,
  }

  await db.localFiles.put(record)
  return record
}

export async function importLocalFiles(
  files: FileList | File[],
  onProgress?: (current: number, total: number, fileName: string) => void,
): Promise<LocalFileRecord[]> {
  const validFiles = Array.from(files).filter((f) =>
    /\.(mp3|flac|wav|ogg|m4a|aac|webm)$/i.test(f.name),
  )

  const records: LocalFileRecord[] = []
  for (let i = 0; i < validFiles.length; i++) {
    const file = validFiles[i]!
    onProgress?.(i + 1, validFiles.length, file.name)
    const rec = await importLocalFile(file)
    records.push(rec)
  }

  return records
}

export function localRecordToTrack(record: LocalFileRecord): Track {
  const artworkUrl = record.artworkBlob ? URL.createObjectURL(record.artworkBlob) : ''

  return {
    id: record.id,
    title: record.title,
    artistName: record.artist,
    artistId: `artist_local_${encodeURIComponent(record.artist)}`,
    albumId: `album_local_${encodeURIComponent(record.album)}`,
    albumTitle: record.album,
    albumImage: artworkUrl,
    artwork: artworkUrl,
    thumbnailUrl: artworkUrl,
    durationMs: record.durationSeconds * 1000,
    durationSeconds: record.durationSeconds,
    explicit: false,
    playCount: 0,
    videoId: record.id,
  }
}

export async function getAllLocalTracks(): Promise<Track[]> {
  const records = await db.localFiles.orderBy('addedAt').reverse().toArray()
  return records.map(localRecordToTrack)
}
