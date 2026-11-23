import { useEffect, useState } from 'react'

export interface ReleaseInfo {
  version: string
  name: string
  apkUrl: string
  githubUrl: string
  sizeMb?: string | null
}

export const FALLBACK_RELEASE: ReleaseInfo = {
  version: 'v1.4.6',
  name: 'Echo Music v1.4.6',
  apkUrl: 'https://github.com/EchoMusicApp/Echo-Music/releases/download/v1.4.6/EchoMusic.apk',
  githubUrl: 'https://github.com/EchoMusicApp/Echo-Music/releases/latest',
  sizeMb: '151 MB',
}

let cachedRelease: ReleaseInfo | null = null

export function useLatestRelease() {
  const [release, setRelease] = useState<ReleaseInfo>(cachedRelease || FALLBACK_RELEASE)

  useEffect(() => {
    if (cachedRelease) return
    let cancelled = false

    async function fetchLatest() {
      try {
        const res = await fetch(
          'https://api.github.com/repos/EchoMusicApp/Echo-Music/releases/latest',
        )
        if (!res.ok) return
        const data = await res.json()
        const apkAsset = data.assets?.find((a: { name?: string }) => a.name?.endsWith('.apk'))
        const resolved: ReleaseInfo = {
          version: data.tag_name || FALLBACK_RELEASE.version,
          name: data.name || `Echo Music ${data.tag_name || ''}`,
          apkUrl:
            apkAsset?.browser_download_url ||
            `https://github.com/EchoMusicApp/Echo-Music/releases/download/${data.tag_name || 'v1.4.6'}/EchoMusic.apk`,
          githubUrl: data.html_url || 'https://github.com/EchoMusicApp/Echo-Music/releases/latest',
          sizeMb: apkAsset?.size ? `${Math.round(apkAsset.size / (1024 * 1024))} MB` : null,
        }
        cachedRelease = resolved
        if (!cancelled) {
          setRelease(resolved)
        }
      } catch {
        // Fallback remains active
      }
    }

    void fetchLatest()
    return () => {
      cancelled = true
    }
  }, [])

  return release
}
