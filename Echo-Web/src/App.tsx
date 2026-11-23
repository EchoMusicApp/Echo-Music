import { lazy, useEffect, useState } from 'react'
import { useIsMobileDevice } from './hooks/useIsMobileDevice'
import { MobileApkLanding } from './components/MobileApkLanding'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './components/Layout'
import { bridgeAudioEngineWithStore } from './hooks/useAudioEngine'
import { useEchoStore } from './stores/useEchoStore'

const HomePage = lazy(() =>
  import('./pages/BrowsePages').then((module) => ({ default: module.HomePage })),
)
const DiscoverPage = lazy(() =>
  import('./pages/BrowsePages').then((module) => ({ default: module.DiscoverPage })),
)
const SearchPage = lazy(() =>
  import('./pages/BrowsePages').then((module) => ({ default: module.SearchPage })),
)
const LibraryPage = lazy(() =>
  import('./pages/BrowsePages').then((module) => ({ default: module.LibraryPage })),
)
const PlaylistPage = lazy(() =>
  import('./pages/DetailPages').then((module) => ({ default: module.PlaylistPage })),
)
const AlbumPage = lazy(() =>
  import('./pages/DetailPages').then((module) => ({ default: module.AlbumPage })),
)
const ArtistPage = lazy(() =>
  import('./pages/DetailPages').then((module) => ({ default: module.ArtistPage })),
)
const LikedPage = lazy(() =>
  import('./pages/CollectionPages').then((module) => ({ default: module.LikedPage })),
)
const RecentlyPlayedPage = lazy(() =>
  import('./pages/CollectionPages').then((module) => ({ default: module.RecentlyPlayedPage })),
)
const QueuePage = lazy(() =>
  import('./pages/CollectionPages').then((module) => ({ default: module.QueuePage })),
)
const SettingsPage = lazy(() =>
  import('./pages/SettingsPage').then((module) => ({ default: module.SettingsPage })),
)

let hasHandledInitialStartup = typeof window !== 'undefined' && window.location.pathname !== '/'

function HomeRoute() {
  const defaultTab = useEchoStore((state) => state.preferences.defaultTab) || 'home'
  const [shouldRedirect] = useState(() => {
    if (!hasHandledInitialStartup) {
      hasHandledInitialStartup = true
      return defaultTab !== 'home'
    }
    return false
  })

  if (shouldRedirect) {
    return <Navigate to={`/${defaultTab}`} replace />
  }

  return <HomePage />
}

function AppRoutes() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<HomeRoute />} />
        <Route path="discover" element={<DiscoverPage />} />
        <Route path="search" element={<SearchPage />} />
        <Route path="library" element={<LibraryPage />} />
        <Route path="liked" element={<LikedPage />} />
        <Route path="recently-played" element={<RecentlyPlayedPage />} />
        <Route path="queue" element={<QueuePage />} />
        <Route path="settings" element={<SettingsPage />} />
        <Route path="playlists/:playlistId" element={<PlaylistPage />} />
        <Route path="albums/:albumId" element={<AlbumPage />} />
        <Route path="artists/:artistId" element={<ArtistPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}

export default function App() {
  const isMobile = useIsMobileDevice()
  const [showWeb, setShowWeb] = useState(false)

  useEffect(() => {
    document.title = 'Echo Music'
    const cleanup = bridgeAudioEngineWithStore()
    return () => {
      cleanup()
    }
  }, [])

  // If on a mobile device and the user hasn't chosen to continue to the web player, show the APK landing page.
  if (isMobile && !showWeb) {
    return <MobileApkLanding onContinueToWeb={() => setShowWeb(true)} />
  }

  // Otherwise, render the full web app.
  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  )
}
