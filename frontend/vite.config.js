import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// In dev, /api calls are proxied to the Spring Boot backend (no CORS headaches).
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true }
    }
  }
})
