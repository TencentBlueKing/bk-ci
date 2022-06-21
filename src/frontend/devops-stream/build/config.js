/**
 * @file config
 * @author Blueking
 */

import path from 'path'
import prodEnv from './prod.env'
import devEnv from './dev.env'
import yargs from 'yargs'

const argv = yargs.default({ 'dist': 'frontend' }).argv
const envDist = argv.env && argv.env.dist ? argv.env.dist : 'frontend'
const dist = path.resolve(__dirname, `../../${envDist}/stream`)

export default {
    build: {
        env: prodEnv,
        assetsRoot: dist,
        assetsSubDirectory: 'static',
        assetsPublicPath: '/',
        productionSourceMap: true,
        productionGzip: false,
        productionGzipExtensions: ['js', 'css'],
        bundleAnalyzerReport: process.env.npm_config_report
    },
    dev: {
        env: devEnv,
        port: '8080',
        assetsSubDirectory: 'static',
        assetsPublicPath: '/',
        proxyTable: {},
        cssSourceMap: false,
        autoOpenBrowser: false
    }
}
