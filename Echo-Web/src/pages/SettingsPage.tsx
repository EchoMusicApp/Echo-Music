import { useState, useEffect, useRef } from 'react'
import {
  Check,
  Copy,
  ExternalLink,
  Globe,
  HardDrive,
  Info,
  Languages,
  Moon,
  Palette,
  Radio,
  RotateCcw,
  Search,
  SlidersHorizontal,
  Smartphone,
  Sparkles,
  Sun,
  Trash2,
  DownloadCloud,
  FileUp,
  X,
} from 'lucide-react'
import { EchoMusicLogo } from '../components/EchoMusicLogo.tsx'
import { useLatestRelease } from '../hooks/useLatestRelease.ts'
import type { AccentColor, AudioQuality, UserPreferences } from '../types/domain.ts'
import { useEchoStore } from '../stores/useEchoStore.ts'
import { classNames } from '../utils/classNames.ts'
import { useAudioEngine } from '../hooks/useAudioEngine.ts'
import { getLastFmAuthUrl, fetchLastFmSession } from '../services/lastfm/lastfmService.ts'
import { castService } from '../services/cast/castService.ts'
import {
  getStorageUsage,
  exportFullBackup,
  importFullBackup,
  clearListeningHistory,
} from '../services/db/echoDatabase.ts'
import {
  getExcludedTracks,
  restoreExcludedTrack,
} from '../services/recommendations/localTasteEngine.ts'
import { triggerFileDownload } from '../services/sharing/playlistPortability.ts'

const accents: { value: AccentColor; label: string; color: string }[] = [
  { value: 'violet', label: 'Violet', color: '#8b79e8' },
  { value: 'emerald', label: 'Fern', color: '#58ad83' },
  { value: 'amber', label: 'Ochre', color: '#d59b52' },
  { value: 'rose', label: 'Rose', color: '#d5798f' },
  { value: 'sky', label: 'Sky', color: '#59a4c6' },
]

const qualityOptions: { value: AudioQuality; label: string; description: string }[] = [
  { value: 'low', label: 'Low', description: 'Smallest data footprint (AAC ~128 kbps)' },
  { value: 'medium', label: 'Standard', description: 'Balanced Opus quality (~100 kbps)' },
  { value: 'high', label: 'High', description: 'Highest available stream quality (~160 kbps)' },
  {
    value: 'lossless',
    label: 'Lossless',
    description: 'Same as High for YouTube sources (lossless streams are not available)',
  },
]

const regions = [
  { code: 'US', name: 'United States' },
  { code: 'GB', name: 'United Kingdom' },
  { code: 'IN', name: 'India' },
  { code: 'JP', name: 'Japan' },
  { code: 'DE', name: 'Germany' },
  { code: 'FR', name: 'France' },
  { code: 'CA', name: 'Canada' },
  { code: 'AU', name: 'Australia' },
  { code: 'BR', name: 'Brazil' },
  { code: 'KR', name: 'South Korea' },
]

type BooleanPreference = {
  [K in keyof UserPreferences]: UserPreferences[K] extends boolean | undefined ? K : never
}[keyof UserPreferences]

function PreferenceToggle<K extends BooleanPreference>({
  label,
  description,
  preference,
  value,
  onChange,
}: {
  label: string
  description: string
  preference: K
  value: boolean | undefined
  onChange: (key: K, value: boolean) => void
}) {
  const checked = Boolean(value)
  return (
    <label className="setting-row">
      <span>
        <strong>{label}</strong>
        <small>{description}</small>
      </span>
      <input
        className="switch"
        type="checkbox"
        checked={checked}
        onChange={(event) => onChange(preference, event.target.checked)}
      />
    </label>
  )
}

