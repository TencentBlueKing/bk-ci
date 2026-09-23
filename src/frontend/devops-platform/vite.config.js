import { defineConfig, loadEnv } from 'vite'
import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx';
import postcssImport from 'postcss-import';
import autoprefixer from 'autoprefixer';
import tailwindcss from 'tailwindcss';

// 应用部署的子目录名
const MODULE = 'platform';

// 将入口 index.html 重命名为网关模板文件名（frontend#platform#index.html），
// 部署时才会被收集为模板并替换 __BK_CI_PUBLIC_PATH__。
function renameHtml (htmlName) {
  return {
    name: 'bk-ci-rename-html',
    apply: 'build',
    enforce: 'post',
    generateBundle (_options, bundle) {
      const html = bundle['index.html'];
      if (htmlName && html) {
        delete bundle['index.html'];
        html.fileName = htmlName;
        bundle[htmlName] = html;
      }
    },
  };
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  return {
    base: `/${MODULE}/`,
    // 运行时计算资源地址，等价于 webpack 的 __webpack_public_path__：
    // 构建期不写死前缀，async chunk / 资源在运行时根据 window.PUBLIC_URL_PREFIX 拼接，
    // 因此无需部署脚本替换 JS 内的占位符。
    experimental: {
      renderBuiltUrl (filename, { hostType }) {
        if (hostType === 'js') {
          return { runtime: `window.__getPublicAssetUrl(${JSON.stringify(filename)})` };
        }
        // html / css 无法执行 JS，使用占位符；其中 html 会被网关 render_tpl 替换
        return `__BK_CI_PUBLIC_PATH__/${MODULE}/${filename}`;
      },
    },
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
    plugins: [vue(), vueJsx(), renameHtml(env.VITE_HTML_NAME)],
    build: {
      outDir: '../frontend/platform'
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
  };
})
