import { defineConfig, type Plugin, type ViteDevServer, type PreviewServer } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'
import { getRequestListener } from '@hono/node-server'

function apiDevServer(): Plugin {
  const attachApi = async (server: ViteDevServer | PreviewServer) => {
    const { app } = await import('./server/index.ts')
    const listener = getRequestListener(app.fetch)
    server.middlewares.use((req, res, next) => {
      if (req.url && req.url.startsWith('/api')) {
        listener(req, res)
      } else {
        next()
      }
    })
  }

  return {
    name: 'api-dev-server',
    configureServer: attachApi,
    configurePreviewServer: attachApi,
  }
}

export default defineConfig({
  plugins: [
    apiDevServer(),
    react(),
    tailwindcss(),
    VitePWA({
      registerType: 'autoUpdate',
      injectRegister: false,
      includeAssets: [
        'favicon.svg',
        'icons.svg',
        'pwa-icon.svg',
        'Echo-new.png',
        'banner.png',
        'echo-logo.svg',
      ],
      manifest: {
        name: 'Echo Music',
        short_name: 'Echo Music',
        description:
          'A modern music player with ad-free streaming, synced lyrics, and an intuitive user experience.',
        theme_color: '#0b0a0f',
        background_color: '#0b0a0f',
        display: 'standalone',
        start_url: '/',
        scope: '/',
        icons: [
          {
            src: '/pwa-icon.svg',
            sizes: 'any',
            type: 'image/svg+xml',
            purpose: 'any maskable',
          },
          {
            src: '/Echo-new.png',
            sizes: '1080x1080',
            type: 'image/png',
            purpose: 'any maskable',
          },
        ],
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,svg,webmanifest}'],
        navigateFallback: 'index.html',
        cleanupOutdatedCaches: true,
        runtimeCaching: [
          {
            urlPattern: /^https:\/\/(i\.ytimg\.com|lh3\.googleusercontent\.com)\/.*/i,
            handler: 'CacheFirst',
            options: {
              cacheName: 'artwork-images',
              expiration: {
                maxEntries: 500,
                maxAgeSeconds: 30 * 24 * 60 * 60, // 30 days
              },
              cacheableResponse: {
                statuses: [0, 200],
              },
            },
          },
        ],
      },
    }),
  ],
  server: {
    port: 5173,
  },
})
