import { Download, ExternalLink, Headphones, Radio, ShieldCheck, Sliders } from 'lucide-react'
import { EchoMusicLogo } from './EchoMusicLogo.tsx'
import { useLatestRelease } from '../hooks/useLatestRelease.ts'

export function MobileApkLanding({ onContinueToWeb }: { onContinueToWeb: () => void }) {
  const release = useLatestRelease()

  return (
    <div className="mobile-apk-container">
      <header className="mobile-apk-header">
        <div className="mobile-apk-brand">
          <span className="brand__symbol">
            <EchoMusicLogo size={20} />
          </span>
          <span className="text-base font-bold tracking-tight text-white">
            Echo <span className="text-[#ed5564]">Music</span>
          </span>
        </div>
      </header>

      <main className="mobile-apk-content">
        <div className="mobile-apk-hero">
          <div className="mobile-apk-badge" aria-hidden="true">
            <img
              src="/Echo-new.png"
              alt="Echo Music Logo"
              width={72}
              height={72}
              className="h-16 w-16 rounded-2xl shadow-xl shadow-black/50"
            />
          </div>
          <h1 className="mobile-apk-heading">Echo Music for Android</h1>
          <p className="mobile-apk-description">
            The full-featured native Android music client with offline downloads, synced lyrics,
            parametric equalization, and ad-free streaming.
          </p>

          <div className="mobile-apk-chip">
            <span className="mobile-apk-chip-dot" />
            <span>Latest Release: {release.version}</span>
            {release.sizeMb && <span className="text-[#a59fad]">({release.sizeMb})</span>}
          </div>
        </div>

        <div className="mobile-apk-buttons">
          <a
            href={release.apkUrl}
            className="button button--primary mobile-apk-btn-primary"
            download
            rel="noopener noreferrer"
          >
            <Download size={18} />
            <span>Download Latest APK</span>
          </a>

          <a
            href={release.githubUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="button button--quiet mobile-apk-btn-secondary"
          >
            <ExternalLink size={16} />
            <span>View on GitHub</span>
          </a>
        </div>

        <section className="mobile-apk-feature-list" aria-label="Key Features">
          <div className="mobile-apk-card">
            <Headphones size={20} className="shrink-0 text-[#b09cff]" />
            <div>
              <h2 className="text-sm font-semibold text-white">Offline Downloads</h2>
              <p className="mt-0.5 text-xs text-[#a59fad]">
                Save songs and playlists directly to local storage for listening anywhere without
                mobile data.
              </p>
            </div>
          </div>

          <div className="mobile-apk-card">
            <Radio size={20} className="shrink-0 text-[#b09cff]" />
            <div>
              <h2 className="text-sm font-semibold text-white">Synchronized Lyrics</h2>
              <p className="mt-0.5 text-xs text-[#a59fad]">
                Real-time line-by-line synced lyrics powered by LRCLIB, KuGou, and Spotify
                providers.
              </p>
            </div>
          </div>

          <div className="mobile-apk-card">
            <Sliders size={20} className="shrink-0 text-[#b09cff]" />
            <div>
              <h2 className="text-sm font-semibold text-white">Parametric DSP Equalizer</h2>
              <p className="mt-0.5 text-xs text-[#a59fad]">
                Audiophile audio pipeline with 10-band equalization, equal-power crossfade, and
                stereo widening.
              </p>
            </div>
          </div>

          <div className="mobile-apk-card">
            <ShieldCheck size={20} className="shrink-0 text-[#b09cff]" />
            <div>
              <h2 className="text-sm font-semibold text-white">Ad-Free and Private</h2>
              <p className="mt-0.5 text-xs text-[#a59fad]">
                Open source, zero tracker telemetry, no account requirement, and no audio
                interruptions.
              </p>
            </div>
          </div>
        </section>

        <div className="mobile-apk-footer">
          <button type="button" className="mobile-apk-bypass-btn" onClick={onContinueToWeb}>
            Continue to Web Player
          </button>
        </div>
      </main>
    </div>
  )
}
