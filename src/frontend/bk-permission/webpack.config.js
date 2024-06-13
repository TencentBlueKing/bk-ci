const path = require('path')
// const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin
const { VueLoaderPlugin } = require('vue-loader')
const TerserPlugin = require('terser-webpack-plugin')
const MiniCssExtractPlugin = require('mini-css-extract-plugin')

module.exports = (env = {}, argv) => {
    const isDev = argv.mode === 'development'
    return {
        cache: {
            type: 'filesystem',
            buildDependencies: {
                config: [__filename]
            }
        },
        entry: './src/main.js',
        output: {
            library: {
                type: 'umd',
                name: 'bkPermission'
            },
            filename: 'main.js',
            path: path.resolve(__dirname, 'dist'),
            clean: true
        },
        externals: {
            vue: {
                commonjs: 'vue',
                commonjs2: 'vue',
                amd: 'vue',
                root: 'Vue'
            },
        },
        module: {
            rules: [
                {
                    test: /\.vue$/,
                    include: path.resolve('src'),
                    loader: 'vue-loader'
                },
                {
                    test: /\.js$/,
                    include: path.resolve('src'),
                    loader: 'babel-loader'
                },
                
                {
                    test: /\.s?css$/,
                    use: [MiniCssExtractPlugin.loader,, 'css-loader', 'sass-loader']
                },
                {
                    test: /\.(svg)(\?inline)?$/,
                    loader: 'svg-inline-loader',
                    type: 'asset/inline'
                }
                
            ]
        },
        plugins: [
            // new BundleAnalyzerPlugin(),
            new VueLoaderPlugin(),
            new MiniCssExtractPlugin({
                filename: 'main.css',
                chunkFilename: '[id].css',
                ignoreOrder: true
            })
        ],
        optimization: {
            chunkIds: isDev ? 'named' : 'deterministic',
            moduleIds: 'deterministic',
            minimize: !isDev,
            removeEmptyChunks: true,
            minimizer: [new TerserPlugin({
                terserOptions: {
                    format: {
                        comments: false
                    }
                },
                extractComments: false
            })]
        },
        resolve: {
            extensions: ['.js', '.vue', '.json', '.ts', '.scss', '.css'],
            fallback: {
                path: false
            },
            alias: {
                '@': path.resolve('src')
            }
        }
    }
}
