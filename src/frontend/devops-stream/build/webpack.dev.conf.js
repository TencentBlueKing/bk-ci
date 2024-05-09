/**
 * @file webpack build conf
 * @author Blueking
 */

import CopyWebpackPlugin from 'copy-webpack-plugin'
import HtmlWebpackPlugin from 'html-webpack-plugin'
import MiniCssExtractPlugin from 'mini-css-extract-plugin'
import OptimizeCSSPlugin from 'optimize-css-assets-webpack-plugin'
import { join, resolve } from 'path'
import TerserPlugin from 'terser-webpack-plugin'
import webpack from 'webpack'
import merge from 'webpack-merge'
 
import config from './config'
import ReplaceCssStaticUrl from './replace-css-static-url-plugin'
import { assetsPath } from './util'
import baseConf from './webpack.base.conf'
 
const devConf = merge(baseConf, {
    mode: 'development',
    entry: {
        main: './src/main.js'
    },
    output: {
        filename: assetsPath('js/[name].js'),
        chunkFilename: assetsPath('js/[name].js')
    },
    optimization: {
        minimizer: [
            new TerserPlugin({
                terserOptions: {
                    compress: false,
                    mangle: true,
                    output: {
                        comments: false
                    }
                },
                cache: true,
                parallel: true,
                sourceMap: true
            }),
            new OptimizeCSSPlugin({
                cssProcessorOptions: {
                    safe: true
                }
            })
        ],
        splitChunks: {
            chunks: 'all',
            // 表示提取出来的文件在压缩前的最小大小，默认为 30kb
            minSize: 30000,
            // 表示被引用次数，默认为 1
            minChunks: 1,
            // 最多有 5 个异步加载请求该 module
            maxAsyncRequests: 5,
            // 初始化的时候最多有 3 个请求该 module
            maxInitialRequests: 3,
            // 名字中间的间隔符
            automaticNameDelimiter: '~',
            // chunk 的名字，如果设成 true，会根据被提取的 chunk 自动生成
            cacheGroups: {
                // 提取 chunk-bk-magic-vue 代码块
                bkMagic: {
                    chunks: 'all',
                    // 单独将 bkMagic 拆包
                    name: 'chunk-bk-magic-vue',
                    // 权重
                    priority: 5,
                    // 表示是否使用已有的 chunk，如果为 true 则表示如果当前的 chunk 包含的模块已经被提取出去了，那么将不会重新生成新的。
                    reuseExistingChunk: true,
                    // test: /[\/]node_modules[\/]\@tencent[\/]bk-magic-vue[\/]/
                    test: module => {
                        return /bk-magic-vue/.test(module.context)
                    }
                },
                // 所有 node_modules 的模块被不同的 chunk 引入超过 1 次的提取为 twice
                // 如果去掉 test 那么提取的就是所有模块被不同的 chunk 引入超过 1 次的
                twice: {
                    // test: /[\\/]node_modules[\\/]/,
                    chunks: 'all',
                    name: 'twice',
                    priority: 6,
                    minChunks: 2
                },
                // default 和 vendors 是默认缓存组，可通过 optimization.splitChunks.cacheGroups.default: false 来禁用
                default: {
                    minChunks: 2,
                    priority: -20,
                    reuseExistingChunk: true
                },
                vendors: {
                    test: /[\\/]node_modules[\\/]/,
                    priority: -10
                }
            }
        }
    },
    module: {
        rules: [
            {
                test: /\.(css|postcss)?$/,
                use: [
                    MiniCssExtractPlugin.loader,
                    {
                        loader: 'css-loader',
                        options: {
                            importLoaders: 1
                        }
                    },
                    {
                        loader: 'postcss-loader',
                        options: {
                            config: {
                                path: resolve(__dirname, '..', 'postcss.config.js')
                            }
                        }
                    }
                ]
            }
        ]
    },
    plugins: [
        new webpack.DefinePlugin(config.dev.env),
 
        new HtmlWebpackPlugin({
            filename: 'index.html',
            template: join(__dirname, '..', '/index.html'),
            inject: true,
            minify: {
                removeComments: true,
                collapseWhitespace: true,
                removeAttributeQuotes: true
            },
            sourceMap: true,
            chunksSortMode: 'none'
        }),
 
        new MiniCssExtractPlugin({
            filename: assetsPath('css/[name].css')
        }),
 
        new ReplaceCssStaticUrl({}),
 
        new CopyWebpackPlugin([
            {
                from: resolve(__dirname, '../static'),
                to: resolve(config.build.assetsRoot, 'static'),
                ignore: ['.*']
            }
        ])
    ]
})
 
export default devConf
