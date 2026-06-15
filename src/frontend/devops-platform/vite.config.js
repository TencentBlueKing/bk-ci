import { defineConfig, loadEnv } from 'vite'
import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx';
import postcssImport from 'postcss-import';
import autoprefixer from 'autoprefixer';
import tailwindcss from 'tailwindcss';

// 构建产物后处理：
// 1. vite 会对 base 强制补前导斜杠（如 /__BK_CI_PUBLIC_PATH__/platform/），占位符替换为空时
//    会变成 //platform/ 的错误路径，这里去掉多余的前导斜杠，与 webpack 版本的 publicPath 保持一致。
// 2. 将入口 index.html 重命名为网关模板文件名（frontend#platform#index.html），
//    部署时才会被收集为模板并替换 __BK_CI_PUBLIC_PATH__。
function bkBuildAdjust (htmlName) {
  const fixPublicPath = (code) => code.split('/__BK_CI_PUBLIC_PATH__/').join('__BK_CI_PUBLIC_PATH__/');
  return {
    name: 'bk-ci-build-adjust',
    apply: 'build',
    enforce: 'post',
    generateBundle (_options, bundle) {
      Object.values(bundle).forEach((file) => {
        if (file.type === 'chunk') {
          file.code = fixPublicPath(file.code);
        } else if (typeof file.source === 'string') {
          file.source = fixPublicPath(file.source);
        }
      });
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
    base: env.VITE_PUBLIC_PATH || '/platform/',
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
    plugins: [vue(), vueJsx(), bkBuildAdjust(env.VITE_HTML_NAME)],
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
