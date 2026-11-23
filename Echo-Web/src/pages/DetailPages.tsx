import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  ArrowLeft,
  ArrowRight,
  Check,
  Clock3,
  Heart,
  Pencil,
  Play,
  Plus,
  Shuffle,
  Trash2,
  Share2,
  FileDown,
} from 'lucide-react'
import type { Playlist, Track } from '../types/domain.ts'
import { useAlbum } from '../hooks/useAlbum.ts'
import { useArtist } from '../hooks/useArtist.ts'
import { useAudioEngine } from '../hooks/useAudioEngine.ts'
import { getCachedTrack, useEchoStore } from '../stores/useEchoStore.ts'
import { classNames } from '../utils/classNames.ts'
import { formatCount, formatDurationLong } from '../utils/format.ts'
import { artwork } from '../utils/artwork.ts'
import { MediaCard, SectionHeading, TrackList } from '../components/MusicUI.tsx'
import {
  exportPlaylistToM3U,
  triggerFileDownload,
} from '../services/sharing/playlistPortability.ts'
import { resolveMissingTrack } from '../services/db/echoDatabase.ts'

function getRandomIndex(length: number): number {
  return length > 0 ? Math.floor(Math.random() * length) : 0
}

function PlaylistDetailsDialog({
  playlist,
  open,
  onClose,
}: {
  playlist: Playlist
  open: boolean
  onClose: () => void
}) {
  const [title, setTitle] = useState(playlist.title)
  const [description, setDescription] = useState(playlist.description)
  const [error, setError] = useState('')
  const dialogRef = useRef<HTMLDialogElement>(null)
  const updatePlaylist = useEchoStore((state) => state.updatePlaylist)
  const setPlaylistCover = useEchoStore((state) => state.setPlaylistCover)
  const notify = useEchoStore((state) => state.setToast)

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    const close = () => onClose()
    dialog.addEventListener('close', close)
    return () => dialog.removeEventListener('close', close)
  }, [onClose])

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    try {
      updatePlaylist(playlist.id, title, description)
      setError('')
      onClose()
      notify('Playlist details updated')
    } catch (issue) {
      setError(issue instanceof Error ? issue.message : 'Could not update this playlist.')
    }
  }
  return (
    <dialog className="dialog" ref={dialogRef} aria-labelledby="edit-playlist-title">
      <form className="dialog__form" onSubmit={submit}>
        <h2 id="edit-playlist-title">Edit details</h2>
        <label htmlFor="edit-playlist-name">Name</label>
        <input
          id="edit-playlist-name"
          autoFocus
          maxLength={60}
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          required
        />
        <label htmlFor="edit-playlist-description">Description</label>
        <textarea
          id="edit-playlist-description"
          maxLength={240}
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          rows={4}
        />
        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}
        {playlist.trackIds.length > 0 && (
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-medium text-[#a59fad]">Cover (from track artwork)</label>
            <div className="flex flex-wrap gap-1.5">
              {playlist.trackIds.slice(0, 8).map((trackId) => {
                const art =
                  getCachedTrack(trackId)?.artwork ||
                  `https://i.ytimg.com/vi/${trackId}/hqdefault.jpg`
                return (
                  <button
                    key={trackId}
                    type="button"
                    aria-label={`Set cover from track artwork ${trackId}`}
                    onClick={() => {
                      setPlaylistCover(playlist.id, art)
                      notify('Playlist cover updated')
                    }}
                    className={`h-10 w-10 overflow-hidden rounded-lg border transition-all ${
                      playlist.image === art
                        ? 'border-2 border-[#7c5cff]'
                        : 'border-white/10 hover:border-white/30'
                    }`}
                  >
                    <img
                      src={art}
                      alt=""
                      className="h-full w-full object-cover"
                      referrerPolicy="no-referrer"
                    />
                  </button>
                )
              })}
            </div>
          </div>
        )}
        <div className="dialog__actions">
          <button
            type="button"
            className="button button--quiet"
            onClick={() => {
              setPlaylistCover(playlist.id, '')
              notify('Playlist cover reset — the first track artwork is used again')
            }}
          >
            Remove cover
          </button>
          <button type="button" className="button button--quiet" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="button button--primary">
            Save changes
          </button>
        </div>
      </form>
    </dialog>
  )
}

