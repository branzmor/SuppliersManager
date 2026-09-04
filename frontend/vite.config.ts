/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In Docker Compose the frontend calls the backend service directly via VITE_API_BASE_URL, so
// this proxy only matters for local `npm run dev` against a backend running on localhost:8080.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/candidates': 'http://localhost:8080',
      '/suppliers': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
  },
});
