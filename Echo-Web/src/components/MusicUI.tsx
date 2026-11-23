import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  ArrowDown,
  ArrowUp,
  Heart,
  ListPlus,
  MoreHorizontal,
  Music2,
  Play,
  Plus,
  UserRound,
} from 'lucide-react'
import type { Album, Artist, Playlist, PlaylistSummary, Track } from '../types/domain.ts'
import { getCachedTrack, useEchoStore } from '../stores/useEchoStore.ts'
import { classNames } from '../utils/classNames.ts'
import { formatDuration, formatCount } from '../utils/format.ts'
import { artwork } from '../utils/artwork.ts'

export function PageHeading({
  title,
  description,
  action,
}: {
  title: string
  description?: string
  action?: ReactNode
}) {
  return (
    <header className="page-heading">
      <div>
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>
      {action}
    </header>
  )
}

export function SectionHeading({
  title,
  to,
  action,
}: {
  title: string
  to?: string
  action?: React.ReactNode
}) {
  return (
    <div className="section-heading">
      <h2>{title}</h2>
      {action ??
        (to ? (
          <Link className="text-link" to={to}>
            See all
          </Link>
        ) : null)}
    </div>
  )
}

export function MediaCard({
  item,
  kind,
  subtitle,
  compact = false,
}: {
  item: Album | Artist | Playlist | PlaylistSummary | Track
  kind: 'album' | 'artist' | 'playlist' | 'track'
  subtitle?: string
  compact?: boolean
}) {
  const isTrack = kind === 'track' || ('durationSeconds' in item && 'artists' in item)
  const isArtist = kind === 'artist' || ('monthlyListeners' in item && !('title' in item))
  const isAlbum = kind === 'album' || ('trackCount' in item && 'artists' in item)

  const image =
    'thumbnailUrl' in item && item.thumbnailUrl
      ? item.thumbnailUrl
      : 'image' in item && item.image
        ? item.image
        : 'artwork' in item && item.artwork
          ? item.artwork
          : ''

  const title = isArtist ? (item as Artist).name : 'title' in item ? item.title : ''

  const detail =
    subtitle ??
    (isArtist
      ? (item as Artist).monthlyListeners
        ? `${formatCount((item as Artist).monthlyListeners!)} monthly listeners`
        : 'Artist'
      : isAlbum
        ? `${(item as Album).artistName || 'Album'}${(item as Album).year ? ` · ${(item as Album).year}` : ''}`
        : isTrack
          ? (item as Track).artistName
          : 'description' in item && item.description
            ? item.description
            : 'author' in item && item.author
              ? item.author
              : '')

  const to = isArtist
    ? `/artists/${item.id}`
    : isAlbum
      ? `/albums/${item.id}`
      : isTrack
        ? `/albums/${(item as Track).albumId || item.id}`
        : `/playlists/${item.id}`

  const playTrack = useEchoStore((state) => state.playTrack)
  const navigate = useNavigate()

  const handlePlayClick = (e: React.MouseEvent) => {
    e.preventDefault()
    e.stopPropagation()
    if (isTrack) {
      playTrack(item as Track)
    } else {
      navigate(to)
    }
  }

  return (
    <article className={classNames('media-card', compact && 'media-card--compact')}>
      <Link
        to={to}
        className={classNames('media-card__art', isArtist && 'media-card__art--artist')}
        aria-label={`Open ${title}`}
      >
        <img
          src={image || artwork(title || 'echo-item', 300)}
          alt=""
          loading="lazy"
          referrerPolicy="no-referrer"
          onError={(e) => {
            const fallback = artwork(title || 'echo-item', 300)
            if (e.currentTarget.src !== fallback) {
              e.currentTarget.src = fallback
            }
          }}
        />
        {isArtist && (
          <span className="artist-art-mark">
            <UserRound size={20} />
          </span>
        )}
      </Link>
      <button
        className="media-card__play"
        type="button"
        aria-label={`Play ${title}`}
        title={`Play ${title}`}
        onClick={handlePlayClick}
      >
        <Play size={16} fill="currentColor" aria-hidden="true" />
      </button>
      <div className="media-card__copy">
        <Link to={to} className="media-card__title">
          {title}
        </Link>
        <p>{detail}</p>
      </div>
      <div className="media-card__menu">
        <MediaMenu
          kind={isArtist ? 'artist' : isAlbum ? 'album' : isTrack ? 'track' : 'playlist'}
          id={item.id}
          track={isTrack ? (item as Track) : undefined}
        />
      </div>
    </article>
  )
}