function DeletePlaylistDialog({
  playlist,
  open,
  onClose,
}: {
  playlist: Playlist
  open: boolean
  onClose: () => void
}) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const deletePlaylist = useEchoStore((state) => state.deletePlaylist)
  const notify = useEchoStore((state) => state.setToast)
  const navigate = useNavigate()
  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])
  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    const close = () => onClose()
    dialog.addEventListener('close', close)
    return () => dialog.removeEventListener('close', close)
  }, [onClose])
  const confirm = () => {
    deletePlaylist(playlist.id)
    onClose()
    notify('Playlist deleted')
    navigate('/library')
  }
  return (
    <dialog
      className="dialog dialog--small"
      ref={dialogRef}
      aria-labelledby="delete-playlist-title"
    >
      <div className="dialog__form">
        <h2 id="delete-playlist-title">Delete “{playlist.title}”?</h2>
        <p className="dialog__copy">
          You cannot undo this. The playlist will leave your library, but its songs will stay in
          your collection.
        </p>
        <div className="dialog__actions">
          <button type="button" className="button button--quiet" onClick={onClose}>
            Keep playlist
          </button>
          <button type="button" className="button button--danger" onClick={confirm}>
            <Trash2 size={15} /> Delete playlist
          </button>
        </div>
      </div>
    </dialog>
  )
}

