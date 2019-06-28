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
const MiniCssExtractPlugin = require('mini-css-extract-plugin')
const AssetPlugin = require('./webpack/assets-webpack-plugin')
const AddAssetHtmlPlugin = require('add-asset-html-webpack-plugin')
const SpriteLoaderPlugin = require('svg-sprite-loader/plugin')
const CopyWebpackPlugin = require('copy-webpack-plugin')
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin
const TerserPlugin = require('terser-webpack-plugin')
const VueLoaderPlugin = require('vue-loader/lib/plugin')
const webpack = require('webpack')
const path = require('path')

module.exports = (env, argv) => {
    const isDev = argv.mode === 'development'
    const nodeEnv = process.env.NODE_ENV

    return {
        entry: './src/index.ts',
        output: {
            publicPath: '/console/',
            filename: 'js/[name].[chunkhash].js',
            chunkFilename: 'js/[name].[chunkhash].js'
        },
        devtool: 'source-map',
        module: {
            rules: [
                {
                    test: /\.vue$/,
                    include: path.resolve('src'),
                    use: ['vue-loader', {
                        loader: 'eslint-loader',
                        options: {
                            fix: true
                        }
                    }]
                },
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
                },
                {
                    test: /\.js$/,
                    include: path.resolve('src'),
                    loader: 'babel-loader'
                },
                {
                    test: /.scss$/,
                    use: [isDev ? 'style-loader' : MiniCssExtractPlugin.loader, 'css-loader', 'sass-loader']
                },
                {
                    test: /\.(png|jpg|gif|svg)$/,
                    loader: 'url-loader',
                    options: {
                        limit: 10000,
                        name: '[name].[ext]?[hash]'
                    }
                },
                {
                    test: /\.(woff2?|eot|ttf|otf)(\?.*)?$/,
                    loader: 'url-loader',
                    options: {
                        limit: 10000
                    }
                }
            ]
        },
        plugins: [
            new VueLoaderPlugin(),
            new MiniCssExtractPlugin({
                filename: 'css/[name].[chunkhash].css',
                chunkName: 'css/[id].[chunkhash].css'
            }),
            new HtmlWebpackPlugin({
                template: './src/index.html',
                filename: 'frontend#console#index.html',
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
            // new BundleAnalyzerPlugin(),
            new webpack.HashedModuleIdsPlugin(),
            new CopyWebpackPlugin([{ from: path.join(__dirname, './src/assets/static'), to: './static' }])
        ],
        optimization: {
            namedChunks: true,
            minimizer: [
                new TerserPlugin({
                  cache: true,
                  parallel: true,
                  sourceMap: !isDev, // Must be set to true if using source-maps in production
                  terserOptions: {
                    output: {
                        comments: false
                    },
                    compress: {
                        drop_console: true
                    }
                  }
                })
            ],
            splitChunks: {
                cacheGroups: {
                    vendors: {
                        test: /node_modules/,
                        name: 'vendors',
                        chunks: 'all'
                    }
                }
            }
        },
        resolve: {
            extensions: ['.ts', '.js', '.vue', '.json', '.scss', '.css'],
            alias: {
                '@': path.resolve('src'),
                'vue$': 'vue/dist/vue.esm.js'
            }
        },
        externals: {
            'vue': 'Vue',
            'vuex': 'Vuex',
            'vue-router': 'VueRouter'
        },
        devServer: {
            contentBase: path.join(__dirname, 'dist'),
            historyApiFallback: {
                rewrites: [
                    { from: /^\/console/, to: '/console/index.html' }
                ]
            },
            noInfo: false,
            disableHostCheck: true
        },
        performance: {
            hints: false
        }
    }
}
