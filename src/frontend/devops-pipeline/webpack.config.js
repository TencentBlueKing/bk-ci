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
const webpack = require('webpack')
const HtmlWebpackPlugin = require('html-webpack-plugin')
const CssExtractPlugin = require('mini-css-extract-plugin')
const VueLoaderPlugin = require('vue-loader/lib/plugin')
const TerserPlugin = require('terser-webpack-plugin')

const resolve = dist => {
    return path.resolve(__dirname, dist)
}

module.exports = (env, argv) => {
    const isProd = argv.mode === 'production'
    const urlPrefix = env && env.name ? `${env.name}.` : ''
    const extUrlPrefix = env && env.name ? `${env.name}-` : ''

    return {
        devtool: isProd ? '#source-map' : '#source-map',
        entry: {
            pipeline: './src/main.js'
        },
        output: {
            filename: '[name].[contentHash].js',
            chunkFilename: 'js/[name].[chunkHash:8].js',
            publicPath: isProd ? '/pipeline/' : '/'
        },
        resolve: {
            extensions: ['.js', '.vue', '.json', '.css', '.scss'],
            alias: {
                'vue': 'vue/dist/vue.esm.js',
                '@': resolve('src')
            }
        },
        module: {
            // for es5 file
            noParse: [
                /\/node_modules\/jquery\/dist\/jquery\.min\.js$/,
                /\/node_modules\/echarts\/dist\/echarts\.min\.js$/
            ],
            rules: [
                {
                    test: /\.vue$/,
                    include: [ resolve('src'), resolve('node_modules/vue-echarts') ],
                    use: ['vue-loader', {
                        loader: 'eslint-loader',
                        options: {
                            fix: true
                        }
                    }]
                },
                {
                    test: /\.js$/,
                    include: [ resolve('src'), resolve('node_modules/vue-echarts') ],
                    use: [{
                        loader: 'babel-loader',
                        query: {
                            cacheDirectory: './webpack_cache/'
                        }
                    }, {
                        loader: 'eslint-loader',
                        options: {
                            fix: true
                        }
                    }]
                },
                {
                    test: /\.(png|jpe?g|gif|svg)(\?.*)?$/,
                    loader: 'url-loader'
                },
                {
                    test: /\.(mp4|webm|ogg|mp3|wav|flac|aac)(\?.*)?$/,
                    loader: 'url-loader'
                },
                {
                    test: /\.(woff2?|eot|ttf|otf)(\?.*)?$/,
                    loader: 'url-loader'
                },
                {
                    test: /\.css$/,
                    loader: [isProd ? CssExtractPlugin.loader : 'style-loader', 'css-loader']
                },
                {
                    test: /.scss$/,
                    use: [isProd ? CssExtractPlugin.loader : 'style-loader', 'css-loader', 'sass-loader']
                }
            ]
        },
        externals: {
            'vue': 'Vue',
            'vue-router': 'VueRouter',
            'vuex': 'Vuex'
        },
        optimization: {
            namedChunks: true,
            minimizer: [
                new TerserPlugin({
                  cache: true,
                  parallel: true,
                  sourceMap: isProd, // Must be set to true if using source-maps in production
                  terserOptions: {
                    output: {
                        comments: false
                    },
                    compress: {
                        drop_console: true
                    }
                  }
                })
            ]
        },
        plugins: [
            new VueLoaderPlugin(),
            new CssExtractPlugin({
                filename: isProd ? 'css/[name].[chunkHash].css' : 'css/[name].css',
                chunkName: isProd ? 'css/[id].[chunkHash].css' : 'css/[id].css'
            }),
            // brace 优化，只提取需要的语法
            new webpack.ContextReplacementPlugin(/brace\/mode$/, /^\.\/(json|python|sh|text|powershell|batchfile)$/),
            // brace 优化，只提取需要的 theme
            new webpack.ContextReplacementPlugin(/brace\/theme$/, /^\.\/(monokai)$/),
            new HtmlWebpackPlugin({
                filename: 'frontend#pipeline#index.html',
                template: 'index.html',
                inject: true,
                VENDOR_LIBS: `${isProd ? '/pipeline' : ''}/main.dll.js?v=${Math.random()}`,
                urlPrefix,
                extUrlPrefix
            }),
            new webpack.DllReferencePlugin({
                context: __dirname,
                manifest: require('./dist/manifest.json')
            })
        ],
        devServer: {
            port: 80,
            contentBase: path.join(__dirname, 'dist'),
            historyApiFallback: true,
            noInfo: false,
            disableHostCheck: true
        }
    }
}
