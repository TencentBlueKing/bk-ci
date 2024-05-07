/**
 * @file dev server
 * @author Blueking
 */

import bodyParser from 'body-parser'
import history from 'connect-history-api-fallback'
import express from 'express'
import fs from 'fs'
import proxyMiddleware from 'http-proxy-middleware'
import https from 'https'
import open from 'open'
import path from 'path'
import webpack from 'webpack'
import webpackDevMiddleware from 'webpack-dev-middleware'
import webpackHotMiddleware from 'webpack-hot-middleware'

import config from './config'
import devConf from './webpack.dev.conf'

const port = process.env.PORT || config.dev.port

const autoOpenBrowser = !!config.dev.autoOpenBrowser

const proxyTable = config.dev.proxyTable

const app = express()
const compiler = webpack(devConf)

const devMiddleware = webpackDevMiddleware(compiler, {
    publicPath: devConf.output.publicPath
})

const hotMiddleware = webpackHotMiddleware(compiler, {
    log: false,
    heartbeat: 2000
})

Object.keys(proxyTable).forEach(context => {
    let options = proxyTable[context]
    if (typeof options === 'string') {
        options = {
            target: options
        }
    }
    app.use(proxyMiddleware(context, options))
})

app.use(history({
    verbose: false,
    rewrites: [
        {
            // connect-history-api-fallback 默认会对 url 中有 . 的 url 当成静态资源处理而不是当成页面地址来处理
            // 兼容 /router/10.121.23.12 这样以 IP 结尾的 url
            from: /(\d+\.)*\d+$/,
            to: '/'
        },
        {
            // connect-history-api-fallback 默认会对 url 中有 . 的 url 当成静态资源处理而不是当成页面地址来处理
            // 兼容 /router/0.aaa.bbb.ccc.1234567890/ddd/eee
            from: /\/+.*\..*\//,
            to: '/'
        }
    ]
}))

app.use(devMiddleware)

app.use(hotMiddleware)

app.use(bodyParser.json())

app.use(bodyParser.urlencoded({
    extended: true
}))

const staticPath = path.posix.join(config.dev.assetsPublicPath, config.dev.assetsSubDirectory)
app.use(staticPath, express.static('./static'))

const localHostName = 'local-stream.com'

const url = `https://${localHostName}:${port}`

let _resolve
const readyPromise = new Promise(resolve => {
    _resolve = resolve
})

console.log('> Starting dev server...')
devMiddleware.waitUntilValid(() => {
    console.log('> Listening at ' + url + '\n')
    if (autoOpenBrowser) {
        open(url)
    }
    _resolve()
})

// https
const privateKey = fs.readFileSync(path.resolve(__dirname, `../src/conf/${localHostName}.key`), 'utf8')
const certificate = fs.readFileSync(path.resolve(__dirname, `../src/conf/${localHostName}.crt`), 'utf8')
const credentials = { key: privateKey, cert: certificate }

const server = https.createServer(credentials, app).listen(port)

export default {
    ready: readyPromise,
    close: () => {
        server.close()
    }
}
