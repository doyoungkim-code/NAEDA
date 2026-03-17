import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://j14d103.p.ssafy.io:8080',
        changeOrigin: true,
      },
    },
  },
})