export function TrackRow({
  track,
  index,
  showAlbum = true,
  onRemove,
  onMove,
  onPlay,
}: {
  track: Track
  index: number
  showAlbum?: boolean
  onRemove?: () => void
  onMove?: (direction: -1 | 1) => void
  onPlay?: () => void
}) {
  const playTrack = useEchoStore((state) => state.playTrack)
  const isPlaying = useEchoStore((state) => state.isPlaying)
  const currentTrackId = useEchoStore((state) => state.currentTrackId)
  const liked = useEchoStore((state) => state.likedTrackIds.includes(track.id))
  const toggleLike = useEchoStore((state) => state.toggleTrackLike)
  const current = currentTrackId === track.id
  const durationMs = track.durationMs || (track.durationSeconds ? track.durationSeconds * 1000 : 0)

  const handlePlay = () => {
    if (onPlay) {
      onPlay()
    } else {
      playTrack(track)
    }
  }

  return (
    <div className={classNames('track-row', current && isPlaying && 'track-row--playing')}>
      <div className="track-row__number">
        <span>
          {current && isPlaying ? (
            <span className="playing-bars" aria-label="Now playing">
              <i />
              <i />
              <i />
            </span>
          ) : (
            index + 1
          )}
        </span>
        <button
          className="icon-button track-row__play"
          type="button"
          aria-label={`Play ${track.title}`}
          onClick={handlePlay}
        >
          <Play size={14} fill="currentColor" />
        </button>
      </div>
      <button
        className="track-row__art"
        type="button"
        onClick={handlePlay}
        aria-label={`Play ${track.title}`}
      >
        <img
          src={
            track.artwork ||
            track.thumbnailUrl ||
            (track.videoId ? `https://i.ytimg.com/vi/${track.videoId}/hqdefault.jpg` : '') ||
            artwork(track.title || 'echo-track', 120)
          }
          alt=""
          loading="lazy"
          referrerPolicy="no-referrer"
          onError={(e) => {
            const fallback =
              track.thumbnailUrl ||
              (track.videoId ? `https://i.ytimg.com/vi/${track.videoId}/hqdefault.jpg` : '') ||
              track.albumImage ||
              artwork(track.title || 'echo-track', 120)
            if (e.currentTarget.src !== fallback) {
              e.currentTarget.src = fallback
            }
          }}
        />
      </button>
      <div className="track-row__main">
        <button className="track-row__title" type="button" onClick={handlePlay}>
          {track.title}
        </button>
        <Link className="track-row__artist" to={`/artists/${track.artistId}`}>
          {track.artistName}
        </Link>
      </div>
      {showAlbum && (
        <Link className="track-row__album" to={`/albums/${track.albumId}`}>
          {track.albumTitle}
        </Link>
      )}
      <button
        className={classNames('icon-button', 'track-row__like', liked && 'is-liked')}
        type="button"
        aria-label={liked ? `Unlike ${track.title}` : `Like ${track.title}`}
        aria-pressed={liked}
        onClick={() => toggleLike(track)}
      >
        <Heart size={16} fill={liked ? 'currentColor' : 'none'} />
      </button>
      <span className="track-row__duration">{formatDuration(durationMs)}</span>
      {onMove && (
        <span className="track-row__move">
          <button
            className="icon-button"
            type="button"
            aria-label={`Move ${track.title} up`}
            disabled={index === 0}
            onClick={() => onMove(-1)}
          >
            <ArrowUp size={14} />
          </button>
          <button
            className="icon-button"
            type="button"
            aria-label={`Move ${track.title} down`}
            onClick={() => onMove(1)}
          >
            <ArrowDown size={14} />
          </button>
        </span>
      )}
      <MediaMenu kind="track" id={track.id} track={track} {...(onRemove ? { onRemove } : {})} />
    </div>
  )
}

