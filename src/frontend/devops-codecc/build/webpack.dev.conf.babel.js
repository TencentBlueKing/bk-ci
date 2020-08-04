/**
 * @file webpack dev conf
 * @author blueking
 */

import path from 'path'

import webpack from 'webpack'
import merge from 'webpack-merge'
import HtmlWebpackPlugin from 'html-webpack-plugin'
import FriendlyErrorsPlugin from 'friendly-errors-webpack-plugin'

import config from './config'
import baseConf from './webpack.base.conf.babel'
import manifest from '../static/lib-manifest.json'

// const HOST = 'localhost'
// const PORT = 8080

const webpackConfig = merge(baseConf, {
    mode: 'development',
    entry: {
        main: './src/main.js'
    },

    module: {
        rules: [
            {
                test: /\.(css|postcss)$/,
                use: [
                    'vue-style-loader',
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
                                path: path.resolve(__dirname, '..', 'postcss.config.js')
                            }
                        }
                    }
                ]
            }
        ]
    },

    plugins: [
        new webpack.DefinePlugin(config.dev.env),

        new webpack.DllReferencePlugin({
            context: __dirname,
            manifest: manifest
        }),

        new webpack.HotModuleReplacementPlugin(),

        new webpack.NoEmitOnErrorsPlugin(),

        new HtmlWebpackPlugin({
            filename: 'index.html',
            template: 'index-dev.html',
            inject: true,
            staticUrl: config.dev.env.staticUrl
        }),

        new FriendlyErrorsPlugin()
    ]
})

Object.keys(webpackConfig.entry).forEach(name => {
    webpackConfig.entry[name] = ['./build/dev-client'].concat(webpackConfig.entry[name])
})

export default webpackConfig
