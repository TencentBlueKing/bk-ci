const { defineConfig } = require('@vue/cli-service')
const fs = require('fs')
const path = require('path')

const chatXRoot = path.dirname(require.resolve('@blueking/chat-x/dist/index.js'))
const chatXMonorepo = path.resolve(chatXRoot, '../../..')

const isLibBuild = process.env.BUILD_TARGET === 'lib'

const sharedConfig = {
  transpileDependencies: true,
  chainWebpack: config => {
    config.module.rule('js').test(/\.[jt]sx?$/)
    config.resolve.extensions.prepend('.tsx').prepend('.ts')
  },
  configureWebpack: {
    resolve: {
      alias: {
        vue$: path.resolve(__dirname, 'node_modules/vue'),
        'bkui-vue': path.resolve(__dirname, 'node_modules/bkui-vue'),
      },
      modules: [
        path.resolve(__dirname, 'node_modules'),
        path.resolve(chatXMonorepo, 'node_modules'),
        'node_modules',
      ],
    },
  },
}

const appConfig = {
  ...sharedConfig,
  publicPath: '/ai/',
  outputDir: path.resolve(__dirname, '../frontend/ai'),
  pages: {
    index: {
      entry: 'src/main.ts',
    },
  },
  // devServer: {
  //   port: 8081,
  //   host: 'local-ai.devops.woa.com',
  //   https: {
  //     key: fs.readFileSync(path.resolve(__dirname, 'local-ai.devops.woa.com.key')),
  //     cert: fs.readFileSync(path.resolve(__dirname, 'local-ai.devops.woa.com.crt')),
  //   },
  //   allowedHosts: ['local-ai.devops.woa.com'],
  //   compress: false,
  //   proxy: {
  //     '/ms/': {
  //       target: 'https://dev.devops.woa.com',
  //       changeOrigin: true,
  //       secure: false,
  //     },
  //   },
  // },
}

const libConfig = {
  ...sharedConfig,
  css: {
    extract: true,
  },
}

module.exports = defineConfig(isLibBuild ? libConfig : appConfig)
