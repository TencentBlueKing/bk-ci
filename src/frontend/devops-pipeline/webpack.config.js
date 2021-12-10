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
const path = require('path')
const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin')

const webpack = require('webpack')
const HtmlWebpackPlugin = require('html-webpack-plugin')
const CopyWebpackPlugin = require('copy-webpack-plugin')
const webpackBaseConfig = require('../webpack.base')
module.exports = (env, argv) => {
    const isProd = argv.mode === 'production'
    const envDist = env && env.dist ? env.dist : 'frontend'
    const extUrlPrefix = env && env.name ? `${env.name}-` : ''
    const dist = path.join(__dirname, `../${envDist}/pipeline`)
    const config = webpackBaseConfig({
        env,
        argv,
        entry: {
            pipeline: './src/main.js'
        },
        publicPath: '/pipeline/',
        dist: '/pipeline',
        port: 8006
    })
    config.plugins.pop()
    config.plugins = [
        ...config.plugins,
        new MonacoWebpackPlugin({
            publicPath: '/pipeline/',
            languages: ['bat', 'javascript', 'dockerfile', 'typescript', 'kotlin', 'rust', 'go', 'yaml', 'json', 'python', 'shell', 'powershell', 'markdown', 'lua', 'php']
        }),
        new HtmlWebpackPlugin({
            filename: isProd ? `${dist}/frontend#pipeline#index.html` : `${dist}/index.html`,
            template: 'index.html',
            inject: true,
            VENDOR_LIBS: `/pipeline/main.dll.js?v=${Math.random()}`,
            extUrlPrefix
        }),
        new webpack.DllReferencePlugin({
            context: __dirname,
            manifest: require('./dist/manifest.json')
        }),
        new CopyWebpackPlugin({
            patterns: [{ from: path.join(__dirname, './dist'), to: dist }]
        })
    ]
    config.devServer.historyApiFallback = {
        rewrites: [
            { from: /^\/pipeline/, to: '/pipeline/index.html' }
        ]
    }
    return config
}