export function SettingsPage() {
  const release = useLatestRelease()
  const preferences = useEchoStore((state) => state.preferences)
  const setPreference = useEchoStore((state) => state.setPreference)
  const likedTrackIds = useEchoStore((state) => state.likedTrackIds)
  const notify = useEchoStore((state) => state.setToast)
  const { setCrossfadeDuration, setVolumeNormalization, getAudioTelemetry } = useAudioEngine()

  const [activeSection, setActiveSection] = useState('all')
  const [settingsFilter, setSettingsFilter] = useState('')
  const [storageStats, setStorageStats] = useState<{
    localFilesCount: number
    historyCount: number
  } | null>(null)
  const [excludedTracks, setExcludedTracks] = useState<
    Array<{ trackId: string; trackTitle: string; artistName: string }>
  >([])

  const restoreInputRef = useRef<HTMLInputElement>(null)
  const telemetry = getAudioTelemetry()

  const sections = [
    {
      id: 'playback',
      label: 'Playback & Audio',
      icon: SlidersHorizontal,
      keywords: 'quality crossfade normalize data saver streaming silence audio dsp',
    },
    {
      id: 'appearance',
      label: 'Appearance',
      icon: Palette,
      keywords: 'theme accent scroll compact layout dark light system',
    },
    {
      id: 'content',
      label: 'Content & Region',
      icon: Globe,
      keywords: 'region explicit start tab catalog language',
    },
    {
      id: 'lyrics',
      label: 'Lyrics & Providers',
      icon: Languages,
      keywords: 'lyrics provider romanize translation lrclib kugou',
    },
    {
      id: 'integrations',
      label: 'Connected Services',
      icon: Radio,
      keywords: 'lastfm scrobble discord presence cast streaming chromecast',
    },
    {
      id: 'taste',
      label: 'Recommendation Exclusions',
      icon: Sparkles,
      keywords: 'exclude recommend restore taste songs',
    },
    {
      id: 'storage',
      label: 'Storage & Backup',
      icon: HardDrive,
      keywords: 'backup export restore history clear diagnostics database reset',
    },
    {
      id: 'about',
      label: 'About Echo',
      icon: Info,
      keywords: 'version telemetry engine legal info license',
    },
  ]

  useEffect(() => {
    let active = true
    Promise.all([getStorageUsage(), getExcludedTracks()])
      .then(([stats, exclusions]) => {
        if (active) {
          setStorageStats(stats)
          setExcludedTracks(exclusions)
        }
      })
      .catch(() => {})

    // Check for Last.fm OAuth callback token
    const params = new URLSearchParams(window.location.search)
    const token = params.get('token')
    if (token) {
      params.delete('token')
      const rest = params.toString()
      window.history.replaceState(null, '', window.location.pathname + (rest ? `?${rest}` : ''))
      void fetchLastFmSession(token)
        .then((session) => {
          setPreference('lastfmSession', session)
          setPreference('lastfmScrobbling', true)
          notify(`Connected to Last.fm as ${session.username}`)
        })
        .catch((err) => {
          notify(err instanceof Error ? err.message : 'Failed to connect to Last.fm')
        })
    }

    return () => {
      active = false
    }
  }, [setPreference, notify])

  const save = <K extends keyof UserPreferences>(key: K, value: UserPreferences[K]) => {
    setPreference(key, value)
    if (key === 'crossfadeDuration' && typeof value === 'number') {
      setCrossfadeDuration(value)
    }
    if (key === 'volumeNormalization' && typeof value === 'boolean') {
      setVolumeNormalization(value)
    }
    notify('Preference saved')
  }

  const jumpTo = (id: string) => {
    setActiveSection(id)
    if (id === 'all') {
      window.scrollTo({ top: 0, behavior: preferences.smoothScrolling ? 'smooth' : 'auto' })
      return
    }
    document.getElementById(`settings-${id}`)?.scrollIntoView({
      behavior: preferences.smoothScrolling ? 'smooth' : 'auto',
      block: 'start',
    })
  }

  const handleExportBackup = async () => {
    try {
      const json = await exportFullBackup(
        preferences as unknown as Record<string, unknown>,
        likedTrackIds,
      )
      triggerFileDownload(
        json,
        `echo-backup-${new Date().toISOString().slice(0, 10)}.json`,
        'application/json',
      )
      notify('Exported full database backup')
    } catch {
      notify('Failed to export backup')
    }
  }

  const refreshStorageData = async () => {
    try {
      const stats = await getStorageUsage()
      setStorageStats(stats)
      const exclusions = await getExcludedTracks()
      setExcludedTracks(exclusions)
    } catch {
      // Ignore
    }
  }

  const handleRestoreBackup = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    try {
      const content = await file.text()
      const res = await importFullBackup(content, 'merge')
      notify(`Restored ${res.playlistsRestored} playlists and ${res.historyRestored} history items`)
      await refreshStorageData()
    } catch (err) {
      notify(err instanceof Error ? err.message : 'Invalid backup file')
    } finally {
      if (e.target) e.target.value = ''
    }
  }

  const handleRestoreExcluded = async (trackId: string, trackTitle: string) => {
    await restoreExcludedTrack(trackId)
    notify(`Restored "${trackTitle}" to recommendations`)
    await refreshStorageData()
  }

  const handleClearHistoryAction = async () => {
    await clearListeningHistory()
    notify('Cleared listening history')
    await refreshStorageData()
  }

  const matchesFilter = (sectionId: string, label: string, keywords: string) => {
    if (!settingsFilter.trim()) return true
    const q = settingsFilter.toLowerCase().trim()
    return (
      label.toLowerCase().includes(q) || keywords.toLowerCase().includes(q) || sectionId.includes(q)
    )
  }

  return (
    <div className="page settings-page">
      <header className="settings-header">
        <div className="settings-header__copy">
          <h1>Settings</h1>
          <p>Configure playback DSP, visual theme, regional catalog, and local data persistence.</p>
        </div>
        <div className="settings-header__search">
          <Search size={15} />
          <input
            type="search"
            placeholder="Search settings..."
            value={settingsFilter}
            onChange={(e) => setSettingsFilter(e.target.value)}
            aria-label="Filter settings"
          />
          {settingsFilter && (
            <button
              type="button"
              onClick={() => setSettingsFilter('')}
              className="absolute top-1/2 right-2.5 -translate-y-1/2 text-white/40 hover:text-white"
              aria-label="Clear filter"
            >
              <X size={14} />
            </button>
          )}
        </div>
      </header>

      <div className="settings-pills" role="tablist" aria-label="Settings categories">
        <button
          type="button"
          role="tab"
          aria-selected={activeSection === 'all'}
          className={classNames('settings-pill', activeSection === 'all' && 'is-active')}
          onClick={() => jumpTo('all')}
        >
          All Categories
        </button>
        {sections.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            type="button"
            role="tab"
            aria-selected={activeSection === id}
            className={classNames('settings-pill', activeSection === id && 'is-active')}
            onClick={() => jumpTo(id)}
          >
            <Icon size={14} />
            <span>{label}</span>
          </button>
        ))}
      </div>

      <div className="settings-content">
        {/* Section: Playback & Audio DSP */}
        {matchesFilter(
          'playback',
          'Playback & Audio DSP',
          'quality crossfade normalize data saver streaming silence',
        ) && (
          <section id="settings-playback" className="settings-section">
            <div className="settings-section__heading">
              <span className="settings-section__icon">
                <SlidersHorizontal size={18} />
              </span>
              <div>
                <h2>Playback & Audio DSP</h2>
                <p>Fine-tune crossfades, volume normalization, and streaming fidelity.</p>
              </div>
            </div>

            <div className="quality-setting">
              <div>
                <strong>Streaming audio quality</strong>
                <small>Preferred audio bitrate and encoding for stream resolution.</small>
              </div>
              <div
                className="quality-options"
                role="radiogroup"
                aria-label="Audio quality preference"
              >
                {qualityOptions.map((option) => (
                  <button
                    key={option.value}
                    className={classNames(
                      'quality-option',
                      preferences.audioQuality === option.value && 'is-active',
                    )}
                    type="button"
                    role="radio"
                    aria-checked={preferences.audioQuality === option.value}
                    onClick={() => save('audioQuality', option.value)}
                  >
                    <span>
                      <strong>{option.label}</strong>
                      <small>{option.description}</small>
                    </span>
                    {preferences.audioQuality === option.value && <Check size={16} />}
                  </button>
                ))}
              </div>
            </div>

            <div className="setting-row">
              <span>
                <strong>Crossfade duration</strong>
                <small>
                  Transition time between consecutive tracks ({preferences.crossfadeDuration ?? 3}
                  s).
                </small>
              </span>
              <div className="flex items-center gap-3">
                <input
                  type="range"
                  min={0}
                  max={12}
                  step={1}
                  value={preferences.crossfadeDuration ?? 3}
                  onChange={(e) => save('crossfadeDuration', Number(e.target.value))}
                  className="w-32 accent-[#7c5cff]"
                  aria-label="Crossfade duration in seconds"
                />
                <span className="w-8 font-mono text-xs text-[#a59fad]">
                  {preferences.crossfadeDuration ?? 3}s
                </span>
              </div>
            </div>

            <PreferenceToggle
              label="Volume normalization"
              description="DynamicsCompressorNode evens out perceived loudness across diverse master recordings."
              preference="volumeNormalization"
              value={preferences.volumeNormalization}
              onChange={save}
            />

            <PreferenceToggle
              label="Skip silence"
              description="Automatically detects and skips leading silence at the start of tracks."
              preference="skipSilence"
              value={preferences.skipSilence}
              onChange={save}
            />

            <PreferenceToggle
              label="Data saver"
              description="Prefers lower stream bitrates and suspends background track preloading."
              preference="dataSaver"
              value={preferences.dataSaver}
              onChange={save}
            />
          </section>
        )}

        {/* Section: Appearance */}
        {matchesFilter('appearance', 'Appearance', 'theme accent scroll compact layout') && (
          <section id="settings-appearance" className="settings-section">
            <div className="settings-section__heading">
              <span className="settings-section__icon">
                <Palette size={18} />
              </span>
              <div>
                <h2>Appearance</h2>
                <p>Customize the visual presentation and layout density.</p>
              </div>
            </div>

            <div className="setting-row">
              <span>
                <strong>Theme mode</strong>
                <small>Select light, dark, or system color scheme.</small>
              </span>
              <div className="segmented-control" role="group" aria-label="Theme mode">
                <button
                  type="button"
                  className={preferences.themeMode === 'light' ? 'is-active' : ''}
                  aria-pressed={preferences.themeMode === 'light'}
                  onClick={() => save('themeMode', 'light')}
                >
                  <Sun size={14} /> Light
                </button>
                <button
                  type="button"
                  className={preferences.themeMode === 'dark' ? 'is-active' : ''}
                  aria-pressed={preferences.themeMode === 'dark'}
                  onClick={() => save('themeMode', 'dark')}
                >
                  <Moon size={14} /> Dark
                </button>
                <button
                  type="button"
                  className={preferences.themeMode === 'system' ? 'is-active' : ''}
                  aria-pressed={preferences.themeMode === 'system'}
                  onClick={() => save('themeMode', 'system')}
                >
                  <Sparkles size={14} /> System
                </button>
              </div>
            </div>

            <div className="setting-row">
              <span>
                <strong>Accent colour</strong>
                <small>
                  Highlight tint applied to playback progress, active badges, and focus rings.
                </small>
              </span>
              <div className="accent-options" role="radiogroup" aria-label="Accent colour">
                {accents.map((accent) => (
                  <button
                    key={accent.value}
                    className={classNames(
                      'accent-swatch',
                      preferences.accentColor === accent.value && 'is-selected',
                    )}
                    style={{ '--swatch-color': accent.color } as React.CSSProperties}
                    type="button"
                    role="radio"
                    aria-checked={preferences.accentColor === accent.value}
                    aria-label={accent.label}
                    onClick={() => save('accentColor', accent.value)}
                  >
                    {preferences.accentColor === accent.value && <Check size={16} />}
                  </button>
                ))}
              </div>
            </div>

            <PreferenceToggle
              label="Smooth scrolling"
              description="Smooth animated transitions when jumping through playlists and queues."
              preference="smoothScrolling"
              value={preferences.smoothScrolling}
              onChange={save}
            />

            <PreferenceToggle
              label="Compact list view"
              description="Denser track rows designed for large collections and desktop displays."
              preference="compactLists"
              value={preferences.compactLists}
              onChange={save}
            />
          </section>
        )}

        {/* Section: Content & Region */}
        {matchesFilter('content', 'Content & Regional Catalog', 'region explicit start tab') && (
          <section id="settings-content" className="settings-section">
            <div className="settings-section__heading">
              <span className="settings-section__icon">
                <Globe size={18} />
              </span>
              <div>
                <h2>Content & Regional Catalog</h2>
                <p>Configure geographical catalog target and content filtering.</p>
              </div>
            </div>

            <div className="setting-row">
              <span>
                <strong>Default startup tab</strong>
                <small>Which screen Echo opens when launched.</small>
              </span>
              <select
                className="settings-select"
                value={preferences.defaultTab || 'home'}
                onChange={(e) =>
                  save('defaultTab', e.target.value as UserPreferences['defaultTab'])
                }
              >
                <option value="home">Home</option>
                <option value="search">Search</option>
                <option value="discover">Discover</option>
                <option value="library">Library</option>
                <option value="settings">Settings</option>
              </select>
            </div>

            <div className="setting-row">
              <span>
                <strong>Catalog region</strong>
                <small>
                  Geographical locale for trending charts, new releases, and discover shelves.
                </small>
              </span>
              <select
                className="settings-select"
                value={preferences.contentRegion || 'US'}
                onChange={(e) => save('contentRegion', e.target.value)}
              >
                {regions.map((reg) => (
                  <option key={reg.code} value={reg.code}>
                    {reg.name} ({reg.code})
                  </option>
                ))}
              </select>
            </div>

            <PreferenceToggle
              label="Show explicit content"
              description="Include tracks with explicit language markers in search results and charts."
              preference="showExplicit"
              value={preferences.showExplicit}
              onChange={save}
            />
          </section>
        )}

        {/* Section: Lyrics & Providers */}
        {matchesFilter(
          'lyrics',
          'Lyrics & Providers',
          'lyrics provider romanize translation lrclib kugou',
        ) && (
          <section id="settings-lyrics" className="settings-section">
            <div className="settings-section__heading">
              <span className="settings-section__icon">
                <Languages size={18} />
              </span>
              <div>
                <h2>Lyrics & Providers</h2>
                <p>Configure synchronized lyric sources and phonetic transliteration.</p>
              </div>
            </div>

            <div className="setting-row">
              <span>
                <strong>Preferred synchronized provider</strong>
                <small>
                  Primary database queried for word-by-word and line-by-line lyric tracks.
                </small>
              </span>
              <select
                className="settings-select"
                value={preferences.preferredLyricsProvider || 'LRCLIB'}
                onChange={(e) =>
                  save(
                    'preferredLyricsProvider',
                    e.target.value as UserPreferences['preferredLyricsProvider'],
                  )
                }
              >
                <option value="LRCLIB">LRCLIB (Fast & Open Source)</option>
                <option value="BETTER_LYRICS">BetterLyrics (TTML Word Timing)</option>
                <option value="KUGOU">KuGou (Global Asian Catalog)</option>
              </select>
            </div>

            <PreferenceToggle
              label="Romanize lyrics"
              description="Show phonetic romanizations (pinyin / romaja / romaji) when non-Latin scripts are present."
              preference="romanizeLyrics"
              value={preferences.romanizeLyrics}
              onChange={save}
            />
          </section>
        )}

        {/* Section: Connected Services */}
        {matchesFilter(
          'integrations',
          'Connected Services',
          'lastfm scrobble discord presence cast streaming',
        ) && (
          <section id="settings-integrations" className="settings-section">
            <div className="settings-section__heading">
              <span className="settings-section__icon">
                <Radio size={18} />
              </span>
              <div>
                <h2>Connected Services</h2>
                <p>External scrobbling, local desktop presence, and casting bridges.</p>
              </div>
            </div>

            <div className="setting-row">
              <span>
                <strong>Last.fm scrobbling</strong>
                <small>
                  {preferences.lastfmSession?.username
                    ? `Connected as ${preferences.lastfmSession.username}`
                    : 'Scrobble listening history to your Last.fm profile as tracks play.'}
                </small>
              </span>
              <div>
                {preferences.lastfmSession ? (
                  <button
                    type="button"
                    className="button button--quiet"
                    onClick={() => {
                      save('lastfmSession', undefined)
                      save('lastfmScrobbling', false)
                      notify('Disconnected from Last.fm')
                    }}
                  >
                    Disconnect
                  </button>
                ) : (
                  <button
                    type="button"
                    className="button button--primary"
                    onClick={async () => {
                      try {
                        const url = await getLastFmAuthUrl()
                        window.location.href = url
                      } catch (err) {
                        notify(err instanceof Error ? err.message : 'Could not start Last.fm auth')
                      }
                    }}
                  >
                    Connect Last.fm
                  </button>
                )}
              </div>
            </div>

            {preferences.lastfmSession && (
              <PreferenceToggle
                label="Automatic scrobbling"
                description="Submits scrobbles once a song reaches 50% or 4 minutes of playback."
                preference="lastfmScrobbling"
                value={preferences.lastfmScrobbling}
                onChange={save}
              />
            )}

            <PreferenceToggle
              label="Discord Rich Presence"
              description="Broadcasts current track title and artist to your Discord desktop status."
              preference="discordRpc"
              value={preferences.discordRpc}
              onChange={save}
            />

            <PreferenceToggle
              label="Google Cast"
              description="Enables streaming directly to Google Cast-compatible hardware and speakers."
              preference="googleCast"
              value={preferences.googleCast}
              onChange={(key, val) => {
                save(key, val)
                if (val) castService.init()
              }}
            />
          </section>
        )}

        {/* Section: Recommendation Exclusions */}
        {matchesFilter('taste', 'Recommendation Exclusions', 'exclude recommend restore taste') && (
          <section id="settings-taste" className="settings-section">
            <div className="settings-section__heading">
              <span className="settings-section__icon">
                <Sparkles size={18} />
              </span>
              <div>
                <h2>Recommendation Exclusions</h2>
                <p>Tracks you have excluded from appearing in Local Taste recommendations.</p>
              </div>
            </div>

            {excludedTracks.length === 0 ? (
              <p className="rounded-xl border border-white/5 bg-white/2 p-4 text-xs text-[#a59fad]">
                No songs are currently excluded from your recommendations. Use track action menus to
                exclude tracks you never want to hear.
              </p>
            ) : (
              <div className="custom-scrollbar max-h-56 space-y-2 overflow-y-auto">
                {excludedTracks.map((item) => (
                  <div
                    key={item.trackId}
                    className="flex items-center justify-between rounded-xl border border-white/6 bg-white/3 p-3 text-xs"
                  >
                    <div>
                      <strong className="block text-white">{item.trackTitle}</strong>
                      <span className="text-[#a59fad]">{item.artistName}</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => handleRestoreExcluded(item.trackId, item.trackTitle)}
                      className="rounded-lg bg-[#7c5cff]/20 px-2.5 py-1 text-[11px] font-semibold text-[#b09cff] transition-colors hover:bg-[#7c5cff]/30"
                    >
                      Restore
                    </button>
                  </div>
                ))}
              </div>
            )}
          </section>
        )}

        {/* Section: Storage & Backup */}
        {matchesFilter(
          'storage',
          'Storage & Database Backup',
          'backup export restore history clear diagnostics database reset',
        ) && (
          <section id="settings-storage" className="settings-section">
            <div className="settings-section__heading">
              <span className="settings-section__icon">
                <HardDrive size={18} />
              </span>
              <div>
                <h2>Storage & Database Backup</h2>
                <p>
                  Inspect local database items, export portable library backups, and manage storage.
                </p>
              </div>
            </div>

            {storageStats && (
              <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-3">
                <div className="rounded-xl border border-white/6 bg-white/3 p-3.5 text-xs">
                  <span className="mb-1 block text-[#a59fad]">Local Scanned Files</span>
                  <strong className="font-mono text-base text-white">
                    {storageStats.localFilesCount} files
                  </strong>
                </div>

                <div className="rounded-xl border border-white/6 bg-white/3 p-3.5 text-xs">
                  <span className="mb-1 block text-[#a59fad]">Listening Events</span>
                  <strong className="font-mono text-base text-white">
                    {storageStats.historyCount} plays
                  </strong>
                </div>
              </div>
            )}

            <div className="mb-4 flex flex-wrap gap-2.5">
              <input
                type="file"
                ref={restoreInputRef}
                onChange={handleRestoreBackup}
                accept=".json"
                className="hidden"
              />

              <button
                type="button"
                onClick={handleExportBackup}
                className="flex items-center gap-1.5 rounded-xl bg-[#7c5cff] px-4 py-2 text-xs font-semibold text-black shadow-md transition-colors hover:bg-[#6c48ff]"
              >
                <DownloadCloud size={15} />
                <span>Export Backup (JSON)</span>
              </button>

              <button
                type="button"
                onClick={() => restoreInputRef.current?.click()}
                className="flex items-center gap-1.5 rounded-xl bg-white/6 px-4 py-2 text-xs font-semibold text-white transition-colors hover:bg-white/10"
              >
                <FileUp size={15} />
                <span>Restore Backup</span>
              </button>

              <button
                type="button"
                onClick={handleClearHistoryAction}
                className="flex items-center gap-1.5 rounded-xl bg-red-500/15 px-4 py-2 text-xs font-semibold text-red-300 transition-colors hover:bg-red-500/25"
              >
                <Trash2 size={15} />
                <span>Clear History</span>
              </button>
            </div>

            <button
              type="button"
              className="reset-preferences"
              onClick={() => {
                setPreference('accentColor', 'violet')
                setPreference('themeMode', 'dark')
                setPreference('volume', 68)
                setPreference('muted', false)
                setPreference('shuffle', false)
                setPreference('repeat', 'off')
                setPreference('showExplicit', true)
                setPreference('volumeNormalization', false)
                setPreference('audioQuality', 'high')
                setPreference('smoothScrolling', true)
                setPreference('compactLists', false)
                setPreference('lastPlayedLayout', 'grid')
                setPreference('contentRegion', 'US')
                setPreference('crossfadeDuration', 3)
                setPreference('preferredLyricsProvider', 'LRCLIB')
                setPreference('defaultTab', 'home')
                setPreference('dataSaver', false)
                setPreference('romanizeLyrics', false)
                notify('Preferences reset to default values')
              }}
            >
              <RotateCcw size={15} /> Reset preferences to defaults
            </button>
          </section>
        )}

        {/* Section: About */}
        {matchesFilter(
          'about',
          'About Echo Music',
          'version telemetry engine legal info license app android',
        ) && (
          <section id="settings-about" className="settings-section settings-about">
            <div className="settings-section__heading">
              <span className="settings-section__icon">
                <Info size={18} />
              </span>
              <div>
                <h2>About Echo Music</h2>
                <p>Architecture, audio telemetry, and policy compliance.</p>
              </div>
            </div>

            <div className="about-card">
              <div className="about-card__brand">
                <span className="brand__symbol">
                  <EchoMusicLogo size={20} />
                </span>
                <strong>
                  Echo <span>Music</span>
                </strong>
              </div>
              <p>
                Echo Music Web provides ad-free streaming, dual-deck crossfading, synchronized
                lyrics, on-device Local Taste recommendations, and zero-media-caching architecture
                compliant with open legal policies.
              </p>
              <div className="mt-3 flex flex-wrap gap-4 border-t border-white/6 pt-3 font-mono text-xs text-[#a59fad]">
                <span>Engine Codec: {telemetry.codec}</span>
                <span>Sample Rate: {telemetry.sampleRate}</span>
                <span>Bit Depth: {telemetry.bitDepth}</span>
              </div>
              <div className="mt-4 flex flex-wrap items-center gap-3">
                <a
                  href={release.apkUrl}
                  download
                  className="flex items-center gap-1.5 rounded-lg bg-[#ed5564]/15 px-3 py-1.5 text-[11px] font-semibold text-[#ed5564] transition-colors hover:bg-[#ed5564]/25"
                  title={`Download Echo Music ${release.version} APK`}
                >
                  <Smartphone size={13} /> Download Mobile App ({release.version})
                </a>
                <a
                  href={release.githubUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-1.5 rounded-lg bg-white/6 px-3 py-1.5 text-[11px] font-semibold text-[#a59fad] transition-colors hover:bg-white/10 hover:text-white"
                >
                  <ExternalLink size={13} /> GitHub Repository
                </a>
                <button
                  type="button"
                  onClick={() => {
                    void navigator.clipboard
                      .writeText(
                        JSON.stringify(
                          {
                            userAgent: navigator.userAgent,
                            online: navigator.onLine,
                            quality: preferences.audioQuality,
                            dataSaver: preferences.dataSaver ?? false,
                            telemetry,
                            storage: storageStats,
                          },
                          null,
                          2,
                        ),
                      )
                      .then(() => notify('Diagnostics copied to clipboard'))
                      .catch(() => notify('Could not access clipboard'))
                  }}
                  className="flex items-center gap-1.5 rounded-lg bg-white/6 px-3 py-1.5 text-[11px] font-semibold text-[#a59fad] transition-colors hover:bg-white/10"
                >
                  <Copy size={13} /> Copy diagnostics
                </button>
                <span className="about-card__version">Echo Music Web · v2.4.0</span>
              </div>
            </div>
          </section>
        )}
      </div>
    </div>
  )
}
