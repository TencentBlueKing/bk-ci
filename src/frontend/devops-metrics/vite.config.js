import pluginVue from '@vitejs/plugin-vue';
import pluginVueJsx from '@vitejs/plugin-vue-jsx';
import autoprefixer from 'autoprefixer';
import path from 'path';
import postcssImport from 'postcss-import';
import { defineConfig } from 'vite';

/**
 * @param newFilename {string}
 * @returns {import('vite').Plugin}
 */
const renameIndexPlugin = (newFilename) => {
  if (!newFilename) return

  return {
    name: 'renameIndex',
    enforce: 'post',
    generateBundle(options, bundle) {
      const indexHtml = bundle['index.html']
      indexHtml.fileName = newFilename
    },
  }
}

module.exports = defineConfig({
  base: '/metrics',
  css: {
    postcss: {
      plugins: [
        postcssImport,
        autoprefixer,
      ],
    },
  },
  
  server: {
    https: true,
    port: 3000,
  },
  build: {
    outDir: '../frontend/metrics',
    emptyOutDir: true,
  },
  plugins: [
    pluginVue(),
    pluginVueJsx(),
    renameIndexPlugin('frontend#metrics#index.html')
  ],
  resolve: {
    alias: [
      {
        find: '@',
        replacement: path.resolve('./src'),
      },
    ],
  },
  experimental: {
    renderBuiltUrl(filename, { hostType }) {
      if (hostType === 'js') {
        return { runtime: `window.PUBLIC_URL_PREFIX + ${JSON.stringify(`/metrics/${filename}`)}` }
      } else {
        return `__BK_CI_PUBLIC_PATH__/metrics/${filename}`
      }
    }
  },
});