type MenuKind = 'track' | 'album' | 'artist' | 'playlist'

function MediaMenu({
  kind,
  id,
  track,
  onRemove,
}: {
  kind: MenuKind
  id: string
  track?: Track | undefined
  onRemove?: () => void
}) {
  const [open, setOpen] = useState(false)
  const [showPlaylists, setShowPlaylists] = useState(false)
  const ref = useRef<HTMLDivElement>(null)
  const playlists = useEchoStore((state) => state.playlists)
  const enqueue = useEchoStore((state) => state.enqueue)
  const playTrack = useEchoStore((state) => state.playTrack)
  const addTrackToPlaylist = useEchoStore((state) => state.addTrackToPlaylist)
  const toggleTrackLike = useEchoStore((state) => state.toggleTrackLike)
  const toggleAlbumLike = useEchoStore((state) => state.toggleAlbumLike)
  const toggleArtistFollow = useEchoStore((state) => state.toggleArtistFollow)
  const likedTrack = useEchoStore((state) => state.likedTrackIds.includes(id))
  const likedAlbum = useEchoStore((state) => state.likedAlbumIds.includes(id))
  const followed = useEchoStore((state) => state.followedArtistIds.includes(id))
  const notify = useEchoStore((state) => state.setToast)

  const activeTrack = track || getCachedTrack(id)

  useEffect(() => {
    if (!open) return
    const close = (event: MouseEvent | KeyboardEvent) => {
      if (event instanceof KeyboardEvent && event.key === 'Escape') setOpen(false)
      if (event instanceof MouseEvent && !ref.current?.contains(event.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', close)
    document.addEventListener('keydown', close)
    return () => {
      document.removeEventListener('mousedown', close)
      document.removeEventListener('keydown', close)
    }
  }, [open])

  const closeMenu = () => {
    setOpen(false)
    setShowPlaylists(false)
  }

  const addToPlaylist = (playlistId: string) => {
    if (activeTrack) {
      addTrackToPlaylist(playlistId, activeTrack)
      notify('Added to playlist')
    }
    closeMenu()
  }

  const menuItem = (label: string, action: () => void, icon?: React.ReactNode) => (
    <button className="menu-item" type="button" role="menuitem" onClick={action}>
      {icon}
      {label}
    </button>
  )

  return (
    <div className="menu-wrap" ref={ref}>
      <button
        className="icon-button"
        type="button"
        aria-label={`More ${kind} options`}
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        <MoreHorizontal size={19} />
      </button>
      {open && (
        <div className="context-menu" role="menu">
          {activeTrack &&
            menuItem(
              'Play next',
              () => {
                enqueue(activeTrack)
                notify('Added to your queue')
                closeMenu()
              },
              <ListPlus size={15} />,
            )}
          {activeTrack &&
            menuItem(
              'Play now',
              () => {
                playTrack(activeTrack)
                closeMenu()
              },
              <Play size={15} />,
            )}
          {activeTrack &&
            menuItem(
              likedTrack ? 'Remove from Liked Songs' : 'Add to Liked Songs',
              () => {
                toggleTrackLike(activeTrack)
                closeMenu()
              },
              <Heart size={15} />,
            )}
          {activeTrack && (
            <button
              className="menu-item"
              type="button"
              role="menuitem"
              aria-pressed={showPlaylists}
              onClick={() => setShowPlaylists((value) => !value)}
            >
              <Plus size={15} />
              Add to playlist
            </button>
          )}
          {showPlaylists && (
            <div className="menu-sublist" role="group" aria-label="Choose a playlist">
              {playlists.length ? (
                playlists.map((item) => (
                  <button
                    className="menu-item menu-item--sub"
                    type="button"
                    role="menuitem"
                    key={item.id}
                    onClick={() => addToPlaylist(item.id)}
                  >
                    {item.title}
                  </button>
                ))
              ) : (
                <span className="menu-empty">Create a playlist first</span>
              )}
            </div>
          )}
          {kind === 'album' &&
            menuItem(
              likedAlbum ? 'Remove from your library' : 'Save to your library',
              () => {
                toggleAlbumLike(id)
                closeMenu()
              },
              <Heart size={15} />,
            )}
          {kind === 'artist' &&
            menuItem(
              followed ? 'Unfollow artist' : 'Follow artist',
              () => {
                toggleArtistFollow(id)
                closeMenu()
              },
              <UserRound size={15} />,
            )}
          {kind === 'playlist' && (
            <Link className="menu-item" role="menuitem" to={`/playlists/${id}`} onClick={closeMenu}>
              Open playlist
            </Link>
          )}
          {onRemove &&
            menuItem('Remove from this playlist', () => {
              onRemove()
              closeMenu()
            })}
        </div>
      )}
    </div>
  )
}

export function TrackList({
  items,
  showAlbum = true,
  onRemove,
  onMove,
  onPlay,
}: {
  items: Track[]
  showAlbum?: boolean
  onRemove?: (track: Track) => void
  onMove?: (track: Track, direction: -1 | 1) => void
  onPlay?: (track: Track, index: number) => void
}) {
  const playQueue = useEchoStore((state) => state.playQueue)

  return (
    <div className="track-list">
      <div
        className={classNames('track-list__head', !showAlbum && 'track-list__head--compact')}
        aria-hidden="true"
      >
        <span>#</span>
        <span>Title</span>
        {showAlbum && <span>Album</span>}
        <span className="track-list__time">Time</span>
        <span />
      </div>
      {items.length ? (
        items.map((track, index) => (
          <TrackRow
            key={`${track.id}-${index}`}
            track={track}
            index={index}
            showAlbum={showAlbum}
            onPlay={() => {
              if (onPlay) {
                onPlay(track, index)
              } else {
                playQueue(items, index)
              }
            }}
            {...(onRemove ? { onRemove: () => onRemove(track) } : {})}
            {...(onMove ? { onMove: (dir: -1 | 1) => onMove(track, dir) } : {})}
          />
        ))
      ) : (
        <div className="empty-state">
          <Music2 size={26} />
          <h3>No tracks here yet</h3>
          <p>When you add music, it will show up here.</p>
        </div>
      )}
    </div>
  )
}

export function CreatePlaylistDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const createPlaylist = useEchoStore((state) => state.createPlaylist)
  const notify = useEchoStore((state) => state.setToast)
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const dialogRef = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    const onCancel = () => onClose()
    dialog.addEventListener('close', onCancel)
    return () => dialog.removeEventListener('close', onCancel)
  }, [onClose])

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    try {
      const id = createPlaylist(title, description)
      setTitle('')
      setDescription('')
      setError('')
      onClose()
      notify('Playlist created')
      navigate(`/playlists/${id}`)
    } catch (issue) {
      setError(issue instanceof Error ? issue.message : 'Could not create playlist.')
    }
  }

  return (
    <dialog className="dialog" ref={dialogRef} aria-labelledby="create-playlist-title">
      <form className="dialog__form" onSubmit={submit}>
        <h2 id="create-playlist-title">Create a playlist</h2>
        <label htmlFor="playlist-name">Name</label>
        <input
          id="playlist-name"
          autoFocus
          maxLength={60}
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="Give it a name"
          required
        />
        <label htmlFor="playlist-description">
          Description <span>Optional</span>
        </label>
        <textarea
          id="playlist-description"
          maxLength={240}
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          placeholder="What does this playlist feel like?"
          rows={3}
        />
        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}
        <div className="dialog__actions">
          <button type="button" className="button button--quiet" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="button button--primary">
            Create playlist
          </button>
        </div>
      </form>
    </dialog>
  )
}
