import { fileURLToPath, URL } from 'node:url'
import fs from 'node:fs'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import path from 'path'
import { defineConfig } from 'vite'
import vueDevTools from 'vite-plugin-vue-devtools'
import { createHtmlPlugin } from 'vite-plugin-html'

// Production public path
const PUBLIC_PATH = 'creative-stream'

// bk-permission source root (dist not built; we bundle from source)
const BK_PERM_SRC = path.resolve(__dirname, '../bk-permission/src')

/**
 * Vite plugin: resolves `bk-permission` from source and shims
 * CJS / Vue 2 / webpack-only modules so Vite can bundle them.
 */
function bkPermissionCompat() {
  return {
    name: 'bk-permission-compat',
    enforce: 'pre' as const,

    resolveId(source: string, importer?: string) {
      if (source === 'bk-permission') return '\0bk-permission-entry'

      if (importer?.includes('bk-permission/src/')) {
        const resolved = path.resolve(path.dirname(importer), source)
        if (resolved.includes('utils/vue'))    return '\0bk-perm-vue-shim'
        if (resolved.includes('utils/locale')) return '\0bk-perm-locale-shim'
        if (source.endsWith('.scss'))          return '\0bk-perm-empty'
      }
    },

    load(id: string) {
      if (id === '\0bk-permission-entry') {
        return [
          `export { AuthorityDirectiveV3 } from '${BK_PERM_SRC}/directive/authority-directive.js';`,
          `export { handleNoPermissionV3 } from '${BK_PERM_SRC}/function/permission.js';`,
        ].join('\n')
      }
      if (id === '\0bk-perm-vue-shim') {
        return 'export const version = 3; export * from "vue";'
      }
      if (id === '\0bk-perm-locale-shim') {
        return 'export function loadI18nMessages() {} export function t(key) { return key; } export const localeMixins = {};'
      }
      if (id === '\0bk-perm-empty') return ''
    },
  }
}

// Custom plugin to rename HTML file after build
function renameHtmlPlugin(newFilename: string) {
  return {
    name: 'rename-html',
    closeBundle() {
      const outDir = path.resolve(__dirname, `../frontend/${PUBLIC_PATH}`)
      const oldPath = path.join(outDir, 'index.html')
      const newPath = path.join(outDir, newFilename)
      if (fs.existsSync(oldPath)) {
        fs.renameSync(oldPath, newPath)
      }
    },
  }
}

export default defineConfig(({ mode }) => {
  const isDev = mode === 'development'

  return {
    base: mode === 'production' ? `/${PUBLIC_PATH}/` : '/',
    plugins: [
      bkPermissionCompat(),
      vue(),
      vueJsx(),
      vueDevTools(),
      createHtmlPlugin({
        inject: {
          data: {
            IAM_URL_PREFIX: '__BK_CI_IAM_URL_PREFIX__',
            PUBLIC_PATH_PREFIX: '__BK_CI_PUBLIC_PATH__',
            ICON_COOL_PREFIX: '',
            USER_API_BASE_URL: '__BK_USER_API_BASE_URL__',
            // Add more variables here as needed
          },
        },
      }),
      !isDev && renameHtmlPlugin(`frontend#${PUBLIC_PATH}#index.html`),
    ].filter(Boolean),
    resolve: {
      alias: [
        { find: '@', replacement: fileURLToPath(new URL('./src', import.meta.url)) },
        // 强制依赖中嵌套引用的 bkui-vue 子路径（如 bkui-vue/lib/icon）都解析到当前项目
        // 安装的 bkui-vue@2.1.0-beta.7，避免 Vite 预构建时挑到 pnpm 仓库里其它 workspace
        // 的旧版 bkui-vue@2.0.1，导致 Dialog 渲染出空的 bk-dialog-header
        {
          find: /^bkui-vue(\/.*)?$/,
          replacement: fileURLToPath(new URL('./node_modules/bkui-vue', import.meta.url)) + '$1',
        },
      ],
      dedupe: ['bkui-vue', 'vue'],
    },
    define: {
      global: 'globalThis',
    },
    optimizeDeps: {
      exclude: ['bkui-pipeline'],
    },
    css: {
      preprocessorMaxWorkers: 0,
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
  }
})
