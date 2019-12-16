const path = require('path')
const webpack = require('webpack')
const MiniCssExtractPlugin = require('mini-css-extract-plugin')
const VueLoaderPlugin = require('vue-loader/lib/plugin')
const CopyWebpackPlugin = require('copy-webpack-plugin')
// const TerserPlugin = require('terser-webpack-plugin')
// const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin

module.exports = ({ entry, output = {}, publicPath, dist, port = 8080, argv, env }) => {
    const isDev = argv.mode === 'development'
    const isMaster = process.env.NODE_ENV === 'master'
    const envDist = env && env.dist ? env.dist : 'frontend'
    const version = env && env.version ? env.version : 'tencent'
    const buildDist = path.join(__dirname, envDist, dist)
    console.log(path.join(__dirname, 'locale', dist), version)
    return {
        entry,
        output: {
            publicPath,
            chunkFilename: isMaster ? '[name].[chunkhash].js' : '[name].js',
            filename: isMaster ? '[name].[contentHash].min.js' : '[name].js',
            path: buildDist,
            ...output
        },
        module: {
            rules: [
                {
                    test: /\.vue$/,
                    include: [path.resolve('src'), path.resolve('../node_modules/vue-echarts'), path.resolve('../devops-log')],
                    loader: 'vue-loader'
                },
                {
                    test: /\.js$/,
                    include: [path.resolve('src'), path.resolve('../node_modules/vue-echarts'), path.resolve('../devops-log')],
                    use: [
                        {
                            loader: 'babel-loader'
                        }
                    ]
                },
                {
                    test: /\.css$/,
                    use: [MiniCssExtractPlugin.loader, 'css-loader']
                },
                {
                    test: /\.scss$/,
                    use: [isDev ? MiniCssExtractPlugin.loader : 'style-loader', 'css-loader', 'sass-loader']
                },
                {
                    test: /\.(png|jpe?g|gif|svg|webp|cur)(\?.*)?$/,
                    loader: 'url-loader',
                    options: {
                        limit: 10000,
                        name: '[name].[ext]?[hash]'
                    }
                },
                {
                    test: /\.(js|vue)$/,
                    loader: 'eslint-loader',
                    enforce: 'pre',
                    include: [path.resolve('src')],
                    exclude: /node_modules/,
                    options: {
                        fix: true,
                        formatter: require('eslint-friendly-formatter')
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
            // new BundleAnalyzerPlugin(),
            new VueLoaderPlugin(),
            new webpack.optimize.LimitChunkCountPlugin({
                minChunkSize: 1000
            }),
            new webpack.HashedModuleIdsPlugin(),
            new MiniCssExtractPlugin({
                filename: isMaster ? '[name].[chunkHash].css' : '[name].css',
                chunkName: '[id].css'
            }),
            new webpack.DefinePlugin({
                VERSION_TYPE: JSON.stringify(version)
            }),
            new CopyWebpackPlugin([{ from: path.join(__dirname, 'locale', dist), to: buildDist }])
        ],
        optimization: {
            namedChunks: true,
            minimize: true
        },
        resolve: {
            extensions: ['.js', '.vue', '.json', '.ts', '.scss', '.css'],
            alias: {
                '@': path.resolve('src'),
                'vue$': 'vue/dist/vue.esm.js',
                '@locale': path.resolve(__dirname, 'locale')
            }
        },
        externals: {
            'vue': 'Vue',
            'vue-router': 'VueRouter',
            'vuex': 'Vuex'
        },
        devServer: {
            contentBase: path.join(__dirname, envDist),
            historyApiFallback: true,
            noInfo: false,
            disableHostCheck: true,
            port
        }
    }
}
