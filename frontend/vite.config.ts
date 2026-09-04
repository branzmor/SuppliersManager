import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// TODO: proxy /candidates and /suppliers to the backend during local `npm run dev`
// (in Docker Compose the frontend calls the backend service directly via VITE_API_BASE_URL).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
  },
});
