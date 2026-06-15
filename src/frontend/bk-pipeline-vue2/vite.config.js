import { defineConfig } from 'vite'
import vue2 from '@vitejs/plugin-vue2'
import path from 'path'
import fs from 'fs'

// Paths pointing back to the main bk-pipeline package
const PIPELINE_ROOT = path.resolve(__dirname, '../bk-pipeline')
const SRC_ROOT = path.resolve(PIPELINE_ROOT, 'src')
const ENTRY_FILE = path.resolve(PIPELINE_ROOT, 'index.js')
const DTS_FILE = path.resolve(PIPELINE_ROOT, 'index.d.ts')
const OUT_DIR = path.resolve(PIPELINE_ROOT, 'dist/vue2')

// Copy type declaration file from main package to vue2 dist
function copyDtsPlugin () {
    return {
        name: 'copy-dts',
        closeBundle () {
            if (!fs.existsSync(DTS_FILE)) {
                console.warn('[bk-pipeline-vue2] index.d.ts not found in main package')
                return
            }
            fs.mkdirSync(OUT_DIR, { recursive: true })
            const dest = path.resolve(OUT_DIR, 'index.d.ts')
            fs.copyFileSync(DTS_FILE, dest)
            console.log('[bk-pipeline-vue2] Type declaration file copied to', dest)
        }
    }
}

export default defineConfig({
    plugins: [
        vue2(),
        copyDtsPlugin()
    ],
    resolve: {
        alias: {
            // Use bk-pipeline's src as the single source of truth
            '@': SRC_ROOT
        },
        extensions: ['.js', '.vue', '.json', '.ts', '.scss', '.css']
    },
    optimizeDeps: {
        include: ['vue', 'uuid', 'vue-draggable-plus'],
        exclude: ['bk-magic-vue', 'bkui-vue']
    },
    css: {
        preprocessorOptions: {
            scss: {
                additionalData: ''
            }
        }
    },
    build: {
        lib: {
            entry: ENTRY_FILE,
            name: 'bkPipeline',
            fileName: (format) => {
                if (format === 'es') return 'bk-pipeline.esm.js'
                if (format === 'cjs') return 'bk-pipeline.cjs.js'
                return 'bk-pipeline.min.js'
            },
            formats: ['es', 'cjs', 'umd']
        },
        rollupOptions: {
            external: ['vue', 'bk-magic-vue', 'bkui-vue', 'vue-draggable-plus'],
            output: {
                globals: {
                    vue: 'Vue',
                    'bk-magic-vue': 'bkMagic',
                    'bkui-vue': 'bkuiVue',
                    'vue-draggable-plus': 'VueDraggablePlus'
                },
                assetFileNames: (assetInfo) => {
                    if (assetInfo.name === 'style.css') return 'bk-pipeline.css'
                    return assetInfo.name
                }
            }
        },
        minify: 'terser',
        terserOptions: {
            format: {
                comments: false
            }
        },
        outDir: OUT_DIR,
        emptyOutDir: false,
        sourcemap: false
    },
    server: {
        port: 4300,
        open: false
    }
})
