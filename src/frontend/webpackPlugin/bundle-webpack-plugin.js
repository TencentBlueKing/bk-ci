/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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
        const bundleName = props.bundleName || 'assets_bundle'
        this.SERVICE_ASSETS_JSON_PATH = path.join(
            __dirname,
            '..',
            dist,
            `${bundleName}.json`
        )
        this.SERVICE_ASSETS_DIR = path.dirname(this.SERVICE_ASSETS_JSON_PATH)
    }

    apply (compiler) {
        compiler.hooks.done.tapAsync(
            'BundleWebpackPlugin',
            (compilation, callback) => {
                console.log('This is an example plugin!')
                const { SERVICE_ASSETS_JSON_PATH, SERVICE_ASSETS_DIR } = this
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
                        .map((entryPointPublicPath) => {
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
                }

                let json = {}
                if (fs.existsSync(SERVICE_ASSETS_JSON_PATH)) {
                    json = JSON.parse(
                        fs.readFileSync(SERVICE_ASSETS_JSON_PATH, 'utf-8').toString()
                    )
                }
                json = {
                    ...json,
                    ...assetsMap
                }
                if (!fs.existsSync(SERVICE_ASSETS_DIR)) {
                    fs.mkdirSync(SERVICE_ASSETS_DIR)
                }
                fs.writeFileSync(SERVICE_ASSETS_JSON_PATH, JSON.stringify(json))

                callback()
            }
        )
    }
}
