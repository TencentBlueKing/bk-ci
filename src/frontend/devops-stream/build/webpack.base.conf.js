/**
 * @file webpack base conf
 * @author Blueking
 */

import { VueLoaderPlugin } from 'vue-loader'
import webpack from 'webpack'
import config from './config'
import { assetsPath, resolve } from './util'
const ESLintPlugin = require('eslint-webpack-plugin')
const isProd = process.env.NODE_ENV === 'production'

export default {
    output: {
        path: isProd ? config.build.assetsRoot : config.dev.assetsRoot,
        filename: '[name].js',
        publicPath: isProd ? config.build.assetsPublicPath : config.dev.assetsPublicPath,
        globalObject: 'this'
    },

    resolve: {
        extensions: ['.js', '.vue', '.json'],
        fallback: {
            path: false
        },
        alias: {
            vue$: 'vue/dist/vue.esm.js',
            '@': resolve('src'),
            '@doc': resolve('doc')
        }
    },

    module: {
        noParse: [
            /\/node_modules\/jquery\/dist\/jquery\.min\.js$/,
            /\/node_modules\/echarts\/dist\/echarts\.min\.js$/
        ],
        rules: [
            {
                test: /\.vue$/,
                include: [resolve('src'), /node_modules\/vue-echarts/],
                use: {
                    loader: 'vue-loader',
                    options: {
                        transformAssetUrls: {
                            video: 'src',
                            source: 'src',
                            img: 'src',
                            image: 'xlink:href'
                        }
                    }
                }
            },
            {
                test: /\.js$/,
                include: [resolve('src')],
                use: {
                    loader: 'babel-loader',
                    options: {
                        // include: [resolve('src'), resolve('node_modules/@tencent/bk-magic-vue')],
                        cacheDirectory: './webpack_cache/',
                        // 确保 JS 的转译应用到 node_modules 的 Vue 单文件组件
                        exclude: file => (
                            /node_modules/.test(file) && !/\.vue\.js/.test(file)
                        )
                    }
                }
            },
            {
                test: /\.(png|jpe?g|gif|svg)(\?.*)?$/,
                loader: 'url-loader',
                options: {
                    limit: 10000,
                    name: assetsPath('images/[name].[hash:7].[ext]')
                }
            },
            {
                test: /\.(mp4|webm|ogg|mp3|wav|flac|aac)(\?.*)?$/,
                use: {
                    loader: 'url-loader',
                    options: {
                        limit: 10000,
                        name: assetsPath('media/[name].[hash:7].[ext]')
                    }
                }
            },
            {
                test: /\.(woff2?|eot|ttf|otf)(\?.*)?$/,
                use: {
                    loader: 'url-loader',
                    options: {
                        limit: 10000,
                        name: assetsPath('fonts/[name].[hash:7].[ext]')
                    }
                }
            }
        ]
    },

    plugins: [
        new VueLoaderPlugin(),
        // moment 优化，只提取本地包
        new webpack.ContextReplacementPlugin(/moment\/locale$/, /zh-cn/),
        new ESLintPlugin({
            context: resolve(__dirname, '..', 'src'),
            extensions: ['js', 'ts', 'vue', 'jsx', 'tsx'],
            lintDirtyModulesOnly: true
        })
    ]
}
