import { defineConfig } from 'vite';
import path from 'path';
import pluginVue from '@vitejs/plugin-vue';
import pluginVueJsx from '@vitejs/plugin-vue-jsx';
import postcssImport from 'postcss-import';
import autoprefixer from 'autoprefixer';

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
    outDir: '../frontend/metrics'
  },
  plugins: [
    pluginVue(),
    pluginVueJsx(),
  ],
  resolve: {
    alias: [
      {
        find: '@',
        replacement: path.resolve('./src'),
      },
    ],
  },
});
