/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

const HtmlWebpackPlugin = require('html-webpack-plugin')
const AssetPlugin = require('../webpackPlugin/assets-webpack-plugin')
const ReplacePlugin = require('../webpackPlugin/replace-webpack-plugin')
const AddAssetHtmlPlugin = require('add-asset-html-webpack-plugin')
const SpriteLoaderPlugin = require('svg-sprite-loader/plugin')
const CopyWebpackPlugin = require('copy-webpack-plugin')
const path = require('path')
const webpackBaseConfig = require('../webpack.base')
const webpack = require('webpack')

module.exports = (env = {}, argv) => {
  const isDev = argv.mode === 'development'
  const urlPrefix = env && env.name ? env.name : ''
  const envDist = env && env.dist ? env.dist : 'frontend'
  const dist = path.join(__dirname, `../${envDist}/console`)
  const config = webpackBaseConfig({
    env,
    argv,
    entry: './src/index',
    publicPath: '/console/',
    dist: '/console',
    port: 8080
  })
  config.module.rules = [
    ...config.module.rules,
    {
      test: /\.tsx?$/,
      include: path.resolve('src'),
      use: [
        {
          loader: 'babel-loader'
        },
        {
          loader: 'ts-loader',
          options: {
            appendTsSuffixTo: [/\.vue$/]
          }
        }
      ]
    }
  ]
  config.plugins = [
    ...config.plugins,
    new HtmlWebpackPlugin({
      template: './src/index.html',
      filename: isDev ? 'index.html' : `${dist}/frontend#console#index.html`,
      urlPrefix,
      inject: false
    }),
    new AssetPlugin(),
    new SpriteLoaderPlugin({
      plainSprite: true
    }),
    new AddAssetHtmlPlugin([
      {
        filepath: require.resolve('./src/assets/static/main.dll.js'),
        publicPath: path.posix.join('/console/', 'static/'),
        hash: true,
        includeSourcemap: false
      }
    ]),
    new webpack.DllReferencePlugin({
      context: __dirname,
      manifest: require('./src/assets/static/manifest.json')
    }),
    new CopyWebpackPlugin([{ from: path.join(__dirname, './src/assets/static'), to: `${dist}/static` }]),
    ...(isDev ? [new ReplacePlugin({
      '__HTTP_SCHEMA__://__BKCI_FQDN__/ms': `${urlPrefix}/ms`,
      '__HTTP_SCHEMA__://__BKCI_FQDN__': ''
    })] : [])
  ]
  return config
}
