import { fileURLToPath, URL } from 'node:url';
import fs from 'node:fs';
import vue from '@vitejs/plugin-vue';
import vueJsx from '@vitejs/plugin-vue-jsx';
import path from 'path';
import { defineConfig } from 'vite';
import vueDevTools from 'vite-plugin-vue-devtools';
import { createHtmlPlugin } from 'vite-plugin-html';

// Production public path
const PUBLIC_PATH = 'creative-stream';

export default defineConfig(({ mode }) => {
  const isDev = mode === 'development';

  return {
    base: mode === 'production' ? `/${PUBLIC_PATH}/` : '/',
    plugins: [
      vue(),
      vueJsx(),
      vueDevTools(),
      createHtmlPlugin({
        inject: {
          data: {
            IAM_URL_PREFIX: isDev ? '' : '__BK_CI_IAM_URL_PREFIX__',
            PUBLIC_PATH_PREFIX: isDev ? '' : '__BK_CI_PUBLIC_PATH__',
            ICON_COOL_PREFIX: isDev ? '' : '',
            // Add more variables here as needed
          },
        },
      }),
    ],
    
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    optimizeDeps: {
      exclude: ['bkui-pipeline'],
    },
    build: {
      outDir: path.resolve(__dirname, `../frontend/${PUBLIC_PATH}`),
      emptyOutDir: true,
      rollupOptions: {
        output: {
          manualChunks: undefined,
        },
      },
    },
  };
});