export function PlaylistPage() {
  const { playlistId = '' } = useParams()
  const userPlaylist = useEchoStore((state) =>
    state.playlists.find((item) => item.id === playlistId),
  )
  const saved = useEchoStore((state) => state.savedPlaylistIds.includes(playlistId))
  const toggleSaved = useEchoStore((state) => state.togglePlaylistLike)
  const removeTrack = useEchoStore((state) => state.removeTrackFromPlaylist)
  const reorderTrack = useEchoStore((state) => state.reorderPlaylistTrack)
  const trackCache = useEchoStore((state) => state.trackCache)
  const cacheTrack = useEchoStore((state) => state.cacheTrack)
  const setToast = useEchoStore((state) => state.setToast)
  const { playQueue } = useAudioEngine()

  const isUserPlaylist = Boolean(userPlaylist)
  const { data: remoteData, isLoading: remoteLoading } = useAlbum(
    !isUserPlaylist && playlistId ? playlistId : undefined,
  )

  const [editOpen, setEditOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)

  // Resolve any custom playlist tracks missing from memory/cache
  useEffect(() => {
    if (!userPlaylist?.trackIds || userPlaylist.trackIds.length === 0) return
    const missing = userPlaylist.trackIds.filter((id) => !trackCache[id] && !getCachedTrack(id))
    if (missing.length === 0) return

    let cancelled = false
    void Promise.all(
      missing.map(async (id) => {
        const resolved = await resolveMissingTrack(id)
        if (resolved && !cancelled) {
          cacheTrack(resolved)
        }
      }),
    )

    return () => {
      cancelled = true
    }
  }, [userPlaylist?.trackIds, trackCache, cacheTrack])

  if (isUserPlaylist && userPlaylist) {
    const playlistTracks = userPlaylist.trackIds
      .map((id) => trackCache[id] || getCachedTrack(id))
      .filter((track): track is Track => Boolean(track))
    const totalDuration = playlistTracks.reduce(
      (sum, track) => sum + (track.durationMs || (track.durationSeconds || 0) * 1000),
      0,
    )

    const start = () => {
      if (playlistTracks.length > 0) playQueue(playlistTracks, 0)
      else setToast('Add a few tracks to play this playlist.')
    }
    const shuffle = () => {
      if (playlistTracks.length > 0) {
        const rand = getRandomIndex(playlistTracks.length)
        playQueue(playlistTracks, rand)
      } else setToast('Add a few tracks to shuffle this playlist.')
    }

    const handleExportM3U = () => {
      const m3u = exportPlaylistToM3U(userPlaylist, playlistTracks)
      triggerFileDownload(
        m3u,
        `${userPlaylist.title.replace(/[^a-zA-Z0-9_-]/g, '_')}.m3u`,
        'audio/x-mpegurl',
      )
      setToast(`Exported "${userPlaylist.title}.m3u"`)
    }

    const handleSharePlaylist = () => {
      navigator.clipboard.writeText(window.location.href)
      setToast('Playlist link copied to clipboard')
    }

    return (
      <div className="page page--detail">
        <div className="detail-hero detail-hero--playlist">
          <img
            className="detail-hero__art"
            src={userPlaylist.image || artwork(userPlaylist.title || 'echo-playlist', 300)}
            alt=""
            referrerPolicy="no-referrer"
            onError={(e) => {
              const fallback = artwork(userPlaylist.title || 'echo-playlist', 300)
              if (e.currentTarget.src !== fallback) e.currentTarget.src = fallback
            }}
          />
          <div className="detail-hero__content">
            <h1>{userPlaylist.title}</h1>
            <p>{userPlaylist.description || 'A collection of songs, put together by you.'}</p>
            <div className="detail-meta">
              <span>Playlist</span>
              <span>{userPlaylist.owner}</span>
              <span>{playlistTracks.length} tracks</span>
              <span>{formatDurationLong(totalDuration)}</span>
            </div>
            <div className="detail-actions">
              <button
                className="play-button play-button--large"
                type="button"
                aria-label="Play playlist"
                onClick={start}
              >
                <Play size={20} fill="currentColor" />
              </button>
              <button className="button button--quiet" type="button" onClick={shuffle}>
                <Shuffle size={16} /> Shuffle
              </button>
              <button
                className={classNames('icon-button', 'detail-like', saved && 'is-liked')}
                type="button"
                aria-label={saved ? 'Remove playlist from library' : 'Save playlist to library'}
                aria-pressed={saved}
                onClick={() => toggleSaved(userPlaylist.id)}
              >
                <Heart size={20} fill={saved ? 'currentColor' : 'none'} />
              </button>
              <button
                className="icon-button"
                type="button"
                aria-label="Export to M3U file"
                title="Export to M3U"
                onClick={handleExportM3U}
              >
                <FileDown size={18} />
              </button>
              <button
                className="icon-button"
                type="button"
                aria-label="Share playlist link"
                title="Share playlist"
                onClick={handleSharePlaylist}
              >
                <Share2 size={18} />
              </button>
              <button
                className="icon-button"
                type="button"
                aria-label="Edit playlist details"
                onClick={() => setEditOpen(true)}
              >
                <Pencil size={17} />
              </button>
              <button
                className="icon-button"
                type="button"
                aria-label="Delete playlist"
                onClick={() => setDeleteOpen(true)}
              >
                <Trash2 size={17} />
              </button>
            </div>
          </div>
        </div>
        <div className="playlist-toolbar">
          <div>
            <button className="text-button" type="button" onClick={() => setEditOpen(true)}>
              Edit details
            </button>
            <span> · </span>
            <button
              className="text-button playlist-delete-link"
              type="button"
              onClick={() => setDeleteOpen(true)}
            >
              Delete playlist
            </button>
          </div>
          <span>
            {playlistTracks.length} songs · {formatDurationLong(totalDuration)}
          </span>
        </div>
        <TrackList
          items={playlistTracks}
          onRemove={(track) => removeTrack(userPlaylist.id, track.id)}
          onMove={(track, direction) => {
            const from = playlistTracks.findIndex((t) => t.id === track.id)
            const to = from + direction
            if (from !== -1 && to >= 0 && to < playlistTracks.length) {
              reorderTrack(userPlaylist.id, from, to)
            }
          }}
        />
        {playlistTracks.length === 0 && (
          <div className="playlist-empty-action">
            <Link className="button button--quiet" to="/discover">
              Find songs to add <ArrowRight size={15} />
            </Link>
            <p>Use a track's more menu to add it to this playlist.</p>
          </div>
        )}
        {editOpen && (
          <PlaylistDetailsDialog
            key={userPlaylist.id}
            playlist={userPlaylist}
            open
            onClose={() => setEditOpen(false)}
          />
        )}
        <DeletePlaylistDialog
          playlist={userPlaylist}
          open={deleteOpen}
          onClose={() => setDeleteOpen(false)}
        />
      </div>
    )
  }

  if (remoteLoading) {
    return (
      <div className="page page--detail">
        <div className="detail-hero detail-hero--playlist">
          <div className="skeleton-box" style={{ width: 200, height: 200, borderRadius: 12 }} />
          <div className="detail-hero__content">
            <div className="skeleton-box" style={{ width: '40%', height: 32, marginBottom: 12 }} />
            <div className="skeleton-box" style={{ width: '60%', height: 18 }} />
          </div>
        </div>
      </div>
    )
  }

  if (remoteData?.album) {
    const playlist = remoteData.album
    const playlistTracks = remoteData.tracks || []
    const totalDuration = playlistTracks.reduce(
      (sum, track) => sum + (track.durationMs || (track.durationSeconds || 0) * 1000),
      0,
    )

    const start = () => {
      if (playlistTracks.length > 0) playQueue(playlistTracks, 0)
      else setToast('No tracks available in this playlist.')
    }
    const shuffle = () => {
      if (playlistTracks.length > 0) {
        const rand = getRandomIndex(playlistTracks.length)
        playQueue(playlistTracks, rand)
      }
    }

    return (
      <div className="page page--detail">
        <div className="detail-hero detail-hero--playlist">
          <img
            className="detail-hero__art"
            src={
              playlist.image ||
              playlist.thumbnailUrl ||
              artwork(playlist.title || 'echo-playlist', 300)
            }
            alt=""
            referrerPolicy="no-referrer"
            onError={(e) => {
              const fallback = artwork(playlist.title || 'echo-playlist', 300)
              if (e.currentTarget.src !== fallback) e.currentTarget.src = fallback
            }}
          />
          <div className="detail-hero__content">
            <h1>{playlist.title}</h1>
            <p>{playlist.artistName || 'Curated Playlist'}</p>
            <div className="detail-meta">
              <span>Playlist</span>
              <span>{playlistTracks.length} tracks</span>
              <span>{formatDurationLong(totalDuration)}</span>
            </div>
            <div className="detail-actions">
              <button
                className="play-button play-button--large"
                type="button"
                aria-label="Play playlist"
                onClick={start}
              >
                <Play size={20} fill="currentColor" />
              </button>
              <button className="button button--quiet" type="button" onClick={shuffle}>
                <Shuffle size={16} /> Shuffle
              </button>
              <button
                className={classNames('icon-button', 'detail-like', saved && 'is-liked')}
                type="button"
                aria-label={saved ? 'Remove playlist from library' : 'Save playlist to library'}
                aria-pressed={saved}
                onClick={() => toggleSaved(playlistId)}
              >
                <Heart size={20} fill={saved ? 'currentColor' : 'none'} />
              </button>
            </div>
          </div>
        </div>
        <div className="playlist-toolbar">
          <span>
            {playlistTracks.length} songs · {formatDurationLong(totalDuration)}
          </span>
        </div>
        <TrackList items={playlistTracks} />
      </div>
    )
  }

  return (
    <NotFound
      title="That playlist wandered off."
      message="It may have been deleted, or it may not be part of this collection."
    />
  )
}

