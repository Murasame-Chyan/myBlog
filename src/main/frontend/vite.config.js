import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import { fileURLToPath } from 'url'

const __dirname = fileURLToPath(new URL('.', import.meta.url))

const pages = ['index', 'readBlog', 'writeBlog', 'profile', 'archives', 'readArchive', 'followList', 'error']

function buildInput() {
  const input = {}
  for (const page of pages) {
    input[page] = resolve(__dirname, `src/pages/${page}/index.html`)
  }
  return input
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': resolve(__dirname, 'src') }
  },
  build: {
    outDir: resolve(__dirname, '../resources/static/dist'),
    emptyOutDir: true,
    rollupOptions: {
      input: buildInput()
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/auth': 'http://localhost:8080',
      '/api': 'http://localhost:8080',
      '/blogs': 'http://localhost:8080',
      '/user': 'http://localhost:8080',
      '/archives': 'http://localhost:8080',
      '/images': 'http://localhost:8080',
      '/pics': 'http://localhost:8080',
      '/css': 'http://localhost:8080',
      '/js': 'http://localhost:8080'
    }
  }
})
