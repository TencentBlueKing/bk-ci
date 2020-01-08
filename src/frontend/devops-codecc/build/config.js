/**
 * @file config
 * @author blueking
 */

import path from 'path'
import prodEnv, { cfgInHtml as cfgInHtmlProd } from './prod.env'
import devEnv, { cfgInHtml as cfgInHtmlDev } from './dev.env'

export default {
    build: {
        env: prodEnv,
        cfgInHtml: cfgInHtmlProd,
        assetsRoot: path.resolve(__dirname, '../dist'),
        assetsSubDirectory: 'static',
        assetsPublicPath: '/',
        productionSourceMap: true,
        productionGzip: false,
        productionGzipExtensions: ['js', 'css'],
        bundleAnalyzerReport: process.env.npm_config_report
    },
    dev: {
        env: devEnv,
        cfgInHtml: cfgInHtmlDev,
        port: 80,
        assetsSubDirectory: 'static',
        assetsPublicPath: '/',
        proxyTable: {},
        cssSourceMap: false,
        autoOpenBrowser: false
    }
}
