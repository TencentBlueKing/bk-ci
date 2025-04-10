import { defineConfig } from 'vite'
import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx';
import postcssImport from 'postcss-import';
import autoprefixer from 'autoprefixer';
import tailwindcss from 'tailwindcss';

export default defineConfig({
  base: '/platform',
  css: {
    postcss: {
      plugins: [
        postcssImport,
        autoprefixer,
        tailwindcss
      ],
    },
  },
  server: {
    port: 8010
  },
  plugins: [vue(), vueJsx()],
  build: {
    outDir: '../frontend/platform'
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
