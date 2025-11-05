/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
const fs = require('fs')
const path = require('path')

module.exports = class BundleWebpackPlugin {
    // Define `apply` as its prototype method which is supplied with compiler as its argument
    constructor (props) {
        const dist = props.dist || '.'
        const entryFolderName = props.entryFolderName
        this.isDev = props.isDev || false
        this.DEBUG_ASSETS_BUNDLE_JSON_FILE = path.join( __dirname, '..', dist, 'assets_bundle.json')
        this.SERVICE_ASSETS_DIR = path.join(
            __dirname,
            '..',
            dist,
            entryFolderName
        )
        if (!this.isDev && !fs.existsSync(this.SERVICE_ASSETS_DIR)) {
            fs.mkdirSync(this.SERVICE_ASSETS_DIR, { recursive: true })
        }
    }

    apply (compiler) {
        compiler.hooks.done.tapAsync(
            'BundleWebpackPlugin',
            (compilation, callback) => {
                const { SERVICE_ASSETS_DIR } = this
                const entryNames = Array.from(
                    compilation.compilation.entrypoints.keys()
                )
                const extensionRegexp = /\.(css|js|mjs)(\?|$)/
                const entryPointPublicPathMap = {}
                const assetsMap = {}

                for (let i = 0; i < entryNames.length; i++) {
                    const entryName = entryNames[i]
                    const entryPointFiles = compilation.compilation.entrypoints
                        .get(entryName)
                        .getFiles()
                    const assets = {
                        js: '',
                        css: ''
                    }
                    entryPointFiles
                        .map((chunkFile) =>
                            chunkFile
                                .split('/')
                                .map(encodeURIComponent)
                                .join('/')
                        )
                        .forEach((entryPointPublicPath) => {
                            const extMatch = extensionRegexp.exec(
                                entryPointPublicPath
                            )
                            // Skip if the public path is not a .css, .mjs or .js file
                            if (!extMatch || /\.hot-update\.js$/.test(entryPointPublicPath)) {
                                return
                            }
                            // Skip if this file is already known
                            // (e.g. because of common chunk optimizations)
                            if (entryPointPublicPathMap[entryPointPublicPath]) {
                                return
                            }
                            entryPointPublicPathMap[entryPointPublicPath] = true
                            // ext will contain .js or .css, because .mjs recognizes as .js
                            const ext = extMatch[1] === 'mjs' ? 'js' : extMatch[1]
                            assets[ext] = entryPointPublicPath
                        })

                    assetsMap[entryName] = assets
                    if (!this.isDev) {
                        fs.writeFileSync(`${SERVICE_ASSETS_DIR}/${entryName}.json`, JSON.stringify(assetsMap))
                        console.log(`get assets entry about ${entryName}, ${JSON.stringify(assetsMap)}`)
                    }
                }
                if (this.isDev) {
                    fs.writeFileSync(this.DEBUG_ASSETS_BUNDLE_JSON_FILE, JSON.stringify(assetsMap))
                } 
                
                callback()
            }
        )
    }
}
