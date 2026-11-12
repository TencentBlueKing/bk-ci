import { fileURLToPath } from 'node:url'
import { configDefaults, defineConfig, mergeConfig } from 'vitest/config'
import viteConfig from './vite.config'

export default defineConfig((env) =>
  mergeConfig(typeof viteConfig === 'function' ? viteConfig(env) : viteConfig, {
    test: {
      environment: 'jsdom',
      // bkui-vue publishes a module entry but no Node main/exports entry.
      alias: {
        'bkui-vue': fileURLToPath(new URL('./node_modules/bkui-vue/lib/index.js', import.meta.url)),
      },
      exclude: [...configDefaults.exclude, 'e2e/**'],
      root: fileURLToPath(new URL('./', import.meta.url)),
    },
  }),
)
