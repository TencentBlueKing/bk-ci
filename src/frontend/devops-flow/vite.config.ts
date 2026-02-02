import { fileURLToPath, URL } from 'node:url';

import vue from '@vitejs/plugin-vue';
import vueJsx from '@vitejs/plugin-vue-jsx';
import path from 'path';
import { defineConfig } from 'vite';
import vueDevTools from 'vite-plugin-vue-devtools';

// Production public path
const PUBLIC_PATH = 'creative-stream';

export default defineConfig(({ mode }) => ({
  base: mode === 'production' ? `/${PUBLIC_PATH}/` : '/',
  plugins: [vue(), vueJsx(), vueDevTools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: 'local.devops.woa.com',
    https: {
      cert: './local.devops.woa.com+3.pem',
      key: './local.devops.woa.com+3-key.pem',
    }, // 启用 HTTPS，证书由 basicSsl 插件自动生成
    proxy: {
      '/ms': {
        target: 'https://dev.devops.woa.com',
        changeOrigin: true,
      },
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
}));
