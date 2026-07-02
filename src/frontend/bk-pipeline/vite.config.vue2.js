import { defineConfig } from 'vite'
import vue2 from '@vitejs/plugin-vue2'
import path from 'path'
import fs from 'fs'

// Custom plugin: copy type declaration file to dist/
function copyDtsPlugin () {
    return {
        name: 'copy-dts',
        closeBundle () {
            const src = path.resolve(__dirname, 'index.d.ts')
            const dest = path.resolve(__dirname, 'dist/index.d.ts')
            if (fs.existsSync(src)) {
                fs.copyFileSync(src, dest)
                console.log('✓ Type declaration file copied to dist/')
            }
        }
    }
}

// Vite config for Vue 2.7 build
export default defineConfig({
    plugins: [
        vue2(),
        copyDtsPlugin()
    ],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, 'src')
        },
        extensions: ['.js', '.vue', '.json', '.ts', '.scss', '.css']
    },
    optimizeDeps: {
        // Pre-bundle deps for better DX in dev (if we ever dev the Vue 2 build)
        include: ['vue', 'uuid', 'vue-draggable-plus'],
        // Exclude optional UI libs so they stay external
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
            entry: path.resolve(__dirname, 'index.js'),
            name: 'bkPipeline',
            fileName: (format) => {
                if (format === 'es') return 'bk-pipeline.esm.js'
                if (format === 'cjs') return 'bk-pipeline.cjs.js'
                return 'bk-pipeline.min.js'
            },
            formats: ['es', 'cjs', 'umd']
        },
        rollupOptions: {
            // Keep these deps external so they are provided by the host app
            external: ['vue', 'bk-magic-vue', 'bkui-vue', 'vue-draggable-plus'],
            output: {
                globals: {
                    vue: 'Vue',
                    'bk-magic-vue': 'bkMagic',
                    'bkui-vue': 'bkuiVue',
                    'vue-draggable-plus': 'VueDraggablePlus'
                },
                assetFileNames: (assetInfo) => {
                    // Keep CSS file name stable inside vue2 subdir
                    if (assetInfo.name === 'style.css') return 'bk-pipeline.css'
                    return assetInfo.name
                }
            }
        },
        // Use terser for minification
        minify: 'terser',
        terserOptions: {
            format: {
                comments: false
            }
        },
        // Output Vue 2 build into a dedicated sub directory
        outDir: 'dist/vue2',
        // Do not wipe the whole dist when building vue2; only vue2 subdir matters
        emptyOutDir: false,
        sourcemap: false
    },
    // Dev server config (mainly for local debugging if needed)
    server: {
        port: 3001,
        open: false
    }
})
