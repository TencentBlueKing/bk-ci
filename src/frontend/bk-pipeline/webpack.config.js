const path = require('path')
// const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin
const { VueLoaderPlugin } = require('vue-loader')

module.exports = (env = {}, argv) => {
    const isDev = argv.mode === 'development'
    return {
        cache: {
            type: 'filesystem',
            buildDependencies: {
                config: [__filename]
            }
        },
        entry: './index.js',
        output: {
            library: {
                type: 'umd',
                name: 'bkPipeline'
            },
            filename: 'bk-pipeline.min.js',
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
            'bk-magic-vue': {
                commonjs: 'bk-magic-vue',
                commonjs2: 'bk-magic-vue',
                amd: 'bk-magic-vue',
                root: 'bkMagic'
            }
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
                    use: ['style-loader', 'css-loader', 'sass-loader']
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
                }
            ]
        },
        plugins: [
            // new BundleAnalyzerPlugin(),
            new VueLoaderPlugin()
            // new MiniCssExtractPlugin({
            //     filename: 'bk-pipeline.css',
            //     chunkFilename: '[id].css',
            //     ignoreOrder: true
            // })
        ],
        optimization: {
            chunkIds: isDev ? 'named' : 'deterministic',
            moduleIds: 'deterministic',
            minimize: !isDev
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
