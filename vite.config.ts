import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import fs from 'fs';
import path from 'path';

export default defineConfig({
  plugins: [
    react(),
    {
      name: 'serve-apk-folder',
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          if (req.url && req.url.startsWith('/apk/')) {
            const filePath = path.join(__dirname, req.url.split('?')[0]);
            if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
              res.setHeader('Content-Type', 'application/vnd.android.package-archive');
              res.setHeader('Content-Disposition', `attachment; filename="${path.basename(filePath)}"`);
              res.setHeader('Content-Length', fs.statSync(filePath).size);
              const stream = fs.createReadStream(filePath);
              stream.pipe(res);
              return;
            }
          }
          next();
        });
      },
      configurePreviewServer(server) {
        server.middlewares.use((req, res, next) => {
          if (req.url && req.url.startsWith('/apk/')) {
            const filePath = path.join(__dirname, req.url.split('?')[0]);
            if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
              res.setHeader('Content-Type', 'application/vnd.android.package-archive');
              res.setHeader('Content-Disposition', `attachment; filename="${path.basename(filePath)}"`);
              res.setHeader('Content-Length', fs.statSync(filePath).size);
              const stream = fs.createReadStream(filePath);
              stream.pipe(res);
              return;
            }
          }
          next();
        });
      }
    }
  ],
  server: {
    host: '0.0.0.0',
    port: 3000,
    strictPort: true,
  },
  preview: {
    host: '0.0.0.0',
    port: 3000,
  }
});
