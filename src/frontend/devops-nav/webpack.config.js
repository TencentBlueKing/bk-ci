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
const AddAssetHtmlPlugin = require('add-asset-html-webpack-plugin')
const SpriteLoaderPlugin = require('svg-sprite-loader/plugin')
const ForkTsCheckerWebpackPlugin = require('fork-ts-checker-webpack-plugin')
const CopyWebpackPlugin = require('copy-webpack-plugin')
const path = require('path')
const webpackBaseConfig = require('../webpack.base')
const webpack = require('webpack')
module.exports = (env = {}, argv) => {
    const isDev = argv.mode === 'development'
    const envDist = env && env.dist ? env.dist : 'frontend'
    const lsVersion = env && env.lsVersion ? env.lsVersion : 'v2' // 最后一个命令行参数为localStorage版本
    const dist = path.join(__dirname, `../${envDist}/console`)
    const config = webpackBaseConfig({
        env,
        argv,
        entry: './src/entry',
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
                        transpileOnly: true,
                        appendTsSuffixTo: [/\.vue$/]
                    }
                }
            ]
        }
    ]
    config.plugins.pop()
    config.plugins = [
        ...config.plugins,
        new ForkTsCheckerWebpackPlugin({
            eslint: {
                files: './src/**/*.{ts,tsx,vue,js,jsx}'
            }
        }),
        new HtmlWebpackPlugin({
            template: './src/index.html',
            filename: isDev
                ? 'index.html'
                : `${dist}/frontend#console#index.html`,
            inject: false,
            publicPath: `${isDev ? '' : '__BK_CI_PUBLIC_PATH__'}/console/`,
            templateParameters: {
                PUBLIC_PATH_PREFIX: isDev ? '' : '__BK_CI_PUBLIC_PATH__'
            },
            minify: {
                removeComments: false
            },
            DEVOPS_LS_VERSION: lsVersion
        }),
        new AssetPlugin(),
        new SpriteLoaderPlugin({
            plainSprite: true
        }),
        new AddAssetHtmlPlugin([
            {
                filepath: require.resolve('./src/assets/static/main.dll.js'),
                publicPath: path.posix.join((isDev ? '' : '__BK_CI_PUBLIC_PATH__'), '/console/', 'static/'),
                hash: true,
                includeSourcemap: false
            }
        ]),
        new webpack.DllReferencePlugin({
            context: __dirname,
            manifest: require('./src/assets/static/manifest.json')
        }),
        new CopyWebpackPlugin({
            patterns: [
                {
                    from: path.join(__dirname, './src/assets/static'),
                    to: `${dist}/static`
                }
            ]
        })
    ]
    config.devServer.historyApiFallback = {
        rewrites: [{ from: /^\/console/, to: '/console/index.html' }]
    }
    return config
}