export function AlbumPage() {
  const { albumId = '' } = useParams()
  const { data, isLoading, error } = useAlbum(albumId)
  const liked = useEchoStore((state) => state.likedAlbumIds.includes(albumId))
  const toggleLike = useEchoStore((state) => state.toggleAlbumLike)
  const setToast = useEchoStore((state) => state.setToast)
  const { playQueue } = useAudioEngine()

  if (isLoading) {
    return (
      <div className="page page--detail">
        <div className="detail-hero">
          <div className="skeleton-box" style={{ width: 230, height: 230, borderRadius: 12 }} />
          <div className="detail-hero__content">
            <div className="skeleton-box" style={{ width: '40%', height: 32, marginBottom: 12 }} />
            <div className="skeleton-box" style={{ width: '60%', height: 18 }} />
          </div>
        </div>
      </div>
    )
  }

  const album = data?.album
  const albumTracks = data?.tracks || []

  if (error || !album) {
    return (
      <NotFound
        title="Album not found"
        message={error ? error.message : "This release couldn't be loaded."}
      />
    )
  }

  const start = () => albumTracks.length > 0 && playQueue(albumTracks, 0)
  const shuffle = () => {
    if (albumTracks.length > 0) {
      const rand = getRandomIndex(albumTracks.length)
      playQueue(albumTracks, rand)
    }
  }

  const handleShareAlbum = () => {
    navigator.clipboard.writeText(window.location.href)
    setToast('Album link copied to clipboard')
  }

  const totalDuration = albumTracks.reduce(
    (sum, track) => sum + (track.durationMs || (track.durationSeconds || 0) * 1000),
    0,
  )

  return (
    <div className="page page--detail">
      <div className="detail-hero">
        <img
          className="detail-hero__art"
          src={album.image || album.thumbnailUrl || artwork(album.title || 'echo-album', 300)}
          alt={`${album.title} artwork`}
          referrerPolicy="no-referrer"
          onError={(e) => {
            const fallback = artwork(album.title || 'echo-album', 300)
            if (e.currentTarget.src !== fallback) e.currentTarget.src = fallback
          }}
        />
        <div className="detail-hero__content">
          <h1>{album.title}</h1>
          <div className="detail-meta detail-meta--artist">
            <span>{album.type === 'ep' ? 'EP' : album.type || 'Album'}</span>
            {album.artistId ? (
              <Link to={`/artists/${album.artistId}`}>{album.artistName}</Link>
            ) : (
              <span>{album.artistName}</span>
            )}
            {album.year ? <span>{album.year}</span> : null}
            <span>{albumTracks.length} tracks</span>
            <span>{formatDurationLong(album.durationMs || totalDuration)}</span>
          </div>
          <div className="detail-actions">
            <button
              className="play-button play-button--large"
              type="button"
              aria-label={`Play ${album.title}`}
              onClick={start}
            >
              <Play size={20} fill="currentColor" />
            </button>
            <button className="button button--quiet" type="button" onClick={shuffle}>
              <Shuffle size={16} /> Shuffle
            </button>
            <button
              className="icon-button"
              type="button"
              aria-label="Share album link"
              title="Share album"
              onClick={handleShareAlbum}
            >
              <Share2 size={18} />
            </button>
            <button
              className={classNames('icon-button', 'detail-like', liked && 'is-liked')}
              type="button"
              aria-label={liked ? 'Remove album from library' : 'Save album to library'}
              aria-pressed={liked}
              onClick={() => toggleLike(albumId)}
            >
              <Heart size={20} fill={liked ? 'currentColor' : 'none'} />
            </button>
          </div>
        </div>
      </div>
      <div className="detail-section-title">
        <SectionHeading
          title="The songs"
          action={
            <span className="section-note">
              <Clock3 size={14} /> {formatDurationLong(totalDuration)}
            </span>
          }
        />
      </div>
      <TrackList items={albumTracks} />
    </div>
  )
}

export function ArtistPage() {
  const { artistId = '' } = useParams()
  const { data, isLoading, error } = useArtist(artistId)
  const followed = useEchoStore((state) => state.followedArtistIds.includes(artistId))
  const toggleFollow = useEchoStore((state) => state.toggleArtistFollow)
  const setToast = useEchoStore((state) => state.setToast)
  const { playQueue } = useAudioEngine()

  if (isLoading) {
    return (
      <div className="page page--detail">
        <div className="artist-hero">
          <div className="skeleton-box" style={{ width: 180, height: 180, borderRadius: '50%' }} />
          <div className="artist-hero__copy">
            <div className="skeleton-box" style={{ width: '50%', height: 36, marginBottom: 12 }} />
            <div className="skeleton-box" style={{ width: '30%', height: 18 }} />
          </div>
        </div>
      </div>
    )
  }

  const artist = data?.artist
  const artistTracks = data?.topTracks || []
  const artistAlbums = data?.albums || []
  const artistSingles = data?.singles || []
  const related = data?.relatedArtists || []

  if (error || !artist) {
    return (
      <NotFound
        title="Artist not found"
        message={error ? error.message : "This artist profile couldn't be loaded."}
      />
    )
  }

  const start = () => artistTracks.length > 0 && playQueue(artistTracks, 0)

  return (
    <div className="page page--detail">
      <div className="artist-hero">
        <img
          src={artist.image || artist.thumbnailUrl || artwork(artist.name || 'echo-artist', 300)}
          alt=""
          referrerPolicy="no-referrer"
          onError={(e) => {
            const fallback = artwork(artist.name || 'echo-artist', 300)
            if (e.currentTarget.src !== fallback) e.currentTarget.src = fallback
          }}
        />
        <div className="artist-hero__copy">
          <h1>{artist.name}</h1>
          <p>Artist</p>
          {artist.monthlyListeners ? (
            <p>{formatCount(artist.monthlyListeners)} monthly listeners</p>
          ) : artist.subscribers ? (
            <p>{artist.subscribers} subscribers</p>
          ) : null}
          <div className="detail-actions">
            <button
              className="play-button play-button--large"
              type="button"
              aria-label={`Play ${artist.name}`}
              onClick={start}
            >
              <Play size={20} fill="currentColor" />
            </button>
            <button
              className={classNames('button', followed ? 'button--quiet' : 'button--outline')}
              type="button"
              onClick={() => {
                toggleFollow(artistId)
                setToast(followed ? `Unfollowed ${artist.name}` : `Following ${artist.name}`)
              }}
            >
              {followed ? (
                <>
                  <Check size={15} /> Following
                </>
              ) : (
                <>
                  <Plus size={15} /> Follow
                </>
              )}
            </button>
          </div>
        </div>
      </div>
      {artist.bio && (
        <section className="page-section artist-bio">
          <SectionHeading title="A little about" />
          <p>{artist.bio}</p>
          {artist.genres && artist.genres.length > 0 && (
            <div className="artist-genres">
              {artist.genres.map((genre) => (
                <span key={genre}>{genre}</span>
              ))}
            </div>
          )}
        </section>
      )}
      {artistTracks.length > 0 && (
        <section className="page-section">
          <SectionHeading title="Popular" />
          <TrackList items={artistTracks} />
        </section>
      )}
      {artistAlbums.length > 0 && (
        <section className="page-section">
          <SectionHeading title="Albums & EPs" />
          <div className="media-grid">
            {artistAlbums.map((album) => (
              <MediaCard key={album.id} kind="album" item={album} />
            ))}
          </div>
        </section>
      )}
      {artistSingles.length > 0 && (
        <section className="page-section">
          <SectionHeading title="Singles" />
          <div className="media-grid">
            {artistSingles.map((album) => (
              <MediaCard key={album.id} kind="album" item={album} />
            ))}
          </div>
        </section>
      )}
      {related.length > 0 && (
        <section className="page-section">
          <SectionHeading
            title="If you like this"
            action={
              <Link className="text-link" to="/discover">
                Explore
              </Link>
            }
          />
          <div className="media-grid media-grid--artists">
            {related.map((item) => (
              <MediaCard key={item.id} kind="artist" item={item} />
            ))}
          </div>
        </section>
      )}
    </div>
  )
}

function NotFound({ title, message }: { title: string; message: string }) {
  return (
    <div className="not-found">
      <h1>{title}</h1>
      <p>{message}</p>
      <Link className="button button--primary" to="/library">
        <ArrowLeft size={15} /> Back to your library
      </Link>
    </div>
  )
}
